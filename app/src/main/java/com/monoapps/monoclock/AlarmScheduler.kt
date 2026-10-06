package com.monoapps.monoclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.monoapps.monoclock.data.Alarm
import com.monoapps.monoclock.data.ClockStore

/**
 * Thin wrapper around [AlarmManager.setAlarmClock], the OS mechanism for
 * user-visible alarms: exact, Doze-exempt, and shown in the status bar.
 */
object AlarmScheduler {
    const val TIMER_REQUEST = 1_000_000

    fun canSchedule(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    fun scheduleAlarm(context: Context, alarm: Alarm, triggerAt: Long = alarm.nextTrigger()) {
        setAlarmClock(context, triggerAt, alarm.id, isTimer = false)
    }

    fun cancelAlarm(context: Context, id: Int) = cancel(context, id)

    fun scheduleTimer(context: Context, endMillis: Long) {
        setAlarmClock(context, endMillis, TIMER_REQUEST, isTimer = true)
    }

    fun cancelTimer(context: Context) = cancel(context, TIMER_REQUEST)

    /** Re-arm everything after boot or a clock/timezone change. */
    fun rescheduleAll(context: Context) {
        val store = ClockStore(context)
        for (alarm in store.alarms().filter { it.enabled }) scheduleAlarm(context, alarm)
        val end = store.timerEnd
        if (end != null) {
            if (end > System.currentTimeMillis()) scheduleTimer(context, end) else store.clearTimer()
        }
    }

    private fun setAlarmClock(context: Context, triggerAt: Long, requestCode: Int, isTimer: Boolean) {
        if (!canSchedule(context)) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmService.EXTRA_IS_TIMER, isTimer)
                .putExtra(AlarmService.EXTRA_ALARM_ID, requestCode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val show = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
    }

    private fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
    }
}
