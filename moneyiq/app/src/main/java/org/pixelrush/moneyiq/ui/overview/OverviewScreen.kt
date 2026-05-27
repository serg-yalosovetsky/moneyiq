package org.pixelrush.moneyiq.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.dao.CategorySpending
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.util.*
import javax.inject.Inject

// ── Month names (Ukrainian) ───────────────────────────────────────────────────

private val OVR_MONTH_NAMES = arrayOf(
    "СІЧЕНЬ", "ЛЮТИЙ", "БЕРЕЗЕНЬ", "КВІТЕНЬ", "ТРАВЕНЬ", "ЧЕРВЕНЬ",
    "ЛИПЕНЬ", "СЕРПЕНЬ", "ВЕРЕСЕНЬ", "ЖОВТЕНЬ", "ЛИСТОПАД", "ГРУДЕНЬ"
)
private val OVR_MONTH_SHORT = arrayOf(
    "січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру"
)
private val OVR_MONTH_FULL = arrayOf(
    "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
    "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
)

// ── Icon mapper ───────────────────────────────────────────────────────────────

private fun iconVectorFor(iconName: String): ImageVector = when (iconName) {
    "restaurant"     -> Icons.Default.Restaurant
    "directions_car" -> Icons.Default.DirectionsCar
    "shopping_cart"  -> Icons.Default.ShoppingCart
    "home"           -> Icons.Default.Home
    "movie"          -> Icons.Default.Movie
    "local_hospital" -> Icons.Default.LocalHospital
    "checkroom"      -> Icons.Default.Checkroom
    "phone"          -> Icons.Default.Phone
    "school"         -> Icons.Default.School
    "work"           -> Icons.Default.Work
    "laptop"         -> Icons.Default.Laptop
    "trending_up"    -> Icons.AutoMirrored.Filled.TrendingUp
    "card_giftcard"  -> Icons.Default.CardGiftcard
    "directions_bus" -> Icons.Default.DirectionsBus
    else             -> Icons.Default.Category
}

// ── Data classes ──────────────────────────────────────────────────────────────

enum class OverviewMode { EXPENSE, INCOME }

data class OverviewSelMonth(val year: Int, val month: Int)

data class WeekBar(
    val startDay: Int,
    val endDay:   Int,
    val label:    String,
    val amount:   Double,
    val isFuture: Boolean
)

data class OverviewCatRow(
    val categoryId:   Long,
    val name:         String,
    val colorHex:     String,
    val icon:         String,
    val amount:       Double,
    val percent:      Float,   // 0..1, relative to max spending category
    val budgetAmount: Double
)

