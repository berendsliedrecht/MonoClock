package com.monoapps.monoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms alarms and the timer after reboot or a clock/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(context)
    }
}
