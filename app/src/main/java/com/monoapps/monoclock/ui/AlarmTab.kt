package com.monoapps.monoclock.ui

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monoclock.ClockViewModel
import com.monoapps.monoclock.data.Alarm
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun AlarmTab(viewModel: ClockViewModel, modifier: Modifier, onEdit: (Alarm?) -> Unit) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT in 31..32 &&
                !(context.getSystemService(AlarmManager::class.java)).canScheduleExactAlarms()
            ) {
                Column(Modifier.padding(16.dp)) {
                    TextMMD("Exact alarms are disabled for this app.", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButtonMMD(onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }) { TextMMD("Allow in settings") }
                }
                HorizontalDividerMMD()
            }
            if (viewModel.alarms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TextMMD("No alarms", fontSize = 18.sp)
                }
            } else {
                LazyColumnMMD(Modifier.fillMaxWidth().weight(1f)) {
                    items(viewModel.alarms.size) { i ->
                        val alarm = viewModel.alarms[i]
                        AlarmRow(
                            alarm = alarm,
                            onClick = { onEdit(alarm) },
                            onToggle = { viewModel.setAlarmEnabled(alarm, it) },
                        )
                        HorizontalDividerMMD()
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
            OutlinedFab("+") { onEdit(null) }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            TextMMD(
                formatAlarmTime(context, alarm.hour, alarm.minute),
                fontSize = 36.sp,
                fontWeight = if (alarm.enabled) FontWeight.Bold else FontWeight.Normal,
            )
            TextMMD(describeDays(alarm.days), fontSize = 14.sp)
        }
        SwitchMMD(checked = alarm.enabled, onCheckedChange = onToggle)
    }
}

/** Add/edit screen for one alarm; pass null [alarm] to create a new one. */
@Composable
fun AlarmEditScreen(
    viewModel: ClockViewModel,
    alarm: Alarm?,
    modifier: Modifier,
    onDone: () -> Unit,
) {
    var hour by remember { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: 0) }
    var days by remember { mutableStateOf(alarm?.days ?: emptySet<Int>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperColumn(hour, max = 23) { hour = it }
            TextMMD(":", fontSize = 56.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp))
            StepperColumn(minute, max = 59) { minute = it }
        }
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for ((day, letter) in WEEK_DAYS) {
                val selected = day in days
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .then(
                            if (selected) Modifier.border(2.dp, Color.Black, CircleShape)
                            else Modifier
                        )
                        .pointerInput(day) {
                            detectTapGestures {
                                days = if (day in days) days - day else days + day
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    TextMMD(
                        letter,
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextMMD(describeDays(days), fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))
        OutlinedButtonMMD(
            onClick = {
                if (alarm == null) viewModel.addAlarm(hour, minute, days)
                else viewModel.updateAlarm(alarm.copy(hour = hour, minute = minute, days = days, enabled = true))
                onDone()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { TextMMD("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        if (alarm != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButtonMMD(
                onClick = {
                    viewModel.deleteAlarm(alarm)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { TextMMD("Delete", fontSize = 18.sp) }
        }
    }
}