data class OverviewUiState(
    val selectedMonth: OverviewSelMonth = run {
        val c = Calendar.getInstance()
        OverviewSelMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    },
    val daysInMonth:   Int                  = 31,
    val mode:          OverviewMode         = OverviewMode.EXPENSE,
    val totalBalance:  Double               = 0.0,
    val monthExpense:  Double               = 0.0,
    val monthIncome:   Double               = 0.0,
    val txCount:       Int                  = 0,
    val dailyAvg:      Double               = 0.0,
    val todayAmount:   Double               = 0.0,
    val weekAmount:    Double               = 0.0,
    val weekBars:      List<WeekBar>        = emptyList(),
    val categories:    List<OverviewCatRow> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val txRepo:       TransactionRepository,
    private val accountRepo:  AccountRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _sel = MutableStateFlow(run {
        val c = Calendar.getInstance()
        OverviewSelMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    })
    private val _mode = MutableStateFlow(OverviewMode.EXPENSE)

    val state: StateFlow<OverviewUiState> =
        combine(_sel, _mode) { s, m -> s to m }
            .flatMapLatest { (sel, mode) ->
                val (from, to) = monthRange(sel)
                val txType = if (mode == OverviewMode.EXPENSE) TransactionType.EXPENSE
                             else TransactionType.INCOME

                val innerFlow = combine(
                    txRepo.getTransactionsByPeriod(from, to),
                    txRepo.getCategorySpending(txType, from, to),
                    categoryRepo.getByType(txType)
                ) { txList, catSpend, cats -> Triple(txList, catSpend, cats) }

                combine(innerFlow, accountRepo.getTotalBalance()) { (txList, catSpend, cats), bal ->
                    buildState(sel, mode, txList, catSpend, bal ?: 0.0, cats)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState())

    fun prevMonth() {
        _sel.value = _sel.value.run {
            if (month == 0) OverviewSelMonth(year - 1, 11) else OverviewSelMonth(year, month - 1)
        }
    }

    fun nextMonth() {
        _sel.value = _sel.value.run {
            if (month == 11) OverviewSelMonth(year + 1, 0) else OverviewSelMonth(year, month + 1)
        }
    }

    fun setMode(m: OverviewMode) { _mode.value = m }

    private fun monthRange(sel: OverviewSelMonth): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(sel.year, sel.month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59);      cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }

    private fun buildState(
        sel:      OverviewSelMonth,
        mode:     OverviewMode,
        txList:   List<TransactionWithDetails>,
        catSpend: List<CategorySpending>,
        balance:  Double,
        cats:     List<CategoryEntity>
    ): OverviewUiState {
        val montCal = Calendar.getInstance().also { it.set(sel.year, sel.month, 1) }
        val dim = montCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val txType = if (mode == OverviewMode.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
        val monoTx = txList.filter { it.type == txType }

        val monthExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val monthIncome  = txList.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val monthTotal   = monoTx.sumOf { it.amount }

        // Week ranges: [startDay, endDay] inclusive (4 "weeks")
        val weekRanges = listOf(1 to 10, 11 to 17, 18 to 24, 25 to dim)
        val monShort   = OVR_MONTH_SHORT[sel.month]

        val today = Calendar.getInstance()
        val isCurrentMonth = sel.year == today.get(Calendar.YEAR) &&
                             sel.month == today.get(Calendar.MONTH)
        val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else dim + 1

        fun rangeMs(startDay: Int, endDay: Int): LongRange {
            val c = Calendar.getInstance()
            c.set(sel.year, sel.month, startDay, 0, 0, 0)
            c.set(Calendar.MILLISECOND, 0)
            val fr = c.timeInMillis
            c.set(Calendar.DAY_OF_MONTH, endDay.coerceAtMost(dim))
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59);      c.set(Calendar.MILLISECOND, 999)
            return fr..c.timeInMillis
        }

        val weekBars = weekRanges.mapIndexed { idx, (start, end) ->
            val endClamped = end.coerceAtMost(dim)
            val rng    = rangeMs(start, endClamped)
            val label  = when (idx) {
                0                   -> "1 $monShort"
                weekRanges.size - 1 -> "$endClamped $monShort"
                else                -> start.toString()
            }
            WeekBar(
                startDay = start,
                endDay   = endClamped,
                label    = label,
                amount   = monoTx.filter { it.date in rng }.sumOf { it.amount },
                isFuture = start > todayDay
            )
        }

        val todayAmount = if (isCurrentMonth) {
            val rng = rangeMs(todayDay, todayDay)
            monoTx.filter { it.date in rng }.sumOf { it.amount }
        } else 0.0

        val currentWeekRange = weekRanges.find { (s, e) -> todayDay in s..e }
        val weekAmount = currentWeekRange?.let { (s, e) ->
            val rng = rangeMs(s, e.coerceAtMost(dim))
            monoTx.filter { it.date in rng }.sumOf { it.amount }
        } ?: 0.0

        val daysPassed = if (isCurrentMonth) todayDay.coerceAtLeast(1) else dim
        val dailyAvg   = if (daysPassed > 0) monthTotal / daysPassed else 0.0

        val maxAmt = catSpend.maxOfOrNull { it.total } ?: 0.0
        val catMap = cats.associate { it.id to it }
        val catRows = catSpend.map { cs ->
            OverviewCatRow(
                categoryId   = cs.categoryId,
                name         = cs.categoryName,
                colorHex     = cs.categoryColor,
                icon         = cs.categoryIcon,
                amount       = cs.total,
                percent      = if (maxAmt > 0) (cs.total / maxAmt).toFloat() else 0f,
                budgetAmount = catMap[cs.categoryId]?.budgetAmount ?: 0.0
            )
        }

        return OverviewUiState(
            selectedMonth = sel,
            daysInMonth   = dim,
            mode          = mode,
            totalBalance  = balance,
            monthExpense  = monthExpense,
            monthIncome   = monthIncome,
            txCount       = monoTx.size,
            dailyAvg      = dailyAvg,
            todayAmount   = todayAmount,
            weekAmount    = weekAmount,
            weekBars      = weekBars,
            categories    = catRows
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    padding:          PaddingValues = PaddingValues(),
    onAddTransaction: () -> Unit    = {},
    viewModel:        OverviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedCat by remember { mutableStateOf<OverviewCatRow?>(null) }

    val expenseColor = Color(0xFFD81B60)   // pink-crimson
    val incomeColor  = Color(0xFF26A69A)   // teal
    val accentColor  = if (state.mode == OverviewMode.EXPENSE) expenseColor else incomeColor

    // ── Category detail bottom sheet ──────────────────────────────────────────
    selectedCat?.let { cat ->
        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                       catch (_: Exception) { MaterialTheme.colorScheme.primary }

        ModalBottomSheet(
            onDismissRequest = { selectedCat = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = catColor,
            dragHandle = {
                Box(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        ) {
            CategoryDetailSheet(
                cat          = cat,
                catColor     = catColor,
                monthLabel   = "${OVR_MONTH_FULL[state.selectedMonth.month]} ${state.selectedMonth.year}",
                onAddExpense = { selectedCat = null; onAddTransaction() },
                onOperations = { selectedCat = null }
            )
        }
    }

    // ── Main content ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
    ) {
        OverviewTopBar(totalBalance = state.totalBalance)

        OverviewMonthNavPill(
            sel         = state.selectedMonth,
            daysInMonth = state.daysInMonth,
            onPrev      = viewModel::prevMonth,
            onNext      = viewModel::nextMonth
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp)
        ) {
            // Balance + transaction count badge
            item {
                BalanceSection(
                    balance = state.totalBalance,
                    txCount = state.txCount
                )
            }

            // Expense / Income toggle
            item {
                ExpenseIncomeToggle(
                    mode         = state.mode,
                    monthExpense = state.monthExpense,
                    monthIncome  = state.monthIncome,
                    expenseColor = expenseColor,
                    incomeColor  = incomeColor,
                    onSelect     = viewModel::setMode
                )
            }

            // Weekly spending chart
            item {
                SpendingChart(
                    bars        = state.weekBars,
                    accentColor = accentColor,
                    monthShort  = OVR_MONTH_SHORT[state.selectedMonth.month]
                )
            }

            // Stats: daily avg / today / this week
            item {
                StatsRow(
                    dailyAvg    = state.dailyAvg,
                    todayAmt    = state.todayAmount,
                    weekAmt     = state.weekAmount,
                    accentColor = accentColor
                )
            }

            // Category list
            if (state.categories.isEmpty()) {
                item { EmptyCategories() }
            } else {
                items(state.categories) { row ->
                    CategoryRow(
                        row         = row,
                        accentColor = accentColor,
                        onClick     = { selectedCat = row }
                    )
                }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun OverviewTopBar(totalBalance: Double) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person, null,
                modifier = Modifier.size(22.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Center: "Всі рахунки" + balance
        Column(
            modifier              = Modifier.weight(1f),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                "Всі рахунки",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Text(
                formatMoney(totalBalance),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Right placeholder (keeps centre column centred)
        Spacer(Modifier.size(36.dp))
    }
}

// ── Month navigation pill ─────────────────────────────────────────────────────

@Composable
private fun OverviewMonthNavPill(
    sel:         OverviewSelMonth,
    daysInMonth: Int,
    onPrev:      () -> Unit,
    onNext:      () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = "Предыдущий месяц")
        }

        Surface(
            modifier        = Modifier.weight(1f),
            shape           = RoundedCornerShape(50),
            color           = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation  = 1.dp
        ) {
            Row(
                modifier                = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment       = Alignment.CenterVertically,
                horizontalArrangement   = Arrangement.Center
            ) {
                // Day-count badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        daysInMonth.toString(),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${OVR_MONTH_NAMES[sel.month]} ${sel.year}",
                    style         = MaterialTheme.typography.titleSmall,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
            }
        }

        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = "Следующий месяц")
        }
    }
}

// ── Balance section ───────────────────────────────────────────────────────────

@Composable
private fun BalanceSection(balance: Double, txCount: Int) {
    val hasActivity = txCount > 0
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Баланс",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                formatMoney(balance),
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        // Transaction count badge
        val badgeBg = if (hasActivity) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant
        val badgeFg = if (hasActivity) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                txCount.toString(),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = badgeFg
            )
        }
    }
}

