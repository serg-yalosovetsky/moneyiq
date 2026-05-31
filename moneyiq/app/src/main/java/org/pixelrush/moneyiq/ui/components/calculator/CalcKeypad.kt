package org.pixelrush.moneyiq.ui.components.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun QKey(
    label:   String,
    modifier: Modifier = Modifier,
    isOp:    Boolean   = false,
    bg:      Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 20.sp,
            fontWeight = if (isOp) FontWeight.Normal else FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SharedCalcKeypad(
    calc:           CalcStateHolder,
    modifier:       Modifier = Modifier,
    currencySymbol: String   = "₴",
    confirmColor:   Color    = Color(0xFF4CAF50),
    onConfirm:      () -> Unit,
    row2ExtraKey:   (@Composable RowScope.() -> Unit)? = null
) {
    val gap   = 3.dp
    val keyBg = MaterialTheme.colorScheme.surfaceVariant
    val opBg  = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier            = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            QKey("÷", Modifier.weight(1f), isOp = true, bg = opBg) { calc.onKey("÷") }
            QKey("7", Modifier.weight(1f), bg = keyBg) { calc.onKey("7") }
            QKey("8", Modifier.weight(1f), bg = keyBg) { calc.onKey("8") }
            QKey("9", Modifier.weight(1f), bg = keyBg) { calc.onKey("9") }
            Box(
                modifier         = Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp)).background(keyBg)
                    .clickable { calc.onKey("⌫") },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Outlined.Backspace, null, modifier = Modifier.size(20.dp)) }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            QKey("×", Modifier.weight(1f), isOp = true, bg = opBg) { calc.onKey("×") }
            QKey("4", Modifier.weight(1f), bg = keyBg) { calc.onKey("4") }
            QKey("5", Modifier.weight(1f), bg = keyBg) { calc.onKey("5") }
            QKey("6", Modifier.weight(1f), bg = keyBg) { calc.onKey("6") }
            if (row2ExtraKey != null) {
                row2ExtraKey()
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(keyBg)
                        .clickable { calc.onKey("C") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "C",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Row(
            modifier              = Modifier.weight(2f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("−", Modifier.weight(1f).fillMaxWidth(), isOp = true, bg = opBg) { calc.onKey("−") }
                QKey("+", Modifier.weight(1f).fillMaxWidth(), isOp = true, bg = opBg) { calc.onKey("+") }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("1", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("1") }
                QKey(currencySymbol, Modifier.weight(1f).fillMaxWidth(), bg = keyBg) {}
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("2", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("2") }
                QKey("0", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("0") }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("3", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("3") }
                QKey(",", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey(",") }
            }
            val hasPendingOp = calc.pendingOp != null
            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(confirmColor)
                    .clickable { if (hasPendingOp) calc.onKey("=") else onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                if (hasPendingOp) {
                    Text("=", fontSize = 32.sp, fontWeight = FontWeight.Normal, color = Color.White)
                } else {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountCalculatorSheet(
    initial:        Double = 0.0,
    currencySymbol: String = "₴",
    title:          String = "Сума",
    onResult:       (Double) -> Unit,
    onDismiss:      () -> Unit
) {
    val calc    = rememberCalcState(initial)
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.58f)) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                Text(
                    text       = calc.displayExpr(currencySymbol),
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            SharedCalcKeypad(
                calc           = calc,
                modifier       = Modifier.weight(1f).fillMaxWidth(),
                currencySymbol = currencySymbol,
                onConfirm      = { onResult(calc.result()) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
