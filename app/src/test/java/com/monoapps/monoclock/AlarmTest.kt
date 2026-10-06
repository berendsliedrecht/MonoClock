package com.monoapps.monoclock

import com.monoapps.monoclock.data.Alarm
import com.monoapps.monoclock.ui.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AlarmTest {

    // Monday 2026-10-05 10:00
    private fun monday10(): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Long.cal(): Calendar = Calendar.getInstance().apply { timeInMillis = this@cal }

    @Test
    fun oneShotLaterToday() {
        val t = Alarm(1, 15, 30, emptySet(), true).nextTrigger(monday10()).cal()
        assertEquals(Calendar.MONDAY, t.get(Calendar.DAY_OF_WEEK))
        assertEquals(15, t.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, t.get(Calendar.MINUTE))
    }

    @Test
    fun oneShotTimePassedGoesToTomorrow() {
        val t = Alarm(1, 9, 0, emptySet(), true).nextTrigger(monday10()).cal()
        assertEquals(Calendar.TUESDAY, t.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, t.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun exactCurrentMinuteCountsAsPassed() {
        val t = Alarm(1, 10, 0, emptySet(), true).nextTrigger(monday10()).cal()
        assertEquals(Calendar.TUESDAY, t.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun repeatingSkipsToSelectedDay() {
        val t = Alarm(1, 9, 0, setOf(Calendar.FRIDAY), true).nextTrigger(monday10()).cal()
        assertEquals(Calendar.FRIDAY, t.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, t.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun repeatingSameDayNextWeekWhenTimePassed() {
        val t = Alarm(1, 9, 0, setOf(Calendar.MONDAY), true).nextTrigger(monday10()).cal()
        assertEquals(Calendar.MONDAY, t.get(Calendar.DAY_OF_WEEK))
        val days = (t.timeInMillis - monday10().timeInMillis) / 86_400_000
        assertEquals(6, days) // 9:00 is 23h earlier in the day, so 6 full days
    }

    @Test
    fun repeatingTodayWhenTimeAhead() {
        val t = Alarm(1, 22, 15, setOf(Calendar.MONDAY, Calendar.WEDNESDAY), true)
            .nextTrigger(monday10()).cal()
        assertEquals(Calendar.MONDAY, t.get(Calendar.DAY_OF_WEEK))
        assertEquals(22, t.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun durationFormatting() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:59", formatDuration(59_999))
        assertEquals("05:00", formatDuration(5 * 60_000L))
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("2:03:04", formatDuration((2 * 3600 + 3 * 60 + 4) * 1000L))
    }
}
