package com.monoapps.monoclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    Spacer(Modifier.height(32.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatButton("−") { viewModel.updateTimerDuration(duration - 60_000L) }
        TextMMD(
            formatDuration(duration),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        RepeatButton("+") { viewModel.updateTimerDuration(duration + 60_000L) }
    }
    Spacer(Modifier.height(40.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val presets = listOf(
            listOf("+5 min" to 5 * 60_000L, "+10 min" to 10 * 60_000L),
            listOf("+30 min" to 30 * 60_000L, "+1 h" to 3_600_000L),
        )
        for (presetRow in presets) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for ((label, add) in presetRow) {
                    OutlinedButtonMMD(
                        onClick = { viewModel.updateTimerDuration(duration + add) },
                        modifier = Modifier.weight(1f),
                    ) { TextMMD(label, fontSize = 14.sp) }
                }
            }
        }
    }
    Spacer(Modifier.height(48.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButtonMMD(
            onClick = { viewModel.updateTimerDuration(0) },
            modifier = Modifier.weight(1f).height(56.dp),
        ) { TextMMD("Reset", fontSize = 18.sp) }
        OutlinedButtonMMD(
            onClick = { viewModel.startTimer(viewModel.timerDuration) },
            modifier = Modifier.weight(1f).height(56.dp),
        ) { TextMMD("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun BigDuration(millis: Long) {
    TextMMD(formatDuration(millis), fontSize = 80.sp, fontWeight = FontWeight.Black)
}
