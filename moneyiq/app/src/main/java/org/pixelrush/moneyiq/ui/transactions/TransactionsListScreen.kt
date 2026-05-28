@file:OptIn(ExperimentalLayoutApi::class)

package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.categories.QuickExpenseSheet
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.main.SectionHeader
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.TransactionListItem
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── UiState ───────────────────────────────────────────────────────────────────

data class TxSelectedMonth(val year: Int, val month: Int)

data class TxListUiState(
    val selectedMonth: TxSelectedMonth = run {
        val cal = Calendar.getInstance()
        TxSelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    },
    val daysInMonth:       Int                      = 31,
    val transactions:      List<TransactionWithDetails> = emptyList(),
    val totalIncome:       Double                   = 0.0,
    val totalExpense:      Double                   = 0.0,
    val closingBalance:    Double                   = 0.0,
    val openingBalance:    Double                   = 0.0,
    val accounts:          List<AccountEntity>      = emptyList(),
    val expenseCategories: List<CategoryEntity>     = emptyList(),
    val incomeCategories:  List<CategoryEntity>     = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsListViewModel @Inject constructor(
    private val txRepo:       TransactionRepository,
    private val accountRepo:  AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val monthRepo:    SelectedMonthRepository
) : ViewModel() {

    val state: StateFlow<TxListUiState> = monthRepo.month.flatMapLatest { appMonth ->
        val sel = TxSelectedMonth(appMonth.year, appMonth.month)
        val (from, to) = monthRange(sel)
        combine(
            txRepo.getTransactionsByPeriod(from, to),
            accountRepo.getTotalBalance(),
            accountRepo.getAllAccounts(),
            categoryRepo.getAll()
        ) { txList, rawBalance, accounts, categories ->
            val balance  = rawBalance ?: 0.0
            val income   = txList.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }
                .sumOf { it.amount }
            val expense  = txList.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND || it.type == TransactionType.REPAY }
                .sumOf { it.amount }
            val cal = Calendar.getInstance().also { it.set(sel.year, sel.month, 1) }
            TxListUiState(
                selectedMonth   = sel,
                daysInMonth     = cal.getActualMaximum(Calendar.DAY_OF_MONTH),
                transactions    = txList,
                totalIncome     = income,
                totalExpense    = expense,
                closingBalance  = balance,
                openingBalance  = balance - income + expense,
                accounts        = accounts,
                expenseCategories = categories.filter { it.type == TransactionType.EXPENSE && !it.archived },
                incomeCategories  = categories.filter { it.type == TransactionType.INCOME && !it.archived }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxListUiState())

    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)

    fun recordTransaction(accountId: Long, category: CategoryEntity, amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            txRepo.addTransaction(
                TransactionEntity(
                    type       = category.type,
                    amount     = amount,
                    accountId  = accountId,
                    categoryId = category.id,
                    note       = note,
                    date       = date
                )
            )
        }
    }

    private fun monthRange(sel: TxSelectedMonth): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(sel.year, sel.month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    padding:           PaddingValues = PaddingValues(),
    onEditTransaction: (Long) -> Unit = {},
    embeddedMode:      Boolean        = false,
    viewModel:         TransactionsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // ── Фільтри ────────────────────────────────────────────────────────────
    var filterQuery       by remember { mutableStateOf("") }
    var filterTypes       by remember { mutableStateOf(setOf<String>()) }   // порожнє = всі
    var filterAccountIds  by remember { mutableStateOf(setOf<Long>()) }
    var filterCategoryIds by remember { mutableStateOf(setOf<Long>()) }
    var showFilterSheet   by remember { mutableStateOf(false) }

    // ── Швидке додавання ───────────────────────────────────────────────────
    var showCategoryPicker by remember { mutableStateOf(false) }
    var quickCategory      by remember { mutableStateOf<CategoryEntity?>(null) }

    // ── Клієнтська фільтрація ─────────────────────────────────────────────
    val filteredTransactions = remember(state.transactions, filterQuery, filterTypes, filterAccountIds, filterCategoryIds) {
        state.transactions.filter { tx ->
            (filterQuery.isBlank() ||
                tx.note.contains(filterQuery, ignoreCase = true) ||
                tx.categoryName?.contains(filterQuery, ignoreCase = true) == true ||
                tx.accountName.contains(filterQuery, ignoreCase = true)) &&
            (filterTypes.isEmpty() || tx.type.name in filterTypes) &&
            (filterAccountIds.isEmpty() || tx.accountId in filterAccountIds) &&
            (filterCategoryIds.isEmpty() || tx.categoryId in filterCategoryIds)
        }
    }

    val isFiltered = filterQuery.isNotBlank() || filterTypes.isNotEmpty() ||
                     filterAccountIds.isNotEmpty() || filterCategoryIds.isNotEmpty()

    val filteredIncome   = filteredTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }.sumOf { it.amount }
    val filteredExpense  = filteredTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND || it.type == TransactionType.REPAY }.sumOf { it.amount }

    // ── Витрати за категоріями (для пікера) ───────────────────────────────
    val categorySpending = remember(state.transactions) {
        state.transactions.groupBy { it.categoryId }
            .mapValues { (_, txList) -> txList.sumOf { it.amount } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
        ) {
            // Шапка
            if (!embeddedMode) {
                TxTopBar(
                    totalBalance  = state.closingBalance,
                    onSearchClick = { showFilterSheet = true }
                )
            }

            // Пілюля-навігатор місяця
            SharedMonthNavPill(
                year          = state.selectedMonth.year,
                month         = state.selectedMonth.month,
                daysInMonth   = state.daysInMonth,
                onPrev        = viewModel::prevMonth,
                onNext        = viewModel::nextMonth,
                onSelectMonth = viewModel::goToMonth
            )

            // Активні фільтр-чіпси
            if (isFiltered) {
                ActiveFilterChipsRow(
                    filterQuery       = filterQuery,
                    filterTypes       = filterTypes,
                    filterAccountIds  = filterAccountIds,
                    filterCategoryIds = filterCategoryIds,
                    accounts          = state.accounts,
                    expenseCategories = state.expenseCategories,
                    incomeCategories  = state.incomeCategories,
                    onRemoveQuery     = { filterQuery = "" },
                    onRemoveType      = { t -> filterTypes = filterTypes - t },
                    onRemoveAccount   = { id -> filterAccountIds = filterAccountIds - id },
                    onRemoveCategory  = { id -> filterCategoryIds = filterCategoryIds - id }
                )
            }

            // Баланс-картки
            BalanceCardsRow(
                openingBalance = state.openingBalance,
                closingBalance = state.closingBalance
            )

            // Контент
            if (filteredTransactions.isEmpty()) {
                EmptyMonthState(sel = state.selectedMonth, isFiltered = isFiltered)
            } else {
                TxMonthSummaryRow(income = filteredIncome, expense = filteredExpense)
                TransactionsList(
                    transactions  = filteredTransactions,
                    bottomPadding = padding.calculateBottomPadding(),
                    onEdit        = onEditTransaction
                )
            }
        }

        // ── FAB «+» ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = padding.calculateBottomPadding() + 16.dp, end = 16.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showCategoryPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add, "Нова транзакція",
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // ── Аркуш фільтрів ────────────────────────────────────────────────────
    if (showFilterSheet) {
        TxFilterSheet(
            query             = filterQuery,
            filterTypes       = filterTypes,
            filterAccountIds  = filterAccountIds,
            filterCategoryIds = filterCategoryIds,
            accounts          = state.accounts,
            expenseCategories = state.expenseCategories,
            incomeCategories  = state.incomeCategories,
            onQueryChange     = { filterQuery = it },
            onTypesChange     = { filterTypes = it },
            onAccountsChange  = { filterAccountIds = it },
            onCategoriesChange = { filterCategoryIds = it },
            onReset           = {
                filterQuery = ""
                filterTypes = emptySet()
                filterAccountIds = emptySet()
                filterCategoryIds = emptySet()
            },
            onDismiss         = { showFilterSheet = false }
        )
    }

    // ── Пікер категорій для швидкого додавання ────────────────────────────
    if (showCategoryPicker) {
        CategoryPickerSheet(
            expenseCategories = state.expenseCategories,
            incomeCategories  = state.incomeCategories,
            categorySpending  = categorySpending,
            onSelect          = { cat ->
                showCategoryPicker = false
                quickCategory = cat
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    // ── QuickExpenseSheet ─────────────────────────────────────────────────
    quickCategory?.let { cat ->
        QuickExpenseSheet(
            category  = cat,
            accounts  = state.accounts,
            onSave    = { accountId, amount, note, date ->
                viewModel.recordTransaction(accountId, cat, amount, note, date)
                quickCategory = null
            },
            onDismiss = { quickCategory = null }
        )
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun TxTopBar(totalBalance: Double, onSearchClick: () -> Unit) {
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
            Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Всі рахунки",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            Text(
                "${formatMoney(totalBalance)} ₴",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onSearchClick, modifier = Modifier.size(44.dp).clip(CircleShape)) {
            Icon(Icons.Default.Search, "Пошук", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
    }
}

// ── Рядок активних фільтрів ───────────────────────────────────────────────────

@Composable
private fun ActiveFilterChipsRow(
    filterQuery:       String,
    filterTypes:       Set<String>,
    filterAccountIds:  Set<Long>,
    filterCategoryIds: Set<Long>,
    accounts:          List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    onRemoveQuery:     () -> Unit,
    onRemoveType:      (String) -> Unit,
    onRemoveAccount:   (Long) -> Unit,
    onRemoveCategory:  (Long) -> Unit
) {
    val allCats = expenseCategories + incomeCategories

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Пошуковий запит
        if (filterQuery.isNotBlank()) {
            FilterActiveChip(
                label     = "«$filterQuery»",
                color     = Color(0xFF5C6BC0),
                onRemove  = onRemoveQuery
            )
        }

        // Типи операцій
        filterTypes.forEach { typeName ->
            val (label, color) = when (typeName) {
                "INCOME"   -> "Дохід"   to Color(0xFF26A69A)
                "EXPENSE"  -> "Витрата" to Color(0xFFD81B60)
                "TRANSFER" -> "Переказ" to Color(0xFF607D8B)
                "BORROW"   -> "Борг"    to Color(0xFFEF6C00)
                else       -> typeName  to Color(0xFF607D8B)
            }
            FilterActiveChip(
                label    = label,
                color    = color,
                onRemove = { onRemoveType(typeName) }
            )
        }

        // Рахунки
        filterAccountIds.forEach { accId ->
            val name = accounts.firstOrNull { it.id == accId }?.name ?: "Рахунок"
            FilterActiveChip(
                label    = name,
                color    = Color(0xFF3949AB),
                onRemove = { onRemoveAccount(accId) }
            )
        }

        // Категорії
        filterCategoryIds.forEach { catId ->
            val cat   = allCats.firstOrNull { it.id == catId }
            val name  = cat?.name ?: "Категорія"
            val color = cat?.colorHex?.let { hex ->
                try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF607D8B) }
            } ?: Color(0xFF607D8B)
            FilterActiveChip(label = name, color = color, onRemove = { onRemoveCategory(catId) })
        }
    }
}

@Composable
private fun FilterActiveChip(label: String, color: Color, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = color
    ) {
        Row(
            modifier          = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
            Box(
                modifier         = Modifier
                    .size(16.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

// ── Аркуш фільтрів ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxFilterSheet(
    query:              String,
    filterTypes:        Set<String>,
    filterAccountIds:   Set<Long>,
    filterCategoryIds:  Set<Long>,
    accounts:           List<AccountEntity>,
    expenseCategories:  List<CategoryEntity>,
    incomeCategories:   List<CategoryEntity>,
    onQueryChange:      (String) -> Unit,
    onTypesChange:      (Set<String>) -> Unit,
    onAccountsChange:   (Set<Long>) -> Unit,
    onCategoriesChange: (Set<Long>) -> Unit,
    onReset:            () -> Unit,
    onDismiss:          () -> Unit
) {
    val allCats = expenseCategories + incomeCategories

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Заголовок
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Фільтри", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onReset) { Text("Скинути") }
            }

            // ── Нотатки ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = onQueryChange,
                label         = { Text("Нотатки, категорія, рахунок...") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = if (query.isNotEmpty()) {{
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, null)
                    }
                }} else null,
                shape = RoundedCornerShape(12.dp)
            )

            // ── Тип операції ──────────────────────────────────────────────
            FilterSection(title = "Тип операції") {
                listOf(
                    "EXPENSE"  to "Витрати",
                    "INCOME"   to "Доходи",
                    "TRANSFER" to "Перекази",
                    "BORROW"   to "Борги"
                ).forEach { (key, label) ->
                    val selected = key in filterTypes
                    FilterChip(
                        selected  = selected,
                        onClick   = {
                            onTypesChange(if (selected) filterTypes - key else filterTypes + key)
                        },
                        label     = { Text(label) }
                    )
                }
            }

            // ── Рахунки ───────────────────────────────────────────────────
            if (accounts.isNotEmpty()) {
                FilterSection(title = "Рахунки") {
                    accounts.forEach { acc ->
                        val selected = acc.id in filterAccountIds
                        val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) }
                                       catch (_: Exception) { Color(0xFF3949AB) }
                        FilterChip(
                            selected = selected,
                            onClick  = {
                                onAccountsChange(if (selected) filterAccountIds - acc.id else filterAccountIds + acc.id)
                            },
                            label = { Text(acc.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accColor.copy(alpha = 0.15f),
                                selectedLabelColor     = accColor
                            )
                        )
                    }
                }
            }

            // ── Витрати та доходи (категорії) ─────────────────────────────
            if (allCats.isNotEmpty()) {
                FilterSection(title = "Витрати та доходи") {
                    allCats.forEach { cat ->
                        val selected  = cat.id in filterCategoryIds
                        val catColor  = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                        catch (_: Exception) { Color(0xFF607D8B) }
                        FilterChip(
                            selected = selected,
                            onClick  = {
                                onCategoriesChange(if (selected) filterCategoryIds - cat.id else filterCategoryIds + cat.id)
                            },
                            label  = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.15f),
                                selectedLabelColor     = catColor
                            )
                        )
                    }
                }
            }

            // ── Кнопка Закрити ────────────────────────────────────────────
            Button(
                onClick   = onDismiss,
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp)
            ) {
                Text("Закрити")
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(4.dp),
            content               = content
        )
    }
}

