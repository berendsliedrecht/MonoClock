package com.monoapps.monoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.monoapps.monoclock.data.ClockStore

/** Fired by AlarmManager when an alarm or the timer goes off. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isTimer = intent.getBooleanExtra(AlarmService.EXTRA_IS_TIMER, false)
        val id = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
        val store = ClockStore(context)

        if (isTimer) {
            store.clearTimer()
        } else {
            val alarm = store.alarms().find { it.id == id } ?: return
            if (!alarm.enabled) return
            if (alarm.repeating) {
                AlarmScheduler.scheduleAlarm(context, alarm)
            } else {
                store.updateAlarm(alarm.copy(enabled = false))
            }
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java)
                .setAction(AlarmService.ACTION_RING)
                .putExtra(AlarmService.EXTRA_IS_TIMER, isTimer)
                .putExtra(AlarmService.EXTRA_ALARM_ID, id),
        )
    }
}
