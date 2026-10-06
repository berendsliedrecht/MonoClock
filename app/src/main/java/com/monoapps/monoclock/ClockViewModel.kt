package com.monoapps.monoclock

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.monoapps.monoclock.data.Alarm
import com.monoapps.monoclock.data.ClockStore

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ClockStore(application)
    private val context get() = getApplication<Application>()

    var alarms by mutableStateOf(store.alarms())
        private set

    // Timer
    var timerEnd by mutableStateOf(store.timerEnd)
        private set
    var timerPausedRemaining by mutableStateOf(store.timerPausedRemaining)
        private set
    var timerDuration by mutableStateOf(store.timerDuration)
        private set

    // Stopwatch
    var stopwatchBase by mutableStateOf(store.stopwatchBase)
        private set
    var stopwatchAccumulated by mutableStateOf(store.stopwatchAccumulated)
        private set
    val laps = mutableStateListOf<Long>()

    /** Reload persisted state; called on resume and after an alarm rings. */
    fun refresh() {
        alarms = store.alarms()
        timerEnd = store.timerEnd
        timerPausedRemaining = store.timerPausedRemaining
        // A reboot invalidates the elapsedRealtime base; reset the stopwatch.
        store.stopwatchBase?.let { if (it > SystemClock.elapsedRealtime()) resetStopwatch() }
        stopwatchBase = store.stopwatchBase
        stopwatchAccumulated = store.stopwatchAccumulated
    }

    // Alarms

    fun addAlarm(hour: Int, minute: Int, days: Set<Int>, sound: String? = null): Alarm {
        val alarm = Alarm(store.nextAlarmId(), hour, minute, days, enabled = true, sound = sound)
        saveAlarms(alarms + alarm)
        AlarmScheduler.scheduleAlarm(context, alarm)
        return alarm
    }

    fun updateAlarm(alarm: Alarm) {
        saveAlarms(alarms.map { if (it.id == alarm.id) alarm else it })
        AlarmScheduler.cancelAlarm(context, alarm.id)
        if (alarm.enabled) AlarmScheduler.scheduleAlarm(context, alarm)
    }

    fun deleteAlarm(alarm: Alarm) {
        saveAlarms(alarms.filter { it.id != alarm.id })
        AlarmScheduler.cancelAlarm(context, alarm.id)
    }

    fun setAlarmEnabled(alarm: Alarm, enabled: Boolean) = updateAlarm(alarm.copy(enabled = enabled))

    private fun saveAlarms(list: List<Alarm>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        store.saveAlarms(sorted)
        alarms = sorted
    }

    // Timer

    fun updateTimerDuration(millis: Long) {
        timerDuration = millis.coerceIn(0L, 99 * 3_600_000L)
        store.timerDuration = timerDuration
    }

    fun startTimer(durationMillis: Long = timerPausedRemaining ?: timerDuration) {
        if (durationMillis <= 0) return
        val end = System.currentTimeMillis() + durationMillis
        store.timerEnd = end
        store.timerPausedRemaining = null
        timerEnd = end
        timerPausedRemaining = null
        AlarmScheduler.scheduleTimer(context, end)
    }

    fun pauseTimer() {
        val end = timerEnd ?: return
        val remaining = (end - System.currentTimeMillis()).coerceAtLeast(1)
        AlarmScheduler.cancelTimer(context)
        store.timerEnd = null
        store.timerPausedRemaining = remaining
        timerEnd = null
        timerPausedRemaining = remaining
    }

    fun cancelTimer() {
        AlarmScheduler.cancelTimer(context)
        store.clearTimer()
        timerEnd = null
        timerPausedRemaining = null
    }

    // Stopwatch

    val stopwatchRunning get() = stopwatchBase != null

    fun stopwatchElapsed(): Long =
        stopwatchAccumulated + (stopwatchBase?.let { SystemClock.elapsedRealtime() - it } ?: 0L)

    fun startStopwatch() {
        if (stopwatchRunning) return
        stopwatchBase = SystemClock.elapsedRealtime()
        store.stopwatchBase = stopwatchBase
    }

    fun pauseStopwatch() {
        stopwatchAccumulated = stopwatchElapsed()
        stopwatchBase = null
        store.stopwatchAccumulated = stopwatchAccumulated
        store.stopwatchBase = null
    }

    fun lapStopwatch() {
        if (stopwatchRunning) laps.add(0, stopwatchElapsed())
    }

    fun resetStopwatch() {
        stopwatchBase = null
        stopwatchAccumulated = 0
        laps.clear()
        store.stopwatchBase = null
        store.stopwatchAccumulated = 0
    }
}
