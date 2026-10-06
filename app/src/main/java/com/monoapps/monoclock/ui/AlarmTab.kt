@file:OptIn(ExperimentalMaterial3Api::class)

package com.monoapps.monoclock.ui

import android.app.AlarmManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun AlarmTab(viewModel: ClockViewModel, modifier: Modifier, onEdit: (Alarm?) -> Unit) {
    val context = LocalContext.current
    var sheetAlarm by remember { mutableStateOf<Alarm?>(null) }

    sheetAlarm?.let { selected ->
        ModalBottomSheetMMD(onDismissRequest = { sheetAlarm = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                TextMMD(
                    formatAlarmTime(context, selected.hour, selected.minute),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedButtonMMD(
                    onClick = {
                        viewModel.deleteAlarm(selected)
                        sheetAlarm = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { TextMMD("Delete", fontSize = 18.sp) }
                Spacer(Modifier.height(12.dp))
                OutlinedButtonMMD(
                    onClick = { sheetAlarm = null },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { TextMMD("Cancel", fontSize = 18.sp) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

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
                            onLongPress = { sheetAlarm = alarm },
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
private fun AlarmRow(
    alarm: Alarm,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(alarm) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
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
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: 0) }
    var days by remember { mutableStateOf(alarm?.days ?: emptySet<Int>()) }
    var sound by remember { mutableStateOf(alarm?.sound) }

    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    val pickSound = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked = result.data
                ?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            // Picking "Default" stores null so the alarm follows the system setting.
            sound = picked?.takeIf { it != defaultUri }?.toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperColumn(hour, max = 23) { hour = it }
            TextMMD(":", fontSize = 44.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp))
            StepperColumn(minute, max = 59) { minute = it }
        }
        Spacer(Modifier.height(28.dp))
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
        Spacer(Modifier.height(28.dp))
        val soundName = sound
            ?.let { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) }
            ?: "Default"
        OutlinedButtonMMD(
            onClick = {
                pickSound.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
                        .putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            sound?.let(Uri::parse) ?: defaultUri,
                        )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { TextMMD("Sound: $soundName", fontSize = 16.sp) }
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (alarm != null) {
                OutlinedButtonMMD(
                    onClick = {
                        viewModel.deleteAlarm(alarm)
                        onDone()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { TextMMD("Delete", fontSize = 18.sp) }
            }
            OutlinedButtonMMD(
                onClick = {
                    if (alarm == null) viewModel.addAlarm(hour, minute, days, sound)
                    else viewModel.updateAlarm(
                        alarm.copy(hour = hour, minute = minute, days = days, enabled = true, sound = sound)
                    )
                    onDone()
                },
                modifier = Modifier.weight(1f).height(52.dp),
            ) { TextMMD("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
