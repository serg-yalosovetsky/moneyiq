package org.syalosovetskyi.onemoney.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Централізовані формати дат. Раніше SimpleDateFormat зі шаблонами-рядками
 * дублювався у 17 місцях, причому один і той самий шаблон подекуди йшов то з
 * Locale.getDefault(), то з жорстким "uk" — звідси неконсистентність.
 *
 * Правило: дати, що ПОКАЗУЮТЬСЯ користувачу, форматуються за
 * Locale.getDefault() (слідують за мовою застосунку). Технічні/файлові дати
 * (імена бекапів тощо) — за Locale.US заради стабільності.
 *
 * SimpleDateFormat не потокобезпечний, тому створюємо новий екземпляр на виклик.
 */
object DateFormats {

    // ── UI (за мовою застосунку) ────────────────────────────────────────────────
    /** "3 лип." */
    fun dayMonthShort(date: Date): String = ui("d MMM").format(date)
    /** "3 липня" */
    fun dayMonthFull(date: Date): String = ui("d MMMM").format(date)
    /** "3 лип. 2026, 14:30" */
    fun dayMonthYearTime(date: Date): String = ui("d MMM yyyy, HH:mm").format(date)
    /** "3 лип. 2026 р." */
    fun dayMonthYearUk(date: Date): String = ui("d MMM yyyy 'р.'").format(date)
    /** "Липень 2026" */
    fun monthYear(date: Date): String = ui("LLLL yyyy").format(date)

    // ── Технічні / файлові (стабільний Locale) ──────────────────────────────────
    /** "2026-07-03" */
    fun isoDate(date: Date): String = tech("yyyy-MM-dd").format(date)
    /** "2026-07-03 14:30" */
    fun isoDateTime(date: Date): String = tech("yyyy-MM-dd HH:mm").format(date)
    /** "20260703_1430" — для імен файлів */
    fun fileStamp(date: Date): String = tech("yyyyMMdd_HHmm").format(date)
    /** "20260703" */
    fun fileStampDate(date: Date): String = tech("yyyyMMdd").format(date)

    private fun ui(pattern: String)   = SimpleDateFormat(pattern, Locale.getDefault())
    private fun tech(pattern: String) = SimpleDateFormat(pattern, Locale.US)
}
