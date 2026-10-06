package com.monoapps.monoclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monoclock.ClockViewModel
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay

@Composable
fun TimerTab(viewModel: ClockViewModel, modifier: Modifier) {
    val end = viewModel.timerEnd
    val paused = viewModel.timerPausedRemaining

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        when {
            end != null -> RunningTimer(viewModel, end)
            paused != null -> PausedTimer(viewModel, paused)
            else -> TimerPicker(viewModel)
        }
    }
}

@Composable
private fun RunningTimer(viewModel: ClockViewModel, end: Long) {
    var remaining by remember(end) { mutableLongStateOf(end - System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            val r = end - System.currentTimeMillis()
            remaining = r
            if (r <= 0) {
                // Rang in the background; pick up the cleared state.
                viewModel.refresh()
                break
            }
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }
    BigDuration(remaining.coerceAtLeast(0))
    Spacer(Modifier.height(64.dp))
    Row {
        OutlinedButtonMMD(onClick = { viewModel.pauseTimer() }) {
            TextMMD("Pause", fontSize = 18.sp)
        }
        Spacer(Modifier.width(16.dp))
        OutlinedButtonMMD(onClick = { viewModel.cancelTimer() }) {
            TextMMD("Cancel", fontSize = 18.sp)
        }
    }
}

@Composable
private fun PausedTimer(viewModel: ClockViewModel, remaining: Long) {
    BigDuration(remaining)
    Spacer(Modifier.height(64.dp))
    Row {
        OutlinedButtonMMD(onClick = { viewModel.startTimer(remaining) }) {
            TextMMD("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        OutlinedButtonMMD(onClick = { viewModel.cancelTimer() }) {
            TextMMD("Cancel", fontSize = 18.sp)
        }
    }
}

@Composable
private fun TimerPicker(viewModel: ClockViewModel) {
    val duration = viewModel.timerDuration
    val h = (duration / 3_600_000L).toInt()
    val m = (duration % 3_600_000L / 60_000L).toInt()
    val s = (duration % 60_000L / 1000L).toInt()
    fun set(h2: Int = h, m2: Int = m, s2: Int = s) =
        viewModel.updateTimerDuration((h2 * 3600L + m2 * 60L + s2) * 1000L)

    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperColumn(h, max = 23) { set(h2 = it) }
        Colon()
        StepperColumn(m, max = 59) { set(m2 = it) }
        Colon()
        StepperColumn(s, max = 59) { set(s2 = it) }
    }
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for ((label, minutes) in listOf("1 min" to 1, "5 min" to 5, "10 min" to 10, "30 min" to 30)) {
            OutlinedButtonMMD(onClick = { viewModel.updateTimerDuration(minutes * 60_000L) }) {
                TextMMD(label, fontSize = 14.sp)
            }
        }
    }
    Spacer(Modifier.height(48.dp))
    OutlinedButtonMMD(
        onClick = { viewModel.startTimer(viewModel.timerDuration) },
        modifier = Modifier.height(56.dp),
    ) { TextMMD("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Colon() {
    TextMMD(
        ":", fontSize = 56.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun BigDuration(millis: Long) {
    TextMMD(formatDuration(millis), fontSize = 80.sp, fontWeight = FontWeight.Black)
}
