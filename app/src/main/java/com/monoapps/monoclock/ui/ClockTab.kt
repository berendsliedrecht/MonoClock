package com.monoapps.monoclock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monoclock.ClockViewModel
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun ClockTab(viewModel: ClockViewModel, modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            // Wake on the minute boundary; the display has no seconds.
            delay(60_000 - now % 60_000)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        TextMMD(formatWallClock(context, now), fontSize = 88.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        TextMMD(
            DateFormat.format("EEEE, MMMM d", Date(now)).toString(),
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(48.dp))
        val next = viewModel.nextAlarm()
        TextMMD(
            if (next == null) "No alarm set"
            else "Next alarm " + DateFormat.format("EEE", Date(next.nextTrigger())) +
                " " + formatAlarmTime(context, next.hour, next.minute),
            fontSize = 16.sp,
        )
    }
}
