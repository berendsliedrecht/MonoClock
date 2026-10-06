package com.monoapps.monoclock.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatWallClock(context: Context, millis: Long = System.currentTimeMillis()): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return DateFormat.format(pattern, Date(millis)).toString()
}

fun formatAlarmTime(context: Context, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return formatWallClock(context, cal.timeInMillis)
}

/** H:MM:SS, or MM:SS under an hour. */
fun formatDuration(millis: Long): String {
    val total = millis / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

/** Monday-first day order for the repeat picker. */
val WEEK_DAYS = listOf(
    Calendar.MONDAY to "M",
    Calendar.TUESDAY to "T",
    Calendar.WEDNESDAY to "W",
    Calendar.THURSDAY to "T",
    Calendar.FRIDAY to "F",
    Calendar.SATURDAY to "S",
    Calendar.SUNDAY to "S",
)

fun describeDays(days: Set<Int>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == setOf(Calendar.SATURDAY, Calendar.SUNDAY) -> "Weekends"
    days == (Calendar.MONDAY..Calendar.FRIDAY).toSet() -> "Weekdays"
    else -> WEEK_DAYS.filter { it.first in days }.joinToString(" ") {
        when (it.first) {
            Calendar.MONDAY -> "Mon"; Calendar.TUESDAY -> "Tue"; Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"; Calendar.FRIDAY -> "Fri"; Calendar.SATURDAY -> "Sat"
            else -> "Sun"
        }
    }
}

/**
 * One column of a time picker: + above, the two-digit value, − below.
 * Holding a button repeats the step; no animation, just state ticks.
 */
@Composable
fun StepperColumn(value: Int, max: Int, onChange: (Int) -> Unit) {
    val current by rememberUpdatedState(value)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepeatButton("+") { onChange((current + 1).mod(max + 1)) }
        TextMMD(
            String.format(Locale.US, "%02d", value),
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
        )
        RepeatButton("−") { onChange((current - 1).mod(max + 1)) }
    }
}

@Composable
fun RepeatButton(label: String, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    step()
                    var repeater: Job? = scope.launch {
                        delay(400)
                        while (isActive) {
                            step()
                            delay(120)
                        }
                    }
                    tryAwaitRelease()
                    repeater?.cancel()
                    repeater = null
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(label, fontSize = 24.sp)
    }
}

/** Outlined round add button (MMD's filled FAB violates the no-infill rule). */
@Composable
fun OutlinedFab(label: String, onClick: () -> Unit) {
    val click by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .size(64.dp)
            .border(2.dp, Color.Black, CircleShape)
            .pointerInput(Unit) { detectTapGestures { click() } },
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(label, fontSize = 32.sp)
    }
}
