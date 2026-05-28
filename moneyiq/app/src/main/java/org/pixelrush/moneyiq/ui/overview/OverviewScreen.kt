package org.pixelrush.moneyiq.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.dao.CategorySpending
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.AppMonth
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
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

data class DayBar(
    val day:      Int,
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

/** Сырые данные из Room — все потоки за один flatMapLatest */
private data class OvrRaw(
    val txList:   List<TransactionWithDetails>,
    val expSpend: List<CategorySpending>,
    val incSpend: List<CategorySpending>,
    val expCats:  List<CategoryEntity>,
    val incCats:  List<CategoryEntity>
)

data class OverviewUiState(
    val selectedMonth: OverviewSelMonth = run {
        val c = Calendar.getInstance()
        OverviewSelMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    },
    val appMonth:           AppMonth             = AppMonth(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)),
    val daysInMonth:        Int                  = 31,
    val mode:               OverviewMode         = OverviewMode.EXPENSE,
    val totalBalance:       Double               = 0.0,
    val monthExpense:       Double               = 0.0,
    val monthIncome:        Double               = 0.0,
    val txCount:            Int                  = 0,
    val dailyAvg:           Double               = 0.0,
    val todayAmount:        Double               = 0.0,
    val weekAmount:         Double               = 0.0,
    val dayBars:            List<DayBar>         = emptyList(),
    val categories:         List<OverviewCatRow> = emptyList(),
    /** Суммы для секции «Бюджет» внизу экрана */
    val totalExpenseBudget: Double               = 0.0,
    val totalIncomeBudget:  Double               = 0.0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val txRepo:       TransactionRepository,
    private val accountRepo:  AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val monthRepo:    SelectedMonthRepository        // ← общий репозиторий
) : ViewModel() {

    private val _mode = MutableStateFlow(OverviewMode.EXPENSE)

    val state: StateFlow<OverviewUiState> =
        combine(monthRepo.month, _mode) { am, mode ->
            am to mode
        }
        .flatMapLatest { (am, mode) ->
            val sel        = OverviewSelMonth(am.year, am.month)
            val (from, to) = monthRepo.computeRange(am)

            val innerFlow = combine(
                txRepo.getTransactionsByPeriod(from, to),
                txRepo.getCategorySpending(TransactionType.EXPENSE, from, to),
                txRepo.getCategorySpending(TransactionType.INCOME,  from, to),
                categoryRepo.getByType(TransactionType.EXPENSE),
                categoryRepo.getByType(TransactionType.INCOME)
            ) { txList, expSpend, incSpend, expCats, incCats ->
                OvrRaw(txList, expSpend, incSpend, expCats, incCats)
            }

            combine(innerFlow, accountRepo.getTotalBalance()) { data, bal ->
                buildState(am, sel, mode, data, bal ?: 0.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState())

    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)
    fun setPeriod(appMonth: AppMonth)    = monthRepo.setPeriod(appMonth)
    fun setMode(m: OverviewMode)         { _mode.value = m }

    private fun buildState(
        am:      AppMonth,
        sel:     OverviewSelMonth,
        mode:    OverviewMode,
        data:    OvrRaw,
        balance: Double
    ): OverviewUiState {
        val catSpend = if (mode == OverviewMode.EXPENSE) data.expSpend else data.incSpend
        val cats     = if (mode == OverviewMode.EXPENSE) data.expCats  else data.incCats
        val txList   = data.txList
        val montCal  = Calendar.getInstance().also { it.set(sel.year, sel.month, 1) }
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

        val isFutureMonth = sel.year > today.get(Calendar.YEAR) ||
            (sel.year == today.get(Calendar.YEAR) && sel.month > today.get(Calendar.MONTH))

        val dayBars = (1..dim).map { day ->
            val rng = rangeMs(day, day)
            DayBar(
                day      = day,
                amount   = monoTx.filter { it.date in rng }.sumOf { it.amount },
                isFuture = isFutureMonth || day > todayDay
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

        val activeSpend = catSpend.filter { it.total > 0 }
        val maxAmt = activeSpend.maxOfOrNull { it.total } ?: 0.0
        val catMap = cats.associate { it.id to it }
        val catRows = activeSpend.map { cs ->
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

        val totalExpenseBudget = data.expCats.sumOf { it.budgetAmount }
        val totalIncomeBudget  = data.incCats.sumOf { it.budgetAmount }

        return OverviewUiState(
            selectedMonth      = sel,
            appMonth           = am,
            daysInMonth        = monthRepo.daysInPeriod(am),
            mode               = mode,
            totalBalance       = balance,
            monthExpense       = monthExpense,
            monthIncome        = monthIncome,
            txCount            = monoTx.size,
            dailyAvg           = dailyAvg,
            todayAmount        = todayAmount,
            weekAmount         = weekAmount,
            dayBars            = dayBars,
            categories         = catRows,
            totalExpenseBudget = totalExpenseBudget,
            totalIncomeBudget  = totalIncomeBudget
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    padding:          PaddingValues = PaddingValues(),
    onAddTransaction: () -> Unit    = {},
    embeddedMode:     Boolean       = false,
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
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
            .horizontalSwipe(
                onSwipeLeft  = viewModel::nextMonth,
                onSwipeRight = viewModel::prevMonth
            )
    ) {
        if (!embeddedMode) {
            OverviewTopBar(totalBalance = state.totalBalance)
        }

        SharedMonthNavPill(
            appMonth       = state.appMonth,
            daysInPeriod   = state.daysInMonth,
            onPrev         = viewModel::prevMonth,
            onNext         = viewModel::nextMonth,
            onSelectPeriod = viewModel::setPeriod
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

            // Daily spending chart
            item {
                SpendingChart(
                    dayBars     = state.dayBars,
                    daysInMonth = state.daysInMonth,
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

            // ── Секція «Бюджет» внизу ────────────────────────────────────────
            item {
                BudgetSummarySection(
                    monthExpense       = state.monthExpense,
                    monthIncome        = state.monthIncome,
                    totalExpenseBudget = state.totalExpenseBudget,
                    totalIncomeBudget  = state.totalIncomeBudget
                )
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
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // « двойная стрелка влево
        IconButton(onClick = onPrev) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
            }
        }

        // Пилюля [дни] МІСЯЦЬ РІК ▾
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
                    "${OVR_MONTH_NAMES[sel.month]} ${sel.year}",
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

        // » двойная стрелка вправо
        IconButton(onClick = onNext) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
            }
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

// ── Daily spending chart ──────────────────────────────────────────────────────

@Composable
private fun SpendingChart(
    dayBars:     List<DayBar>,
    daysInMonth: Int,
    accentColor: Color,
    monthShort:  String
) {
    if (dayBars.isEmpty()) {
        Spacer(Modifier.height(180.dp))
        return
    }

    val maxAmt     = dayBars.maxOf { it.amount }.coerceAtLeast(1.0)
    val gridColor  = MaterialTheme.colorScheme.outlineVariant
    val hatchColor = accentColor.copy(alpha = 0.25f)
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    // Y-axis label values
    val yMax = maxAmt
    val yMid = maxAmt / 2.0

    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
        // Chart canvas + X-axis labels
        Column(modifier = Modifier.fillMaxWidth().padding(end = 36.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val w         = size.width
                val h         = size.height
                val count     = dayBars.size.coerceAtLeast(1)
                val slotW     = w / count
                val gap       = (slotW * 0.15f).coerceIn(0.5f, 3.dp.toPx())
                val barW      = (slotW - gap).coerceAtLeast(1f)
                val dashPE    = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                val hatchStep = 9.dp.toPx()

                // Dashed grid lines at 50% and 100%
                listOf(0.5f, 1.0f).forEach { frac ->
                    drawLine(
                        color       = gridColor,
                        start       = Offset(0f, h * (1f - frac)),
                        end         = Offset(w, h * (1f - frac)),
                        strokeWidth = 0.8.dp.toPx(),
                        pathEffect  = if (frac < 1f) dashPE else null
                    )
                }

                // Bars
                dayBars.forEachIndexed { idx, bar ->
                    val slotL = idx * slotW
                    val barL  = slotL + gap / 2f
                    val barR  = barL + barW

                    val barFrac = (bar.amount / maxAmt).toFloat().coerceIn(0f, 1f)
                    val barTop  = h * (1f - barFrac)

                    when {
                        bar.isFuture -> {
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
                                color   = accentColor,
                                topLeft = Offset(barL, barTop),
                                size    = Size(barW, h - barTop)
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

            // X-axis milestone labels: 1, 11, 18, last day
            val milestones = listOf(1, 11, 18, daysInMonth)
            Row(modifier = Modifier.fillMaxWidth()) {
                milestones.forEachIndexed { i, day ->
                    val weight = when (i) {
                        0 -> (11 - 1).toFloat()
                        1 -> (18 - 11).toFloat()
                        2 -> (daysInMonth - 18).toFloat().coerceAtLeast(1f)
                        else -> 0f
                    }
                    if (weight > 0f) {
                        Box(modifier = Modifier.weight(weight)) {
                            Text(
                                text  = if (day == daysInMonth) "$day $monthShort" else "$day",
                                style = labelStyle,
                                color = labelColor
                            )
                        }
                    } else {
                        Text(
                            text  = "$day $monthShort",
                            style = labelStyle,
                            color = labelColor
                        )
                    }
                }
            }
        }

        // Y-axis labels pinned to right edge
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(34.dp)
                .height(150.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text     = formatMoney(yMax).let { if (it.length > 6) it.take(6) else it },
                style    = labelStyle,
                color    = labelColor,
                modifier = Modifier.align(Alignment.End)
            )
            Text(
                text     = formatMoney(yMid).let { if (it.length > 6) it.take(6) else it },
                style    = labelStyle,
                color    = labelColor,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(Modifier.height(0.dp))
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

// ── Секція «Бюджет» внизу Огляду ─────────────────────────────────────────────

@Composable
private fun BudgetSummarySection(
    monthExpense:       Double,
    monthIncome:        Double,
    totalExpenseBudget: Double,
    totalIncomeBudget:  Double
) {
    val expenseColor = Color(0xFFD81B60)
    val savingsColor = Color(0xFF26A69A)
    val incomeColor  = Color(0xFF66BB6A)

    Spacer(Modifier.height(8.dp))
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Заголовок
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Бюджет",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Speed, null,
                    modifier = Modifier.size(20.dp),
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            HorizontalDivider()

            // Витрати
            BudgetSummaryRow(
                label       = "Витрати",
                amount      = monthExpense,
                budget      = totalExpenseBudget,
                amountLabel = "витрачено",
                budgetLabel = "в бюджеті",
                borderColor = expenseColor
            )
            HorizontalDivider(modifier = Modifier.padding(start = 18.dp))

            // Заощадження
            val savings = (totalIncomeBudget - monthExpense).coerceAtLeast(0.0)
            BudgetSummaryRow(
                label       = "Заощадження",
                amount      = savings,
                budget      = totalIncomeBudget,
                amountLabel = "збережено",
                budgetLabel = "в бюджеті",
                borderColor = savingsColor
            )
            HorizontalDivider(modifier = Modifier.padding(start = 18.dp))

            // Доходи
            BudgetSummaryRow(
                label       = "Доходи",
                amount      = monthIncome,
                budget      = totalIncomeBudget,
                amountLabel = "отримано",
                budgetLabel = "в бюджеті",
                borderColor = incomeColor
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BudgetSummaryRow(
    label:       String,
    amount:      Double,
    budget:      Double,
    amountLabel: String,
    budgetLabel: String,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(borderColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = borderColor
                )
                Text(
                    formatMoney(amount),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$amountLabel ${formatMoney(amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Text(
                    "$budgetLabel ${formatMoney(budget)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            if (budget > 0) {
                Spacer(Modifier.height(4.dp))
                val progress = (amount / budget).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color      = borderColor,
                    trackColor = borderColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}