// ── Expense / Income toggle ───────────────────────────────────────────────────

@Composable
private fun ExpenseIncomeToggle(
    mode:         OverviewMode,
    monthExpense: Double,
    monthIncome:  Double,
    expenseColor: Color,
    incomeColor:  Color,
    onSelect:     (OverviewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val expSel = mode == OverviewMode.EXPENSE
        // ── Витрати (Expenses) ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    color = if (expSel) expenseColor else expenseColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                )
                .clickable { onSelect(OverviewMode.EXPENSE) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Витрати",
                    color      = if (expSel) Color.White else expenseColor,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatMoney(monthExpense),
                    color      = if (expSel) Color.White else expenseColor,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // ── Доходи (Income) ───────────────────────────────────────────────────
        val incSel = mode == OverviewMode.INCOME
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    color = if (incSel) incomeColor else incomeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                )
                .clickable { onSelect(OverviewMode.INCOME) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Доходи",
                    color      = if (incSel) Color.White else incomeColor,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatMoney(monthIncome),
                    color      = if (incSel) Color.White else incomeColor,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Weekly spending chart ─────────────────────────────────────────────────────

@Composable
private fun SpendingChart(
    bars:        List<WeekBar>,
    accentColor: Color,
    monthShort:  String
) {
    if (bars.isEmpty()) {
        Spacer(Modifier.height(200.dp))
        return
    }

    val maxAmt      = bars.maxOf { it.amount }.coerceAtLeast(1.0)
    val gridColor   = MaterialTheme.colorScheme.outlineVariant
    val hatchColor  = accentColor.copy(alpha = 0.28f)
    val labelStyle  = MaterialTheme.typography.labelSmall
    val labelColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val w        = size.width
            val h        = size.height
            val barCount = bars.size
            val secW     = w / barCount          // width of each section
            val barPad   = 8.dp.toPx()
            val dashPE   = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            val hatchStep = 10.dp.toPx()

            // Horizontal dashed grid lines at 25 / 50 / 75 %
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                drawLine(
                    color       = gridColor,
                    start       = Offset(0f, h * (1f - frac)),
                    end         = Offset(w, h * (1f - frac)),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect  = dashPE
                )
            }

            // Bars
            bars.forEachIndexed { idx, bar ->
                val secL = idx * secW
                val secR = secL + secW
                val barL = secL + barPad
                val barR = secR - barPad

                // Vertical section divider (skip the very first)
                if (idx > 0) {
                    drawLine(
                        color       = gridColor.copy(alpha = 0.5f),
                        start       = Offset(secL, 0f),
                        end         = Offset(secL, h),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                val barFrac = (bar.amount / maxAmt).toFloat().coerceIn(0f, 1f)
                val barTop  = h * (1f - barFrac)

                when {
                    bar.isFuture -> {
                        // Diagonal hatching for the future section
                        withTransform({ clipRect(barL, 0f, barR, h) }) {
                            var sx = barL - h
                            while (sx < barR + h) {
                                drawLine(
                                    color       = hatchColor,
                                    start       = Offset(sx, h),
                                    end         = Offset(sx + h, 0f),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                sx += hatchStep
                            }
                        }
                    }
                    bar.amount > 0 -> {
                        drawRect(
                            color    = accentColor,
                            topLeft  = Offset(barL, barTop),
                            size     = Size(barR - barL, h - barTop)
                        )
                    }
                }
            }

            // Bottom border
            drawLine(
                color       = gridColor,
                start       = Offset(0f, h),
                end         = Offset(w, h),
                strokeWidth = 1.dp.toPx()
            )
        }

        // X-axis labels: start of each bar + end of the last bar
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bars.forEach { bar ->
                Text(bar.label, style = labelStyle, color = labelColor)
            }
            // End-of-month label for last bar
            Text(
                "${bars.last().endDay} $monthShort",
                style = labelStyle,
                color = labelColor
            )
        }
    }
}

// ── Stats row (daily avg / today / this week) ─────────────────────────────────

@Composable
private fun StatsRow(
    dailyAvg:    Double,
    todayAmt:    Double,
    weekAmt:     Double,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.08f))
    ) {
        OvrStatCell("День (сер.)", dailyAvg, Modifier.weight(1f))
        OvrStatCell("Сьогодні",   todayAmt, Modifier.weight(1f))
        OvrStatCell("Тиждень",    weekAmt,  Modifier.weight(1f))
    }
}

