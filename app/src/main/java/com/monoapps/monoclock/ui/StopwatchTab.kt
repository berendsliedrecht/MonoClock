package com.monoapps.monoclock.ui

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
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun StopwatchTab(viewModel: ClockViewModel, modifier: Modifier) {
    val running = viewModel.stopwatchRunning
    var elapsed by remember { mutableLongStateOf(viewModel.stopwatchElapsed()) }
    LaunchedEffect(running) {
        elapsed = viewModel.stopwatchElapsed()
        // One tick per second: e-ink refreshes are too slow for centiseconds.
        while (running) {
            delay(1000 - viewModel.stopwatchElapsed() % 1000)
            elapsed = viewModel.stopwatchElapsed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        TextMMD(formatDuration(elapsed), fontSize = 80.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(48.dp))
        Row {
            when {
                running -> {
                    OutlinedButtonMMD(onClick = { viewModel.lapStopwatch() }) {
                        TextMMD("Lap", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    OutlinedButtonMMD(onClick = {
                        viewModel.pauseStopwatch()
                    }) { TextMMD("Pause", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                else -> {
                    OutlinedButtonMMD(onClick = { viewModel.startStopwatch() }) {
                        TextMMD("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    if (elapsed > 0) {
                        Spacer(Modifier.width(16.dp))
                        OutlinedButtonMMD(onClick = { viewModel.resetStopwatch() }) {
                            TextMMD("Reset", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        if (viewModel.laps.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            LazyColumnMMD(Modifier.fillMaxWidth().weight(1f)) {
                items(viewModel.laps.size) { i ->
                    val lapNumber = viewModel.laps.size - i
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    ) {
                        TextMMD(
                            String.format(Locale.US, "Lap %d", lapNumber),
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextMMD(formatDuration(viewModel.laps[i]), fontSize = 16.sp)
                    }
                    HorizontalDividerMMD()
                }
            }
        }
    }
}
