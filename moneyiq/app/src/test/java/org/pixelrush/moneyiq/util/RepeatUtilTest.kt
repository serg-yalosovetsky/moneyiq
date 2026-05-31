package org.pixelrush.moneyiq.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RepeatUtilTest {

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0
    ): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, hour, minute, 0)
    }.timeInMillis

    private fun dayOfWeek(timestamp: Long): Int =
        Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK)

    @Test
    fun `calculateNextRepeatDate daily adds one day`() {
        val next = calculateNextRepeatDate(millis(2026, 6, 1), "DAILY")

        assertEquals(millis(2026, 6, 2), next)
    }

    @Test
    fun `calculateNextRepeatDate weekdays skips weekend`() {
        val friday = millis(2026, 6, 5)
        val next = calculateNextRepeatDate(friday, "WEEKDAYS")

        assertEquals(Calendar.MONDAY, dayOfWeek(next))
        assertEquals(millis(2026, 6, 8), next)
    }

    @Test
    fun `calculateNextRepeatDate weekends jumps to Saturday`() {
        val monday = millis(2026, 6, 1)
        val next = calculateNextRepeatDate(monday, "WEEKENDS")

        assertEquals(Calendar.SATURDAY, dayOfWeek(next))
        assertEquals(millis(2026, 6, 6), next)
    }

    @Test
    fun `calculateNextRepeatDate unknown mode returns Long MAX_VALUE`() {
        assertEquals(Long.MAX_VALUE, calculateNextRepeatDate(millis(2026, 6, 1), "NEVER"))
    }

    @Test
    fun `startOfDay clears time fields`() {
        assertEquals(millis(2026, 6, 1, 0, 0), startOfDay(millis(2026, 6, 1, 23, 59)))
    }

    @Test
    fun `reminderOffsetDays maps reminder modes`() {
        assertEquals(0, reminderOffsetDays("SAME_DAY"))
        assertEquals(7, reminderOffsetDays("7_DAYS"))
        assertEquals(-1, reminderOffsetDays("NEVER"))
    }
}
