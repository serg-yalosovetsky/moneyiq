package org.pixelrush.moneyiq.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// Единые украинские названия месяцев для всего приложения
val MONTH_NAMES_UA = arrayOf(
    "СІЧЕНЬ", "ЛЮТИЙ", "БЕРЕЗЕНЬ", "КВІТЕНЬ", "ТРАВЕНЬ", "ЧЕРВЕНЬ",
    "ЛИПЕНЬ", "СЕРПЕНЬ", "ВЕРЕСЕНЬ", "ЖОВТЕНЬ", "ЛИСТОПАД", "ГРУДЕНЬ"
)

val MONTH_NAMES_UA_FULL = arrayOf(
    "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
    "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
)

/** Единое состояние выбранного месяца, общее для всех вкладок. */
data class AppMonth(val year: Int, val month: Int)

/**
 * @Singleton — один экземпляр на всё приложение.
 * Все ViewModels подписываются на [month], поэтому смена месяца на одном экране
 * мгновенно отражается на всех остальных.
 */
@Singleton
class SelectedMonthRepository @Inject constructor() {

    private val _month = MutableStateFlow(currentMonth())
    val month: StateFlow<AppMonth> = _month.asStateFlow()

    fun prevMonth() {
        _month.value = _month.value.run {
            if (month == 0) AppMonth(year - 1, 11) else AppMonth(year, month - 1)
        }
    }

    fun nextMonth() {
        _month.value = _month.value.run {
            if (month == 11) AppMonth(year + 1, 0) else AppMonth(year, month + 1)
        }
    }

    fun daysInMonth(year: Int, month: Int): Int =
        Calendar.getInstance().also { it.set(year, month, 1) }
            .getActualMaximum(Calendar.DAY_OF_MONTH)

    private fun currentMonth(): AppMonth = Calendar.getInstance().run {
        AppMonth(get(Calendar.YEAR), get(Calendar.MONTH))
    }
}
