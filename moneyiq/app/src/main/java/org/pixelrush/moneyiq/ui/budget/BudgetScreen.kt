package org.pixelrush.moneyiq.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.util.suggestCategoryStyle

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun BudgetScreen(
    padding:           PaddingValues  = PaddingValues(),
    embeddedMode:      Boolean        = false,
    showSettings:      Boolean        = false,
    onSettingsDismiss: () -> Unit     = {},
    viewModel:         BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val expenseColor        = Color(0xFFD81B60)
    val incomeColor         = Color(0xFF26A69A)
    val monthLabel          = "${MONTH_NAMES_UA_FULL[state.selectedMonth.month]} ${state.selectedMonth.year}"
    var showSettingsSheet   by remember { mutableStateOf(false) }
    var currentExpensesMode by remember { mutableStateOf(false) }
    val settingsVisible     = showSettings || showSettingsSheet
    var incomeBudgetRow     by remember { mutableStateOf<BudgetCatRow?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
            .horizontalSwipe(
                onSwipeLeft  = viewModel::nextMonth,
                onSwipeRight = viewModel::prevMonth
            )
    ) {
        if (!embeddedMode) {
            BudgetTopBar(
                totalBalance    = state.totalBalance,
                onSettingsClick = { showSettingsSheet = true }
            )
        }

        SharedMonthNavPill(
            appMonth       = state.appMonth,
            daysInPeriod   = state.daysInMonth,
            onPrev         = viewModel::prevMonth,
            onNext         = viewModel::nextMonth,
            onSelectPeriod = viewModel::setPeriod
        )

        LazyColumn(
            modifier       = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                BudgetSectionCard(
                    data                = state.expenseSection,
                    title               = "Витрати",
                    amountLabel         = "витрачено",
                    accentColor         = expenseColor,
                    monthLabel          = monthLabel,
                    currentExpensesMode = currentExpensesMode,
                    onUpdateBudget      = viewModel::updateCategoryBudget
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SavingsSectionCard(
                    expenseTotal  = state.expenseSection.totalAmount,
                    incomeTotal   = state.incomeSection.totalAmount,
                    incomeBudget  = state.incomeSection.totalBudget,
                    expenseBudget = state.expenseSection.totalBudget,
                    daysInMonth   = state.daysInMonth,
                    daysPassed    = state.daysPassed
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                BudgetSectionCard(
                    data                = state.incomeSection,
                    title               = "Доходи",
                    amountLabel         = "отримано",
                    accentColor         = incomeColor,
                    monthLabel          = monthLabel,
                    currentExpensesMode = currentExpensesMode,
                    onUpdateBudget      = viewModel::updateCategoryBudget
                )
            }
        }
        IncomeBudgetBar(
            incomeSection = state.incomeSection,
            expenseTotal  = state.expenseSection.totalAmount,
            onClick = {
                val row = state.incomeSection.rows.firstOrNull { it.category.budgetAmount == 0.0 }
                    ?: state.incomeSection.rows.firstOrNull()
                incomeBudgetRow = row
            },
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        )
    }

    incomeBudgetRow?.let { row ->
        BudgetInputSheet(
            catRow      = row,
            monthLabel  = monthLabel,
            accentColor = incomeColor,
            amountLabel = "отримано",
            onDismiss   = { incomeBudgetRow = null },
            onConfirm   = { newBudget ->
                viewModel.updateCategoryBudget(row.category, newBudget)
                incomeBudgetRow = null
            }
        )
    }

    if (settingsVisible) {
        BudgetSettingsSheet(
            monthLabel          = monthLabel,
            currentExpensesMode = currentExpensesMode,
            onToggleMode        = { currentExpensesMode = it },
            onDeleteBudget      = { viewModel.clearAllBudgets(); showSettingsSheet = false; onSettingsDismiss() },
            onDismiss           = { showSettingsSheet = false; onSettingsDismiss() }
        )
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetTopBar(totalBalance: Double, onSettingsClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier            = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Всі рахунки",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(totalBalance),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        IconButton(
            onClick  = onSettingsClick,
            modifier = Modifier.size(44.dp).clip(CircleShape)
        ) {
            Icon(
                Icons.Outlined.Speed, "Настройки бюджета",
                tint     = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Іконка категорії з авто-підказкою ────────────────────────────────────────

internal fun resolvedCatIcon(iconName: String, catName: String, type: TransactionType) =
    categoryIconFor(
        if (iconName == "category") suggestCategoryStyle(catName, type).first else iconName
    )

// ── Секция бюджета (Расходы / Доходы) ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSectionCard(
    data:                BudgetSectionData,
    title:               String,
    amountLabel:         String,
    accentColor:         Color,
    monthLabel:          String,
    currentExpensesMode: Boolean = false,
    onUpdateBudget:      (CategoryEntity, Double) -> Unit
) {
    var expanded    by remember { mutableStateOf(false) }
    var editingRow  by remember { mutableStateOf<BudgetCatRow?>(null) }

    val budgetedRows = if (currentExpensesMode) emptyList()
                      else data.rows.filter { it.category.budgetAmount > 0 }
    val chipRows     = if (currentExpensesMode) data.rows.filter { it.amount > 0 }.sortedByDescending { it.amount }
                      else data.rows.filter { it.category.budgetAmount == 0.0 && it.amount > 0 }
    val visibleChips = if (expanded) chipRows else chipRows.take(3)
    val hasMoreChips = chipRows.size > 3
    val remaining    = data.totalBudget - data.totalAmount

    editingRow?.let { row ->
        BudgetInputSheet(
            catRow      = row,
            monthLabel  = monthLabel,
            accentColor = accentColor,
            amountLabel = amountLabel,
            onDismiss   = { editingRow = null },
            onConfirm   = { newBudget ->
                onUpdateBudget(row.category, newBudget)
                editingRow = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.06f))
    ) {
        // ── Section header ────────────────────────────────────────────────
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(title,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${formatMoney(remaining)} ₴",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = when {
                            remaining < 0   -> MaterialTheme.colorScheme.error
                            remaining > 0   -> accentColor
                            else            -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$amountLabel ${formatMoney(data.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (data.totalAmount > 0) accentColor
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Text("в бюджеті ${formatMoney(data.totalBudget)} ₴",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
        }

        // ── Budgeted categories — full rows ──────────────────────────────
        budgetedRows.forEach { row ->
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            BudgetCatFullRow(
                row         = row,
                accentColor = accentColor,
                onClick     = { editingRow = row }
            )
        }

        // ── Chip-row for unbudgeted (or all in currentExpensesMode) ──────
        if (chipRows.isNotEmpty()) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            val rowBg = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            if (!expanded) {
                Row(modifier = rowBg) {
                    visibleChips.forEach { row ->
                        Box(Modifier.weight(1f)) {
                            BudgetCatChip(row = row, accentColor = accentColor, onClick = { editingRow = row })
                        }
                    }
                    if (hasMoreChips) {
                        Box(Modifier.weight(1f)) {
                            MoreLessChip(
                                expanded  = false,
                                remaining = chipRows.size - 3,
                                onClick   = { expanded = true }
                            )
                        }
                    }
                }
            } else {
                val totalItems = chipRows.size + 1
                Column(modifier = rowBg) {
                    (0 until totalItems).chunked(4).forEach { indices ->
                        Row(Modifier.fillMaxWidth()) {
                            indices.forEach { i ->
                                Box(Modifier.weight(1f)) {
                                    if (i < chipRows.size) {
                                        val r = chipRows[i]
                                        BudgetCatChip(row = r, accentColor = accentColor, onClick = { editingRow = r })
                                    } else {
                                        MoreLessChip(expanded = true, remaining = 0, onClick = { expanded = false })
                                    }
                                }
                            }
                            repeat(4 - indices.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

// ── Full row for a budgeted category ─────────────────────────────────────────

@Composable
private fun BudgetCatFullRow(
    row:         BudgetCatRow,
    accentColor: Color,
    onClick:     () -> Unit
) {
    val color = remember(row.category.colorHex) {
        try { Color(android.graphics.Color.parseColor(row.category.colorHex)) }
        catch (_: Exception) { accentColor }
    }
    val remaining = row.category.budgetAmount - row.amount

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                resolvedCatIcon(row.category.icon, row.category.name, row.category.type), null,
                tint     = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.category.name,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(formatMoney(row.amount),
                style = MaterialTheme.typography.labelMedium,
                color = color)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${formatMoney(remaining)} ₴",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = when {
                    remaining < 0 -> MaterialTheme.colorScheme.error
                    remaining > 0 -> accentColor
                    else          -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
            Text("в бюджеті ${formatMoney(row.category.budgetAmount)} ₴",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}

// ── Чип отдельной категории ───────────────────────────────────────────────────

@Composable
private fun BudgetCatChip(
    row:         BudgetCatRow,
    accentColor: Color,
    onClick:     () -> Unit
) {
    val color = remember(row.category.colorHex) {
        try { Color(android.graphics.Color.parseColor(row.category.colorHex)) }
        catch (_: Exception) { accentColor }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            row.category.name,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurface,
            modifier  = Modifier.fillMaxWidth().heightIn(min = 32.dp),
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight
        )
        Spacer(Modifier.height(2.dp))
        val hasAmount = row.amount > 0
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (hasAmount) color else color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                resolvedCatIcon(row.category.icon, row.category.name, row.category.type), null,
                tint     = if (hasAmount) Color.White else color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatMoney(row.amount)} ₴",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = color,
            maxLines   = 1,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

// ── Чип «Больше...» / «Свернуть» ─────────────────────────────────────────────

@Composable
private fun MoreLessChip(expanded: Boolean, remaining: Int, onClick: () -> Unit) {
    val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
    val onSurfVar  = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (expanded) "Згорнути" else "Більше...",
            style     = MaterialTheme.typography.labelSmall,
            color     = onSurfVar,
            maxLines  = 2,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().heightIn(min = 32.dp)
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(surfaceVar),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint     = onSurfVar,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (!expanded) "+$remaining" else " ",
            style     = MaterialTheme.typography.labelSmall,
            color     = onSurfVar,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Секция заощаджень з прогнозом ────────────────────────────────────────────

@Composable
private fun SavingsSectionCard(
    expenseTotal:  Double,
    incomeTotal:   Double,
    incomeBudget:  Double,
    expenseBudget: Double,
    daysInMonth:   Int,
    daysPassed:    Int
) {
    val savingsColor = Color(0xFF26A69A)
    val negColor     = Color(0xFFD81B60)

    // Реальні заощадження = отримано - витрачено (для підпису «збережено»)
    val realSavings = incomeTotal - expenseTotal

    // Заощадження за бюджетом = бюджет доходів - витрачено (для заголовку)
    val actualSavings = if (incomeBudget > 0) incomeBudget - expenseTotal else realSavings

    // Прогноз витрат на кінець місяця (лінійна екстраполяція по днях)
    val hasForecast = daysPassed > 0 && daysInMonth > daysPassed && expenseTotal > 0
    val projectedExpenses = if (hasForecast) expenseTotal / daysPassed * daysInMonth else expenseTotal
    val projectedSavings  = if (incomeBudget > 0) incomeBudget - projectedExpenses
                            else realSavings - (projectedExpenses - expenseTotal)

    // Бюджетні заощадження = бюджет доходів - бюджет витрат
    val budgetSavings = incomeBudget - expenseBudget

    // Що показуємо в заголовку: прогноз (якщо є) або бюджетні заощадження
    val headerAmount = if (hasForecast && (incomeBudget > 0 || incomeTotal > 0)) projectedSavings
                       else actualSavings
    val headerColor  = if (headerAmount >= 0) savingsColor else negColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(savingsColor.copy(alpha = 0.06f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(savingsColor))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Заощадження",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasForecast) {
                            Text(
                                "пройшло $daysPassed з $daysInMonth днів",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${formatMoney(headerAmount)} ₴",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = headerColor
                        )
                        if (hasForecast) {
                            Text(
                                "прогноз",
                                style = MaterialTheme.typography.labelSmall,
                                color = headerColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "збережено ${formatMoney(realSavings)} ₴",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (realSavings >= 0) savingsColor.copy(alpha = 0.8f)
                                else negColor.copy(alpha = 0.8f)
                    )
                    if (hasForecast && expenseTotal > 0) {
                        Text(
                            "витрати до кінця ~${formatMoney(projectedExpenses)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    } else if (budgetSavings != 0.0 && incomeBudget > 0) {
                        Text(
                            "в бюджеті ${formatMoney(budgetSavings)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

// ── Нижня панель доходу (prompt → доступно в бюджеті) ────────────────────────

@Composable
private fun IncomeBudgetBar(
    incomeSection: BudgetSectionData,
    expenseTotal:  Double,
    onClick:       () -> Unit,
    modifier:      Modifier = Modifier
) {
    val hasBudget     = incomeSection.totalBudget > 0.0
    val hasCategories = incomeSection.rows.isNotEmpty()
    val overspend     = expenseTotal - incomeSection.totalBudget
    val overspendColor = Color(0xFFD81B60)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!hasBudget && hasCategories) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (hasBudget && overspend > 0)
                    overspendColor.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                hasBudget && overspend > 0 -> {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "перевитрата  ",
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color     = overspendColor
                        )
                        Text(
                            "${formatMoney(overspend)} ₴",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = overspendColor
                        )
                    }
                }
                hasBudget -> {
                    val available = incomeSection.totalBudget - expenseTotal
                    Text(
                        "Доступно в бюджеті: ${formatMoney(available)} ₴",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }
                else -> {
                    Text(
                        "Введіть суму очікуваного доходу...",
                        style     = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                    )
                }
            }
        }
    }
}
