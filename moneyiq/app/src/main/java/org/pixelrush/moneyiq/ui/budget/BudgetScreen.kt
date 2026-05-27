package org.pixelrush.moneyiq.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.util.*
import javax.inject.Inject

// ── Data classes ──────────────────────────────────────────────────────────────

data class BudgetSelMonth(val year: Int, val month: Int)

data class BudgetCatRow(
    val category: CategoryEntity,
    val amount:   Double        // потрачено (EXPENSE) или получено (INCOME)
)

data class BudgetSectionData(
    val totalBudget: Double,
    val totalAmount: Double,
    val rows:        List<BudgetCatRow>
)

data class BudgetUiState(
    val selectedMonth: BudgetSelMonth = run {
        val cal = Calendar.getInstance()
        BudgetSelMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    },
    val daysInMonth:    Int              = 31,
    val totalBalance:   Double           = 0.0,
    val expenseSection: BudgetSectionData = BudgetSectionData(0.0, 0.0, emptyList()),
    val incomeSection:  BudgetSectionData = BudgetSectionData(0.0, 0.0, emptyList())
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository,
    private val txRepo:       TransactionRepository,
    private val accountRepo:  AccountRepository,
    private val monthRepo:    SelectedMonthRepository         // ← общий репозиторий
) : ViewModel() {

    val state: StateFlow<BudgetUiState> = monthRepo.month.flatMapLatest { appMonth ->
        val sel = BudgetSelMonth(appMonth.year, appMonth.month)
        val (from, to) = monthRange(sel)
        combine(
            categoryRepo.getByType(TransactionType.EXPENSE),
            categoryRepo.getByType(TransactionType.INCOME),
            txRepo.getCategorySpending(TransactionType.EXPENSE, from, to),
            txRepo.getCategorySpending(TransactionType.INCOME, from, to),
            accountRepo.getTotalBalance()
        ) { expCats, incCats, expSpend, incSpend, rawBalance ->
            val expMap  = expSpend.associate { it.categoryId to it.total }
            val incMap  = incSpend.associate { it.categoryId to it.total }
            val expRows = expCats.map { BudgetCatRow(it, expMap[it.id] ?: 0.0) }
            val incRows = incCats.map { BudgetCatRow(it, incMap[it.id] ?: 0.0) }
            val cal     = Calendar.getInstance().also { it.set(sel.year, sel.month, 1) }
            BudgetUiState(
                selectedMonth = sel,
                daysInMonth   = cal.getActualMaximum(Calendar.DAY_OF_MONTH),
                totalBalance  = rawBalance ?: 0.0,
                expenseSection = BudgetSectionData(
                    totalBudget = expCats.sumOf { it.budgetAmount },
                    totalAmount = expRows.sumOf { it.amount },
                    rows        = expRows
                ),
                incomeSection = BudgetSectionData(
                    totalBudget = incCats.sumOf { it.budgetAmount },
                    totalAmount = incRows.sumOf { it.amount },
                    rows        = incRows
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    /** Делегируем навигацию в общий репозиторий */
    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)

    private fun monthRange(sel: BudgetSelMonth): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(sel.year, sel.month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59);      cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun BudgetScreen(
    padding:      PaddingValues = PaddingValues(),
    embeddedMode: Boolean       = false,
    viewModel:    BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor  = Color(0xFF26A69A)      // teal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
    ) {
        // ── Шапка ──────────────────────────────────────────────────────────
        if (!embeddedMode) {
            BudgetTopBar(totalBalance = state.totalBalance)
        }

        // ── Навигатор месяца ─────────────────────────────────────────────
        SharedMonthNavPill(
            year          = state.selectedMonth.year,
            month         = state.selectedMonth.month,
            daysInMonth   = state.daysInMonth,
            onPrev        = viewModel::prevMonth,
            onNext        = viewModel::nextMonth,
            onSelectMonth = viewModel::goToMonth
        )

        // ── Секции ──────────────────────────────────────────────────────
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp)
        ) {
            // Витрати
            item {
                BudgetSectionCard(
                    data          = state.expenseSection,
                    title         = "Витрати",
                    amountLabel   = "витрачено",
                    accentColor   = expenseColor
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            // Заощадження (статическая секция — Savings)
            item { SavingsSectionCard() }
            item { Spacer(Modifier.height(4.dp)) }
            // Доходы
            item {
                BudgetSectionCard(
                    data          = state.incomeSection,
                    title         = "Доходи",
                    amountLabel   = "отримано",
                    accentColor   = incomeColor
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            // Строка ожидаемого дохода
            item { ExpectedIncomeBar() }
        }
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetTopBar(totalBalance: Double) {
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
                "Все счета",
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
            onClick  = { /* TODO: настройки бюджета */ },
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

// ── Пилюля-навигатор месяца (идентична TransactionsListScreen) ────────────────

@Composable
private fun BudgetMonthNavPill(
    sel:         BudgetSelMonth,
    daysInMonth: Int,
    onPrev:      () -> Unit,
    onNext:      () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
            }
        }

        val pillAccent = Color(0xFFD81B60)
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = pillAccent.copy(alpha = 0.12f)
        ) {
            Row(
                modifier              = Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = CircleShape, color = pillAccent) {
                    Text(
                        "$daysInMonth",
                        modifier   = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
                Text(
                    "${MONTH_NAMES_UA[sel.month]} ${sel.year}",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = pillAccent
                )
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint     = pillAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(onClick = onNext) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Секция бюджета (Расходы / Доходы) ────────────────────────────────────────

@Composable
private fun BudgetSectionCard(
    data:        BudgetSectionData,
    title:       String,
    amountLabel: String,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleRows = if (expanded) data.rows else data.rows.take(3)
    val hasMore     = data.rows.size > 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.06f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Цветная левая полоса
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Строка 1: заголовок + сумма
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formatMoney(data.totalAmount),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = accentColor
                    )
                }

                // Строка 2: потрачено X / в бюджете X
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$amountLabel ${formatMoney(data.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        "в бюджете ${formatMoney(data.totalBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                // Чипы категорий
                if (data.rows.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))

                    // Обычный Row с horizontalScroll — LazyRow нельзя использовать внутри
                    // Row(Modifier.height(IntrinsicSize.Min)), т.к. не поддерживает intrinsic измерения
                    Row(
                        modifier              = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        visibleRows.forEach { row ->
                            BudgetCatChip(row = row, accentColor = accentColor)
                        }
                        if (hasMore) {
                            MoreLessChip(
                                expanded  = expanded,
                                remaining = data.rows.size - 3,
                                onClick   = { expanded = !expanded }
                            )
                        }
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

// ── Чип отдельной категории ───────────────────────────────────────────────────

@Composable
private fun BudgetCatChip(row: BudgetCatRow, accentColor: Color) {
    val color = remember(row.category.colorHex) {
        try { Color(android.graphics.Color.parseColor(row.category.colorHex)) }
        catch (_: Exception) { accentColor }
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            row.category.name,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurface,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .drawBehind {
                    drawCircle(
                        color  = color,
                        radius = size.minDimension / 2f,
                        style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Category, null,
                tint     = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            formatMoney(row.amount),
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
            .width(76.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (expanded) "Свернуть" else "Больше...",
            style     = MaterialTheme.typography.labelSmall,
            color     = onSurfVar,
            maxLines  = 1,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
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

// ── Секция заощаджень (статическая) ──────────────────────────────────────────

@Composable
private fun SavingsSectionCard() {
    val savingsColor = Color(0xFF26A69A)  // teal, как у доходов
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(savingsColor.copy(alpha = 0.06f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Цветная левая полоса
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(savingsColor)
            )
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
                    Text(
                        "Заощадження",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formatMoney(0.0),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = savingsColor
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "збережено ${formatMoney(0.0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        "в бюджеті ${formatMoney(0.0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

// ── Строка ожидаемого дохода (нижняя панель) ──────────────────────────────────

@Composable
private fun ExpectedIncomeBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Введите сумму ожидаемого дохода...",
                style     = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
            )
        }
    }
}
