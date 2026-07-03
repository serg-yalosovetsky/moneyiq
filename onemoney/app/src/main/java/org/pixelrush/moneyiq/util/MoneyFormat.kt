package org.syalosovetskyi.onemoney.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Форматування грошових сум. Ціле число — без дробової частини, інакше 2 знаки.
 * Раніше жила в MainScreen.kt і імпортувалася по всьому застосунку — суто
 * утиліта, тому винесена сюди без зміни поведінки.
 */
fun formatMoney(amount: Double, currency: String = ""): String {
    val isWhole = amount == kotlin.math.floor(amount) && !amount.isInfinite()
    val nf = NumberFormat.getNumberInstance(Locale.getDefault())
    nf.minimumFractionDigits = if (isWhole) 0 else 2
    nf.maximumFractionDigits = if (isWhole) 0 else 2
    return if (currency.isNotBlank()) "${nf.format(amount)} $currency" else nf.format(amount)
}
