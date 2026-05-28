package org.pixelrush.moneyiq.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.pixelrush.moneyiq.ui.components.calculator.SharedCalcKeypad
import org.pixelrush.moneyiq.ui.components.calculator.rememberCalcState
import org.pixelrush.moneyiq.ui.main.formatMoney

// ── Діалог введення бюджету категорії ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetInputSheet(
    catRow:      BudgetCatRow,
    monthLabel:  String,
    accentColor: Color,
    amountLabel: String = "витрачено",
    onDismiss:   () -> Unit,
    onConfirm:   (Double) -> Unit
) {
    val catColor = remember(catRow.category.colorHex) {
        try { Color(android.graphics.Color.parseColor(catRow.category.colorHex)) }
        catch (_: Exception) { accentColor }
    }

    // ── Стан калькулятора ─────────────────────────────────────────────────
    val calc        = rememberCalcState(catRow.category.budgetAmount)
    val displayText = calc.displayExpr("₴")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = catColor,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))

                // Назва категорії
                Text(
                    catRow.category.name,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Spacer(Modifier.height(10.dp))

                // Місяць + суми
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(monthLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f))
                        Text("$amountLabel ${formatMoney(catRow.amount)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                "${formatMoney(catRow.amount)} ₴",
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("в бюджеті ${formatMoney(catRow.category.budgetAmount)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Белая секция с клавиатурой
                Column(
                    modifier            = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Бюджет на місяць",
                        style = MaterialTheme.typography.labelLarge,
                        color = catColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = displayText,
                        style      = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color      = catColor,
                        maxLines   = 1,
                        overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    SharedCalcKeypad(
                        calc         = calc,
                        modifier     = Modifier.fillMaxWidth().height(252.dp),
                        confirmColor = catColor,
                        onConfirm    = { onConfirm(calc.result()) }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // FAB-иконка категории в правом верхнем углу
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    resolvedCatIcon(catRow.category.icon, catRow.category.name, catRow.category.type), null,
                    modifier = Modifier.size(28.dp),
                    tint     = Color.White
                )
            }
        }
    }
}

// ── Budget settings bottom sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetSettingsSheet(
    monthLabel:          String,
    currentExpensesMode: Boolean,
    onToggleMode:        (Boolean) -> Unit,
    onDeleteBudget:      () -> Unit,
    onDismiss:           () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header: back arrow + month label
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    monthLabel,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // "Операції" section header
            Text(
                "Операції",
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // "Поточні витрати" checkbox item
            ListItem(
                modifier          = Modifier.clickable { onToggleMode(!currentExpensesMode) },
                leadingContent    = {
                    Checkbox(
                        checked         = currentExpensesMode,
                        onCheckedChange = { onToggleMode(it) }
                    )
                },
                headlineContent   = { Text("Поточні витрати") },
                supportingContent = {
                    Text(monthLabel,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(8.dp))

            // "Видалити бюджет"
            ListItem(
                modifier       = Modifier.clickable(onClick = onDeleteBudget),
                leadingContent = {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                },
                headlineContent = {
                    Text("Видалити бюджет", color = MaterialTheme.colorScheme.error)
                }
            )
        }
    }
}