@Composable
private fun OvrStatCell(label: String, amount: Double, modifier: Modifier) {
    Column(
        modifier            = modifier.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            formatMoney(amount),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Category row ──────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    row:         OverviewCatRow,
    accentColor: Color,
    onClick:     () -> Unit
) {
    val catColor = try { Color(android.graphics.Color.parseColor(row.colorHex)) }
                   catch (_: Exception) { accentColor }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circle icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVectorFor(row.icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint     = catColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.name,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    "${(row.percent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Text(
                formatMoney(row.amount),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        // Progress bar under the row
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress     = { row.percent },
            modifier     = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp)
                .height(3.dp)
                .clip(CircleShape),
            color        = catColor,
            trackColor   = catColor.copy(alpha = 0.14f)
        )
    }
    HorizontalDivider(
        modifier  = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

// ── Category detail bottom sheet ──────────────────────────────────────────────

@Composable
private fun CategoryDetailSheet(
    cat:          OverviewCatRow,
    catColor:     Color,
    monthLabel:   String,
    onAddExpense: () -> Unit,
    onOperations: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Space to let the FAB peek above
            Spacer(Modifier.height(20.dp))

            // Category name (large, white)
            Text(
                cat.name,
                modifier   = Modifier.padding(horizontal = 20.dp),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Transaction count label + amount
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = if (cat.amount == 0.0) "Операцій немає" else "За місяць",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatMoney(cat.amount),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            // Budget progress bar
            LinearProgressIndicator(
                progress   = { cat.percent },
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color      = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            Spacer(Modifier.height(6.dp))

            // "0%" label + month
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${(cat.percent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                // budget or month label
            }

            Spacer(Modifier.height(2.dp))

            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    monthLabel,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatMoney(cat.budgetAmount),
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Bottom action row ─────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Витрата button
                Column(
                    modifier            = Modifier.clickable(onClick = onAddExpense),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63).copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward, null,
                            modifier = Modifier.size(26.dp),
                            tint     = Color(0xFFE91E63)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Витрата",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                }

                // Операції button
                Column(
                    modifier            = Modifier.clickable(onClick = onOperations),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7E57C2).copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong, null,
                            modifier = Modifier.size(26.dp),
                            tint     = Color(0xFF7E57C2)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Операції",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                }
            }
        }

        // ── FAB-style category icon at top-right ──────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = iconVectorFor(cat.icon),
                contentDescription = null,
                modifier           = Modifier.size(30.dp),
                tint               = Color.White
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyCategories() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Category, null,
                modifier = Modifier.size(64.dp),
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Немає категорій",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
