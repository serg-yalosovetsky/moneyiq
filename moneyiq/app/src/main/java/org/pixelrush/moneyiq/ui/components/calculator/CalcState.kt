package org.pixelrush.moneyiq.ui.components.calculator

import androidx.compose.runtime.*
import java.text.NumberFormat
import java.util.Locale

@Stable
class CalcStateHolder(initial: Double = 0.0) {

    var currentStr by mutableStateOf(
        when {
            initial <= 0.0                         -> "0"
            initial == initial.toLong().toDouble() -> initial.toLong().toString()
            else -> initial.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
        }
    )
    var pendingVal by mutableStateOf(0.0)
    var pendingOp  by mutableStateOf<String?>(null)

    fun result(): Double {
        val c = currentStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        return when (pendingOp) {
            "+"  -> pendingVal + c
            "−"  -> pendingVal - c
            "×"  -> pendingVal * c
            "÷"  -> if (c != 0.0) pendingVal / c else pendingVal
            else -> c
        }
    }

    fun displayExpr(symbol: String = "₴"): String {
        val nf = NumberFormat.getNumberInstance(Locale.getDefault())
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 2
        return if (pendingOp != null)
            "${nf.format(pendingVal)} $pendingOp $currentStr $symbol"
        else
            "$currentStr $symbol"
    }

    fun onKey(key: String) {
        when (key) {
            "÷", "×", "−", "+" -> { pendingVal = result(); pendingOp = key; currentStr = "0" }
            "=" -> if (pendingOp != null) {
                val r = result()
                currentStr = when {
                    r == r.toLong().toDouble() -> r.toLong().toString()
                    else -> r.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
                }
                pendingOp  = null
                pendingVal = 0.0
            }
            "⌫" -> currentStr = if (currentStr.length <= 1) "0" else currentStr.dropLast(1)
            "C" -> { currentStr = "0"; pendingOp = null; pendingVal = 0.0 }
            "," -> {
                if ("," !in currentStr && "." !in currentStr)
                    currentStr = if (currentStr == "0") "0," else "$currentStr,"
            }
            else -> {
                val dotIdx = currentStr.indexOfFirst { it == ',' || it == '.' }
                currentStr = when {
                    dotIdx >= 0 && currentStr.length - dotIdx > 2 -> currentStr
                    currentStr == "0"      -> key
                    currentStr.length < 12 -> "$currentStr$key"
                    else                   -> currentStr
                }
            }
        }
    }
}

@Composable
fun rememberCalcState(initial: Double = 0.0): CalcStateHolder =
    remember { CalcStateHolder(initial) }
