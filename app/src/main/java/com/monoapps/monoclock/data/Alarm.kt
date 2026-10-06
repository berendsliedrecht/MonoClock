package com.monoapps.monoclock.data

import java.util.Calendar

/**
 * A single alarm. [days] holds [Calendar] day-of-week constants
 * (SUNDAY=1..SATURDAY=7); empty means a one-shot alarm that disables
 * itself after ringing.
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val enabled: Boolean,
) {
    val repeating: Boolean get() = days.isNotEmpty()

    /** Epoch millis of the next time this alarm should ring, strictly after [now]. */
    fun nextTrigger(now: Calendar = Calendar.getInstance()): Long {
        val t = now.clone() as Calendar
        t.set(Calendar.HOUR_OF_DAY, hour)
        t.set(Calendar.MINUTE, minute)
        t.set(Calendar.SECOND, 0)
        t.set(Calendar.MILLISECOND, 0)
        repeat(8) {
            val dayOk = days.isEmpty() || t.get(Calendar.DAY_OF_WEEK) in days
            if (t.timeInMillis > now.timeInMillis && dayOk) return t.timeInMillis
            t.add(Calendar.DAY_OF_YEAR, 1)
        }
        return t.timeInMillis
    }
}
