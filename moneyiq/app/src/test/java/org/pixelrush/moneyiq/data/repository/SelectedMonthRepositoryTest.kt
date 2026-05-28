package org.pixelrush.moneyiq.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class SelectedMonthRepositoryTest {

    private lateinit var repo: SelectedMonthRepository

    @Before
    fun setup() {
        repo = SelectedMonthRepository()
    }

    // ── prevMonth ────────────────────────────────────────────────────────────

    @Test
    fun `prevMonth decrements month within year`() {
        repo.setPeriod(AppMonth(2025, 4)) // May
        repo.prevMonth()
        val result = repo.month.value
        assertEquals(2025, result.year)
        assertEquals(3, result.month) // April
    }

    @Test
    fun `prevMonth rolls year back from January to December`() {
        repo.setPeriod(AppMonth(2025, 0)) // January
        repo.prevMonth()
        val result = repo.month.value
        assertEquals(2024, result.year)
        assertEquals(11, result.month) // December
    }

    @Test
    fun `prevMonth in YEAR mode decrements year`() {
        repo.setPeriod(AppMonth(2025, 6, PeriodMode.YEAR))
        repo.prevMonth()
        val result = repo.month.value
        assertEquals(2024, result.year)
        assertEquals(PeriodMode.YEAR, result.mode)
    }

    // ── nextMonth ────────────────────────────────────────────────────────────

    @Test
    fun `nextMonth increments month within year`() {
        repo.setPeriod(AppMonth(2025, 4)) // May
        repo.nextMonth()
        val result = repo.month.value
        assertEquals(2025, result.year)
        assertEquals(5, result.month) // June
    }

    @Test
    fun `nextMonth rolls year forward from December to January`() {
        repo.setPeriod(AppMonth(2024, 11)) // December
        repo.nextMonth()
        val result = repo.month.value
        assertEquals(2025, result.year)
        assertEquals(0, result.month) // January
    }

    @Test
    fun `nextMonth in YEAR mode increments year`() {
        repo.setPeriod(AppMonth(2025, 0, PeriodMode.YEAR))
        repo.nextMonth()
        val result = repo.month.value
        assertEquals(2026, result.year)
        assertEquals(PeriodMode.YEAR, result.mode)
    }

    // ── goToMonth ────────────────────────────────────────────────────────────

    @Test
    fun `goToMonth sets correct year and month`() {
        repo.goToMonth(2023, 8) // September 2023
        val result = repo.month.value
        assertEquals(2023, result.year)
        assertEquals(8, result.month)
        assertEquals(PeriodMode.MONTH, result.mode)
    }

    // ── computeRange ─────────────────────────────────────────────────────────

    @Test
    fun `computeRange MONTH January 2025 starts at day 1`() {
        val (from, _) = repo.computeRange(AppMonth(2025, 0))
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
    }

    @Test
    fun `computeRange MONTH January 2025 ends on 31st at 23 59 59`() {
        val (_, to) = repo.computeRange(AppMonth(2025, 0))
        val cal = Calendar.getInstance().apply { timeInMillis = to }
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
    }

    @Test
    fun `computeRange MONTH February 2024 leap year ends on 29th`() {
        val (_, to) = repo.computeRange(AppMonth(2024, 1))
        val cal = Calendar.getInstance().apply { timeInMillis = to }
        assertEquals(29, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `computeRange MONTH February 2025 non leap year ends on 28th`() {
        val (_, to) = repo.computeRange(AppMonth(2025, 1))
        val cal = Calendar.getInstance().apply { timeInMillis = to }
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `computeRange TODAY from is start of day`() {
        val (from, _) = repo.computeRange(AppMonth(2025, 0, PeriodMode.TODAY))
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
    }

    @Test
    fun `computeRange TODAY to is end of day`() {
        val (_, to) = repo.computeRange(AppMonth(2025, 0, PeriodMode.TODAY))
        val cal = Calendar.getInstance().apply { timeInMillis = to }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
    }

    @Test
    fun `computeRange YEAR 2025 starts at Jan 1`() {
        val (from, _) = repo.computeRange(AppMonth(2025, 0, PeriodMode.YEAR))
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `computeRange YEAR 2025 ends at Dec 31`() {
        val (_, to) = repo.computeRange(AppMonth(2025, 0, PeriodMode.YEAR))
        val cal = Calendar.getInstance().apply { timeInMillis = to }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(11, cal.get(Calendar.MONTH))
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `computeRange ALL returns 0 to Long MAX VALUE divided by 2`() {
        val (from, to) = repo.computeRange(AppMonth(2025, 0, PeriodMode.ALL))
        assertEquals(0L, from)
        assertEquals(Long.MAX_VALUE / 2, to)
    }

    @Test
    fun `computeRange RANGE returns custom fromMillis and toMillis`() {
        val customFrom = 1_000_000L
        val customTo = 9_000_000L
        val (from, to) = repo.computeRange(AppMonth(2025, 0, PeriodMode.RANGE, customFrom, customTo))
        assertEquals(customFrom, from)
        assertEquals(customTo, to)
    }

    // ── daysInPeriod ─────────────────────────────────────────────────────────

    @Test
    fun `daysInPeriod MONTH January returns 31`() {
        assertEquals(31, repo.daysInPeriod(AppMonth(2025, 0)))
    }

    @Test
    fun `daysInPeriod MONTH February 2024 leap returns 29`() {
        assertEquals(29, repo.daysInPeriod(AppMonth(2024, 1)))
    }

    @Test
    fun `daysInPeriod MONTH February 2025 non leap returns 28`() {
        assertEquals(28, repo.daysInPeriod(AppMonth(2025, 1)))
    }

    @Test
    fun `daysInPeriod TODAY returns 1`() {
        assertEquals(1, repo.daysInPeriod(AppMonth(2025, 0, PeriodMode.TODAY)))
    }

    @Test
    fun `daysInPeriod WEEK returns 7`() {
        assertEquals(7, repo.daysInPeriod(AppMonth(2025, 0, PeriodMode.WEEK)))
    }

    @Test
    fun `daysInPeriod YEAR 2024 leap returns 366`() {
        assertEquals(366, repo.daysInPeriod(AppMonth(2024, 0, PeriodMode.YEAR)))
    }

    @Test
    fun `daysInPeriod YEAR 2025 non leap returns 365`() {
        assertEquals(365, repo.daysInPeriod(AppMonth(2025, 0, PeriodMode.YEAR)))
    }

    @Test
    fun `daysInPeriod ALL returns 0`() {
        assertEquals(0, repo.daysInPeriod(AppMonth(2025, 0, PeriodMode.ALL)))
    }

    @Test
    fun `daysInPeriod RANGE calculates from timestamps`() {
        val from = 0L
        val to = 2 * 86_400_000L  // 2 days in ms
        val days = repo.daysInPeriod(AppMonth(2025, 0, PeriodMode.RANGE, from, to))
        assertEquals(3, days) // +1 inclusive
    }

    // ── pillLabel ────────────────────────────────────────────────────────────

    @Test
    fun `pillLabel MONTH returns uppercase Ukrainian month name with year`() {
        val label = repo.pillLabel(AppMonth(2025, 4)) // May = index 4
        assertEquals("ТРАВЕНЬ 2025", label)
    }

    @Test
    fun `pillLabel YEAR returns year as string`() {
        val label = repo.pillLabel(AppMonth(2025, 0, PeriodMode.YEAR))
        assertEquals("2025", label)
    }

    @Test
    fun `pillLabel ALL returns fixed Ukrainian string`() {
        val label = repo.pillLabel(AppMonth(2025, 0, PeriodMode.ALL))
        assertEquals("ВІД ПОЧАТКУ", label)
    }

    @Test
    fun `pillLabel MONTH for January returns correct name`() {
        val label = repo.pillLabel(AppMonth(2025, 0))
        assertTrue(label.contains("СІЧЕНЬ"))
        assertTrue(label.contains("2025"))
    }

    @Test
    fun `pillLabel MONTH for December returns correct name`() {
        val label = repo.pillLabel(AppMonth(2025, 11))
        assertTrue(label.contains("ГРУДЕНЬ"))
    }

    // ── pillBadge ────────────────────────────────────────────────────────────

    @Test
    fun `pillBadge TODAY returns 1`() {
        assertEquals("1", repo.pillBadge(AppMonth(2025, 0, PeriodMode.TODAY)))
    }

    @Test
    fun `pillBadge WEEK returns 7`() {
        assertEquals("7", repo.pillBadge(AppMonth(2025, 0, PeriodMode.WEEK)))
    }

    @Test
    fun `pillBadge ALL returns infinity symbol`() {
        assertEquals("∞", repo.pillBadge(AppMonth(2025, 0, PeriodMode.ALL)))
    }

    @Test
    fun `pillBadge MONTH January returns 31`() {
        assertEquals("31", repo.pillBadge(AppMonth(2025, 0)))
    }

    @Test
    fun `pillBadge MONTH February 2024 leap returns 29`() {
        assertEquals("29", repo.pillBadge(AppMonth(2024, 1)))
    }

    // ── setPeriod ────────────────────────────────────────────────────────────

    @Test
    fun `setPeriod updates month state flow`() {
        val target = AppMonth(2020, 5, PeriodMode.WEEK)
        repo.setPeriod(target)
        assertEquals(target, repo.month.value)
    }
}
