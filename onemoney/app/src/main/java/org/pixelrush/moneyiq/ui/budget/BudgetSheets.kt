package org.syalosovetskyi.onemoney.ui.budget

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.background
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.border
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.clickable
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.layout.*
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.CircleShape
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.Icons
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.filled.*
import androidx.core.graphics.toColorInt
import androidx.compose.material3.*
import androidx.core.graphics.toColorInt
import androidx.compose.runtime.*
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Alignment
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Modifier
import androidx.core.graphics.toColorInt
import androidx.compose.ui.draw.clip
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.toColorInt
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.lazy.items
import androidx.core.graphics.toColorInt
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import androidx.compose.ui.window.DialogProperties
import org.syalosovetskyi.onemoney.ui.components.calculator.SharedCalcKeypad
import org.syalosovetskyi.onemoney.ui.components.calculator.rememberCalcState
import org.syalosovetskyi.onemoney.ui.main.formatMoney
import org.syalosovetskyi.onemoney.ui.components.currency.CurrencyPageContent
import org.syalosovetskyi.onemoney.ui.components.icons.CircleIconBox
import org.syalosovetskyi.onemoney.ui.settings.data.CURRENCIES_ALL

// ── Діалог введення бюджету категорії ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetInputSheet(
    catRow:      BudgetCatRow,
    monthLabel:  String,
    accentColor: Color,
    amountLabel: String = "витрачено",
    onIconClick: (() -> Unit)? = null,
    onDismiss:   () -> Unit,
    onConfirm:   (Double, String) -> Unit
) {
    val catColor = remember(catRow.category.colorHex) {
        try { Color(catRow.category.colorHex.toColorInt()) }
        catch (_: Exception) { accentColor }
    }
    val isLightBg    = catColor.luminance() > 0.5f
    val onCatColor   = if (isLightBg) Color(0xFF1C1B1F) else Color.White
    val displayColor = if (isLightBg) Color(0xFF1C1B1F) else catColor

    var pickedCurrency     by remember { mutableStateOf(catRow.category.currencyCode) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    LaunchedEffect(catRow.category.id) { pickedCurrency = catRow.category.currencyCode }
    val currencySymbol = CURRENCIES_ALL.find { it.code == pickedCurrency }?.symbol ?: pickedCurrency

    val calc           = rememberCalcState(catRow.category.budgetAmount)
    val displayText    = calc.displayExpr(currencySymbol)
    val budgetProgress = if (catRow.category.budgetAmount > 0.0) {
        (catRow.amount / catRow.category.budgetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

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
                        .background(onCatColor.copy(alpha = 0.35f))
                )
            }
        }
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 26.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(22.dp))

                Text(
                    catRow.category.name,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = onCatColor
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(monthLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCatColor.copy(alpha = 0.9f))
                        Text("$amountLabel ${formatMoney(catRow.amount)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCatColor.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = onCatColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${formatMoney(catRow.amount)} ₴",
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = onCatColor
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("в бюджеті ${formatMoney(catRow.category.budgetAmount)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCatColor.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress   = { budgetProgress },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color      = onCatColor,
                    trackColor = onCatColor.copy(alpha = 0.25f)
                )

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier            = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Бюджет на місяць",
                        style = MaterialTheme.typography.labelLarge,
                        color = displayColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = displayText,
                        style      = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color      = displayColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    SharedCalcKeypad(
                        calc            = calc,
                        modifier        = Modifier.fillMaxWidth().height(252.dp),
                        currencySymbol  = currencySymbol,
                        confirmColor    = displayColor,
                        onCurrencyClick = { showCurrencyPicker = true },
                        onConfirm       = { onConfirm(calc.result(), pickedCurrency) }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp)
                    .offset(y = (-18).dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), CircleShape)
                    .then(if (onIconClick != null) Modifier.clickable(onClick = onIconClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    resolvedCatIcon(catRow.category.icon, catRow.category.name, catRow.category.type), null,
                    modifier = Modifier.size(34.dp),
                    tint     = displayColor
                )
            }
        }
    }

    if (showCurrencyPicker) {
        Dialog(
            onDismissRequest = { showCurrencyPicker = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CurrencyPageContent(
                title    = "Валюта бюджету",
                selected = pickedCurrency,
                onSelect = { code -> pickedCurrency = code; showCurrencyPicker = false },
                onClose  = { showCurrencyPicker = false }
            )
        }
    }
}

// ── Picker дохідних категорій ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncomeCategoryPickerSheet(
    rows:        List<BudgetCatRow>,
    monthLabel:  String,
    accentColor: Color,
    onPick:      (BudgetCatRow) -> Unit,
    onDismiss:   () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Text(
            "Бюджет доходів",
            modifier   = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            monthLabel,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(rows) { row ->
                val catColor = remember(row.category.colorHex) {
                    try { Color(row.category.colorHex.toColorInt()) }
                    catch (_: Exception) { accentColor }
                }
                ListItem(
                    modifier       = Modifier.clickable { onPick(row) },
                    leadingContent = {
                        CircleIconBox(
                            icon  = resolvedCatIcon(row.category.icon, row.category.name, row.category.type),
                            color = catColor
                        )
                    },
                    headlineContent = { Text(row.category.name) },
                    supportingContent = {
                        Text(
                            "отримано ${formatMoney(row.amount)} ₴",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.amount > 0) catColor
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    },
                    trailingContent = {
                        if (row.category.budgetAmount > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${formatMoney(row.category.budgetAmount)} ₴",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = accentColor
                                )
                                Text(
                                    "в бюджеті",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        } else {
                            Icon(Icons.Default.Add, null, tint = accentColor)
                        }
                    }
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
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

            Text(
                "Операції",
                modifier   = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                style      = MaterialTheme.typography.labelLarge,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
