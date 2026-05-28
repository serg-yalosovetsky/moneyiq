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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.AppMonth
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.util.*
import org.pixelrush.moneyiq.ui.categories.CalcStateHolder
import org.pixelrush.moneyiq.ui.categories.SharedCalcKeypad
import org.pixelrush.moneyiq.ui.categories.rememberCalcState
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
    val appMonth:       AppMonth          = AppMonth(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)),
    val daysInMonth:    Int              = 31,
    val pillLabel:      String           = "",
    val pillBadge:      String           = "31",
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

    val state: StateFlow<BudgetUiState> = monthRepo.month.flatMapLatest { am ->
        val sel        = BudgetSelMonth(am.year, am.month)
        val (from, to) = monthRepo.computeRange(am)
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
            BudgetUiState(
                selectedMonth  = sel,
                appMonth       = am,
                daysInMonth    = monthRepo.daysInPeriod(am),
                pillLabel      = monthRepo.pillLabel(am),
                pillBadge      = monthRepo.pillBadge(am),
                totalBalance   = rawBalance ?: 0.0,
                expenseSection = BudgetSectionData(expCats.sumOf { it.budgetAmount }, expRows.sumOf { it.amount }, expRows),
                incomeSection  = BudgetSectionData(incCats.sumOf { it.budgetAmount }, incRows.sumOf { it.amount }, incRows)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)
    fun setPeriod(appMonth: AppMonth)    = monthRepo.setPeriod(appMonth)

    fun updateCategoryBudget(category: org.pixelrush.moneyiq.data.db.entities.CategoryEntity, newBudget: Double) {
        viewModelScope.launch { categoryRepo.update(category.copy(budgetAmount = newBudget)) }
    }

    fun clearAllBudgets() {
        viewModelScope.launch {
            val allCats = categoryRepo.getByType(TransactionType.EXPENSE).first() +
                          categoryRepo.getByType(TransactionType.INCOME).first()
            allCats.forEach { categoryRepo.update(it.copy(budgetAmount = 0.0)) }
        }
    }

}

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
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp)
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
            item { SavingsSectionCard() }
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
            item { Spacer(Modifier.height(8.dp)) }
            item {
                IncomeBudgetBar(
                    incomeSection  = state.incomeSection,
                    expenseTotal   = state.expenseSection.totalAmount,
                    onClick = {
                        val row = state.incomeSection.rows.firstOrNull { it.category.budgetAmount == 0.0 }
                            ?: state.incomeSection.rows.firstOrNull()
                        incomeBudgetRow = row
                    }
                )
            }
        }
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

// ── Маппинг иконок категорий ──────────────────────────────────────────────────

private fun budgetIconFor(iconName: String): androidx.compose.ui.graphics.vector.ImageVector = when (iconName) {
    "restaurant"      -> Icons.Default.Restaurant
    "directions_car"  -> Icons.Default.DirectionsCar
    "shopping_cart"   -> Icons.Default.ShoppingCart
    "local_grocery_store", "grocery" -> Icons.Default.ShoppingBasket
    "home"            -> Icons.Default.Home
    "movie"           -> Icons.Default.Movie
    "local_hospital"  -> Icons.Default.LocalHospital
    "checkroom"       -> Icons.Default.Checkroom
    "phone"           -> Icons.Default.Phone
    "school"          -> Icons.Default.School
    "work"            -> Icons.Default.Work
    "laptop"          -> Icons.Default.Laptop
    "card_giftcard"   -> Icons.Default.CardGiftcard
    "directions_bus"  -> Icons.Default.DirectionsBus
    "sports_esports", "leisure" -> Icons.Default.SportsEsports
    "local_cafe"      -> Icons.Default.LocalCafe
    "sports"          -> Icons.Default.FitnessCenter
    "flight"          -> Icons.Default.Flight
    "pets"            -> Icons.Default.Pets
    "music_note"      -> Icons.Default.MusicNote
    "trending_up"     -> Icons.AutoMirrored.Filled.TrendingUp
    else              -> Icons.Default.Category
}

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
                        formatMoney(remaining),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = if (remaining < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$amountLabel ${formatMoney(data.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (data.totalAmount > 0) accentColor
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Text("в бюджеті ${formatMoney(data.totalBudget)}",
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
            Row(
                modifier              = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                visibleChips.forEach { row ->
                    BudgetCatChip(row = row, accentColor = accentColor, onClick = { editingRow = row })
                }
                if (hasMoreChips) {
                    MoreLessChip(
                        expanded  = expanded,
                        remaining = chipRows.size - 3,
                        onClick   = { expanded = !expanded }
                    )
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
            .background(accentColor.copy(alpha = 0.04f))
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
                budgetIconFor(row.category.icon), null,
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
                formatMoney(remaining),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (remaining < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text("в бюджеті ${formatMoney(row.category.budgetAmount)}",
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
            .width(88.dp)
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
        Box(
            modifier = Modifier
                .size(54.dp)
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
                budgetIconFor(row.category.icon), null,
                tint     = color,
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
            .width(76.dp)
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

// ── Діалог введення бюджету категорії ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetInputSheet(
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
                    budgetIconFor(catRow.category.icon), null,
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
private fun BudgetSettingsSheet(
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

// ── Нижня панель доходу (prompt → доступно в бюджеті) ────────────────────────

@Composable
private fun IncomeBudgetBar(
    incomeSection: BudgetSectionData,
    expenseTotal:  Double,
    onClick:       () -> Unit
) {
    val hasBudget     = incomeSection.totalBudget > 0.0
    val hasCategories = incomeSection.rows.isNotEmpty()
    val overspend     = expenseTotal - incomeSection.totalBudget
    val overspendColor = Color(0xFFD81B60)

    Surface(
        modifier = Modifier
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
