package com.monoapps.monoclock.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** SharedPreferences persistence for alarms, the timer, and the stopwatch. */
class ClockStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("monoclock", Context.MODE_PRIVATE)

    // Alarms

    fun alarms(): List<Alarm> {
        val json = prefs.getString("alarms", null) ?: return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val days = o.getJSONArray("days")
            Alarm(
                id = o.getInt("id"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                days = (0 until days.length()).map { days.getInt(it) }.toSet(),
                enabled = o.getBoolean("enabled"),
            )
        }
    }

    fun saveAlarms(alarms: List<Alarm>) {
        val arr = JSONArray()
        for (a in alarms) {
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("hour", a.hour)
                    .put("minute", a.minute)
                    .put("days", JSONArray(a.days.sorted()))
                    .put("enabled", a.enabled)
            )
        }
        prefs.edit().putString("alarms", arr.toString()).commit()
    }

    fun nextAlarmId(): Int {
        val id = prefs.getInt("next_alarm_id", 1)
        prefs.edit().putInt("next_alarm_id", id + 1).commit()
        return id
    }

    fun updateAlarm(alarm: Alarm) = saveAlarms(alarms().map { if (it.id == alarm.id) alarm else it })

    // Timer: either running (end wall-clock time set) or paused (remaining set)

    var timerEnd: Long?
        get() = prefs.getLong("timer_end", 0L).takeIf { it > 0 }
        set(value) { prefs.edit().putLong("timer_end", value ?: 0L).commit() }

    var timerPausedRemaining: Long?
        get() = prefs.getLong("timer_paused", 0L).takeIf { it > 0 }
        set(value) { prefs.edit().putLong("timer_paused", value ?: 0L).commit() }

    /** Last duration picked, so the picker reopens where the user left it. */
    var timerDuration: Long
        get() = prefs.getLong("timer_duration", 5 * 60_000L)
        set(value) { prefs.edit().putLong("timer_duration", value).commit() }

    fun clearTimer() {
        prefs.edit().putLong("timer_end", 0L).putLong("timer_paused", 0L).commit()
    }

    // Stopwatch: accumulated millis plus, when running, the elapsedRealtime base

    var stopwatchBase: Long?
        get() = prefs.getLong("sw_base", 0L).takeIf { it > 0 }
        set(value) { prefs.edit().putLong("sw_base", value ?: 0L).commit() }

    var stopwatchAccumulated: Long
        get() = prefs.getLong("sw_accum", 0L)
        set(value) { prefs.edit().putLong("sw_accum", value).commit() }
}