// ── Пікер категорій для "+" ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    categorySpending:  Map<Long?, Double>,
    onSelect:          (CategoryEntity) -> Unit,
    onDismiss:         () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }  // 0=Дохід, 1=Витрата, 2=Переказ
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    val tabTypes = listOf(
        TransactionType.INCOME  to "Дохід",
        TransactionType.EXPENSE to "Витрата",
        null                    to "Переказ"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.67f)) {

            // ── Таб-рядок ────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = MaterialTheme.colorScheme.primary
            ) {
                tabTypes.forEachIndexed { i, (type, label) ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    when (type) {
                                        TransactionType.INCOME  -> Icons.Default.ArrowUpward
                                        TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                                        else                    -> Icons.Default.SwapHoriz
                                    },
                                    null, modifier = Modifier.size(16.dp)
                                )
                                Text(label, fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            // ── Сітка категорій ───────────────────────────────────────────
            val currentType = tabTypes[selectedTab].first
            val categories  = when (currentType) {
                TransactionType.INCOME  -> incomeCategories
                TransactionType.EXPENSE -> expenseCategories
                else                   -> emptyList()
            }

            if (currentType == null) {
                // Переказ — заглушка
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Виберіть рахунок для переказу\n(використайте кнопку «+» у шапці)",
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        style     = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns            = GridCells.Fixed(4),
                    modifier           = Modifier.fillMaxSize(),
                    contentPadding     = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { cat ->
                        CategoryPickerCell(
                            cat      = cat,
                            amount   = categorySpending[cat.id] ?: 0.0,
                            onClick  = { onSelect(cat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerCell(
    cat:    CategoryEntity,
    amount: Double,
    onClick: () -> Unit
) {
    val catColor = remember(cat.colorHex) {
        try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            cat.name,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurface,
            modifier  = Modifier.fillMaxWidth().heightIn(min = 30.dp),
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.15f))
                .border(
                    width = if (amount > 0) 2.dp else 1.dp,
                    color = catColor.copy(alpha = if (amount > 0) 1f else 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIconFor(cat.icon), null, tint = catColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatMoney(amount)} ₴",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (amount > 0) FontWeight.Bold else FontWeight.Normal,
            color      = if (amount > 0) catColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
            maxLines   = 1
        )
    }
}

// ── Карточки балансу ─────────────────────────────────────────────────────────

@Composable
private fun BalanceCardsRow(openingBalance: Double, closingBalance: Double) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BalanceCard("Початковий баланс", openingBalance, Modifier.weight(1f))
        BalanceCard("Кінцевий баланс",  closingBalance, Modifier.weight(1f))
    }
}

@Composable
private fun BalanceCard(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatMoney(amount)} ₴",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Рядок підсумків ───────────────────────────────────────────────────────────

@Composable
private fun TxMonthSummaryRow(income: Double, expense: Double) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryChip("+${formatMoney(income)}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
        SummaryChip("−${formatMoney(expense)}", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(label: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = containerColor) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(label, color = contentColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Порожній стан ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyMonthState(sel: TxSelectedMonth, isFiltered: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong, null,
                modifier = Modifier.size(96.dp),
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text      = if (isFiltered) "Нічого не знайдено за вашими фільтрами"
                            else "Тут ви можете переглянути транзакції за\n${MONTH_NAMES_UA_FULL[sel.month]} ${sel.year}",
                style     = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
            )
        }
    }
}

// ── Список транзакцій ─────────────────────────────────────────────────────────

@Composable
private fun TransactionsList(
    transactions:  List<TransactionWithDetails>,
    bottomPadding: Dp,
    onEdit:        (Long) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding + 88.dp)) {
        val grouped = transactions.groupBy { tx ->
            SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(tx.date))
        }
        grouped.forEach { (dateLabel, txList) ->
            item {
                SectionHeader(dateLabel, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp))
            }
            items(txList) { tx ->
                TransactionListItem(tx = tx, onClick = { onEdit(tx.id) })
            }
        }
    }
}
