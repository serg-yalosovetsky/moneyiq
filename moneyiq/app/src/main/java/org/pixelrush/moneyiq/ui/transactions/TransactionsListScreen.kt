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
import org.pixelrush.moneyiq.data.repository.AppMonth
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.ui.accounts.accountIconFromKey
import org.pixelrush.moneyiq.ui.categories.QuickExpenseSheet
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.main.SectionHeader
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.TransactionListItem
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
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
    val appMonth:          AppMonth                 = AppMonth(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)),
    val daysInMonth:       Int                      = 31,
    val pillLabel:         String                   = "",
    val pillBadge:         String                   = "31",
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

    val state: StateFlow<TxListUiState> = monthRepo.month.flatMapLatest { am ->
        val sel        = TxSelectedMonth(am.year, am.month)
        val (from, to) = monthRepo.computeRange(am)
        combine(
            txRepo.getTransactionsByPeriod(from, to),
            accountRepo.getTotalBalance(),
            accountRepo.getAllAccounts(),
            categoryRepo.getAll()
        ) { txList, rawBalance, accounts, categories ->
            val balance = rawBalance ?: 0.0
            val income  = txList.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }.sumOf { it.amount }
            val expense = txList.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND || it.type == TransactionType.REPAY }.sumOf { it.amount }
            TxListUiState(
                selectedMonth     = sel,
                appMonth          = am,
                daysInMonth       = monthRepo.daysInPeriod(am),
                pillLabel         = monthRepo.pillLabel(am),
                pillBadge         = monthRepo.pillBadge(am),
                transactions      = txList,
                totalIncome       = income,
                totalExpense      = expense,
                closingBalance    = balance,
                openingBalance    = balance - income + expense,
                accounts          = accounts,
                expenseCategories = categories.filter { it.type == TransactionType.EXPENSE && !it.archived },
                incomeCategories  = categories.filter { it.type == TransactionType.INCOME && !it.archived }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxListUiState())

    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)
    fun setPeriod(appMonth: AppMonth)    = monthRepo.setPeriod(appMonth)

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

    fun recordTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, date: Long) {
        viewModelScope.launch {
            txRepo.addTransaction(
                TransactionEntity(
                    type        = TransactionType.TRANSFER,
                    amount      = amount,
                    accountId   = fromAccountId,
                    toAccountId = toAccountId,
                    note        = "",
                    date        = date
                )
            )
        }
    }

    fun deleteTransaction(tx: TransactionWithDetails) {
        viewModelScope.launch {
            val entity = txRepo.getById(tx.id) ?: return@launch
            txRepo.deleteTransaction(entity)
        }
    }

    fun updateTransaction(tx: TransactionWithDetails, note: String, amount: Double, date: Long) {
        viewModelScope.launch {
            val orig = txRepo.getById(tx.id) ?: return@launch
            txRepo.updateTransaction(orig, orig.copy(note = note, amount = amount, date = date))
        }
    }

    fun duplicateTransaction(tx: TransactionWithDetails) {
        viewModelScope.launch {
            txRepo.addTransaction(
                TransactionEntity(
                    type        = tx.type,
                    amount      = tx.amount,
                    accountId   = tx.accountId,
                    toAccountId = tx.toAccountId,
                    categoryId  = tx.categoryId,
                    note        = tx.note,
                    date        = System.currentTimeMillis()
                )
            )
        }
    }

}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    padding:                  PaddingValues = PaddingValues(),
    embeddedMode:             Boolean        = false,
    openSearch:               Boolean        = false,
    onSearchDismissed:        () -> Unit     = {},
    initialCategoryFilter:    Long?          = null,
    onInitialFilterApplied:   () -> Unit     = {},
    viewModel:                TransactionsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // ── Фільтри ────────────────────────────────────────────────────────────
    var filterQuery       by remember { mutableStateOf("") }
    var filterTypes       by remember { mutableStateOf(setOf<String>()) }   // порожнє = всі
    var filterAccountIds  by remember { mutableStateOf(setOf<Long>()) }
    var filterCategoryIds by remember { mutableStateOf(setOf<Long>()) }
    var showFilterSheet   by remember { mutableStateOf(false) }

    LaunchedEffect(openSearch) {
        if (openSearch) {
            showFilterSheet = true
            onSearchDismissed()
        }
    }

    LaunchedEffect(initialCategoryFilter) {
        initialCategoryFilter?.let { catId ->
            filterCategoryIds = setOf(catId)
            onInitialFilterApplied()
        }
    }

    // ── Швидке додавання ───────────────────────────────────────────────────
    var showCategoryPicker    by remember { mutableStateOf(false) }
    var quickCategory         by remember { mutableStateOf<CategoryEntity?>(null) }
    var transferFromAccount   by remember { mutableStateOf<AccountEntity?>(null) }

    // ── Деталі транзакції ──────────────────────────────────────────────────
    var selectedDetailTx by remember { mutableStateOf<TransactionWithDetails?>(null) }

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

    Box(modifier = Modifier
        .fillMaxSize()
        .horizontalSwipe(
            onSwipeLeft  = viewModel::nextMonth,
            onSwipeRight = viewModel::prevMonth
        )
    ) {
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
                appMonth       = state.appMonth,
                daysInPeriod   = state.daysInMonth,
                onPrev         = viewModel::prevMonth,
                onNext         = viewModel::nextMonth,
                onSelectPeriod = viewModel::setPeriod
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
                    onEdit        = { txId -> selectedDetailTx = filteredTransactions.firstOrNull { it.id == txId } }
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
        TxSearchScreen(
            query              = filterQuery,
            filterTypes        = filterTypes,
            filterAccountIds   = filterAccountIds,
            filterCategoryIds  = filterCategoryIds,
            accounts           = state.accounts,
            expenseCategories  = state.expenseCategories,
            incomeCategories   = state.incomeCategories,
            onQueryChange      = { filterQuery = it },
            onTypesChange      = { filterTypes = it },
            onAccountsChange   = { filterAccountIds = it },
            onCategoriesChange = { filterCategoryIds = it },
            onReset = {
                filterQuery       = ""
                filterTypes       = emptySet()
                filterAccountIds  = emptySet()
                filterCategoryIds = emptySet()
                showFilterSheet   = false
            },
            onDone = { showFilterSheet = false }
        )
    }

    // ── Пікер категорій для швидкого додавання ────────────────────────────
    if (showCategoryPicker) {
        CategoryPickerSheet(
            expenseCategories = state.expenseCategories,
            incomeCategories  = state.incomeCategories,
            accounts          = state.accounts,
            categorySpending  = categorySpending,
            onSelect          = { cat ->
                showCategoryPicker = false
                quickCategory = cat
            },
            onTransfer        = { acc ->
                showCategoryPicker = false
                transferFromAccount = acc
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

    // ── TransferQuickSheet ────────────────────────────────────────────────
    transferFromAccount?.let { fromAcc ->
        TransferQuickSheet(
            fromAccount = fromAcc,
            allAccounts = state.accounts,
            onSave      = { toAccountId, amount, date ->
                viewModel.recordTransfer(fromAcc.id, toAccountId, amount, date)
                transferFromAccount = null
            },
            onDismiss = { transferFromAccount = null }
        )
    }

    // ── Деталі / редагування транзакції ───────────────────────────────────
    selectedDetailTx?.let { tx ->
        TransactionDetailSheet(
            tx          = tx,
            onDismiss   = { selectedDetailTx = null },
            onDelete    = { viewModel.deleteTransaction(tx); selectedDetailTx = null },
            onDuplicate = { viewModel.duplicateTransaction(tx); selectedDetailTx = null },
            onSave      = { note, amount, date ->
                viewModel.updateTransaction(tx, note, amount, date)
                selectedDetailTx = null
            }
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

@Composable
private fun TxSearchScreen(
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
    onDone:             () -> Unit
) {
    val expenseColor  = Color(0xFFE91E63)
    val incomeColor   = Color(0xFF009688)
    val transferColor = Color(0xFF607D8B)

    Dialog(
        onDismissRequest = onDone,
        properties       = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── Шапка ─────────────────────────────────────────────────────
            Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Close, "Скинути та закрити",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "Пошук",
                        modifier   = Modifier.weight(1f).padding(start = 4.dp),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick        = onDone,
                        shape          = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text("Готово", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Контент ────────────────────────────────────────────────────
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Нотатки ───────────────────────────────────────────────
                item {
                    SearchSectionHeader("Нотатки", MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = query,
                        onValueChange = onQueryChange,
                        placeholder   = { Text("Нотатки...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        leadingIcon   = {
                            Icon(Icons.Default.Notes, null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        },
                        trailingIcon  = if (query.isNotEmpty()) {{
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }} else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── Тип операції ──────────────────────────────────────────
                item {
                    SearchSectionHeader("Тип операції", expenseColor)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple("EXPENSE",  "Витрата",  expenseColor),
                            Triple("INCOME",   "Дохід",    incomeColor),
                            Triple("TRANSFER", "Переказ",  transferColor)
                        ).forEach { (key, label, color) ->
                            val selected = key in filterTypes
                            TypeFilterCard(
                                label    = label,
                                color    = color,
                                selected = selected,
                                icon     = when (key) {
                                    "EXPENSE"  -> Icons.Default.ArrowUpward
                                    "INCOME"   -> Icons.Default.ArrowDownward
                                    else       -> Icons.Default.SwapHoriz
                                },
                                modifier = Modifier.weight(1f),
                                onClick  = {
                                    onTypesChange(if (selected) filterTypes - key else filterTypes + key)
                                }
                            )
                        }
                    }
                }

                // ── Рахунки ───────────────────────────────────────────────
                if (accounts.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Рахунки", MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val selected = acc.id in filterAccountIds
                                val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) }
                                               catch (_: Exception) { Color(0xFF3949AB) }
                                ColoredFilterChip(
                                    label    = acc.name,
                                    subLabel = org.pixelrush.moneyiq.ui.main.formatMoney(acc.balance),
                                    icon     = org.pixelrush.moneyiq.ui.accounts.accountIconFromKey(acc.icon),
                                    color    = accColor,
                                    selected = selected,
                                    onClick  = {
                                        onAccountsChange(if (selected) filterAccountIds - acc.id else filterAccountIds + acc.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Витрати ───────────────────────────────────────────────
                if (expenseCategories.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Витрати", expenseColor)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            expenseCategories.forEach { cat ->
                                val selected = cat.id in filterCategoryIds
                                val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                               catch (_: Exception) { Color(0xFF607D8B) }
                                ColoredFilterChip(
                                    label    = cat.name,
                                    icon     = categoryIconFor(cat.icon),
                                    color    = catColor,
                                    selected = selected,
                                    onClick  = {
                                        onCategoriesChange(if (selected) filterCategoryIds - cat.id else filterCategoryIds + cat.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Доходи ────────────────────────────────────────────────
                if (incomeCategories.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Доходи", incomeColor)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            incomeCategories.forEach { cat ->
                                val selected = cat.id in filterCategoryIds
                                val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                               catch (_: Exception) { Color(0xFF607D8B) }
                                ColoredFilterChip(
                                    label    = cat.name,
                                    icon     = categoryIconFor(cat.icon),
                                    color    = catColor,
                                    selected = selected,
                                    onClick  = {
                                        onCategoriesChange(if (selected) filterCategoryIds - cat.id else filterCategoryIds + cat.id)
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    } // Dialog
}

// ── Секционный заголовок ──────────────────────────────────────────────────────

@Composable
private fun SearchSectionHeader(title: String, color: Color) {
    Text(
        title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = color
    )
}

// ── Большая карточка типа операции ───────────────────────────────────────────

@Composable
private fun TypeFilterCard(
    label:    String,
    color:    Color,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Surface(
        onClick      = onClick,
        modifier     = modifier.height(80.dp),
        shape        = RoundedCornerShape(16.dp),
        color        = if (selected) color else MaterialTheme.colorScheme.surface,
        border       = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = color
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = if (selected) Color.White else color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (selected) Color.White else color
            )
        }
    }
}

// ── Кольоровий чіп фільтра ────────────────────────────────────────────────────

@Composable
private fun ColoredFilterChip(
    label:    String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    color:    Color,
    selected: Boolean,
    subLabel: String? = null,
    onClick:  () -> Unit
) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(50),
        color    = if (selected) color else MaterialTheme.colorScheme.surface,
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, color)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, null,
                tint     = if (selected) Color.White else color,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    label,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selected) Color.White else color
                )
                if (subLabel != null) {
                    Text(
                        subLabel,
                        style  = MaterialTheme.typography.labelSmall,
                        color  = if (selected) Color.White.copy(alpha = 0.8f)
                                 else color.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ── Пікер категорій для "+" ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    accounts:          List<AccountEntity>,
    categorySpending:  Map<Long?, Double>,
    onSelect:          (CategoryEntity) -> Unit,
    onTransfer:        (AccountEntity) -> Unit,
    onDismiss:         () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }  // 0=Дохід, 1=Витрата, 2=Переказ
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    data class TabDef(val type: TransactionType?, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tabDefs = listOf(
        TabDef(TransactionType.INCOME,  "Дохід",   Icons.Default.ArrowUpward),
        TabDef(TransactionType.EXPENSE, "Витрата", Icons.Default.ArrowDownward),
        TabDef(null,                    "Переказ", Icons.Default.SwapHoriz)
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
                tabDefs.forEachIndexed { i, tab ->
                    val active = selectedTab == i
                    val tint = if (active) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    Tab(
                        selected = active,
                        onClick  = { selectedTab = i },
                        text     = {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, tint, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tab.icon, null, tint = tint, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    tab.label,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = tint
                                )
                            }
                        }
                    )
                }
            }

            // ── Контент по вкладці ────────────────────────────────────────
            val currentType = tabDefs[selectedTab].type
            val categories  = when (currentType) {
                TransactionType.INCOME  -> incomeCategories
                TransactionType.EXPENSE -> expenseCategories
                else                   -> emptyList()
            }

            if (currentType == null) {
                // Переказ — список рахунків
                val totalBal = accounts.filter { it.includeInTotal }.sumOf { it.balance }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Рахунки",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${formatMoney(totalBal)} ₴",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (totalBal < 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    accounts.forEach { acc ->
                        AccountPickerRow(account = acc, onClick = { onTransfer(acc) })
                    }
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
                    // Кнопка додавання категорії (заглушка)
                    item {
                        Column(
                            modifier            = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(30.dp))
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add, null,
                                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
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
    val hasSpending = amount > 0
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            cat.name,
            style      = MaterialTheme.typography.labelSmall,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.fillMaxWidth().heightIn(min = 30.dp),
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (hasSpending) catColor else catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIconFor(cat.icon), null,
                tint     = if (hasSpending) Color.White else catColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatMoney(amount)} ₴",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (hasSpending) FontWeight.Bold else FontWeight.Normal,
            color      = if (hasSpending) catColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
            maxLines   = 1
        )
    }
}

@Composable
private fun AccountPickerRow(account: AccountEntity, onClick: () -> Unit) {
    val accColor = remember(account.colorHex) {
        try { Color(android.graphics.Color.parseColor(account.colorHex)) }
        catch (_: Exception) { Color(0xFF3949AB) }
    }
    val balColor = when {
        account.balance < 0 -> Color(0xFFD32F2F)
        account.balance > 0 -> MaterialTheme.colorScheme.onSurface
        else                -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(accColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        org.pixelrush.moneyiq.ui.accounts.accountTypeIcon(account.type),
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                if (account.isDefault) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint     = Color(0xFFFFB300),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomStart)
                    )
                }
            }
        },
        headlineContent = {
            Text(account.name, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                "${formatMoney(account.balance)} ₴",
                color = balColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (account.balance != 0.0) FontWeight.Bold else FontWeight.Normal
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferQuickSheet(
    fromAccount: AccountEntity,
    allAccounts: List<AccountEntity>,
    onSave:      (toAccountId: Long, amount: Double, date: Long) -> Unit,
    onDismiss:   () -> Unit
) {
    var selectedFrom by remember { mutableStateOf(fromAccount) }
    var selectedTo   by remember { mutableStateOf(allAccounts.firstOrNull { it.id != fromAccount.id }) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var note         by remember { mutableStateOf("") }
    var showDateSheet    by remember { mutableStateOf(false) }
    var showFullDate     by remember { mutableStateOf(false) }
    var showFromAccSheet by remember { mutableStateOf(false) }
    var showToAccSheet   by remember { mutableStateOf(false) }

    val calc = org.pixelrush.moneyiq.ui.categories.rememberCalcState()

    val fromColor = remember(selectedFrom.colorHex) {
        try { Color(android.graphics.Color.parseColor(selectedFrom.colorHex)) }
        catch (_: Exception) { Color(0xFF26A69A) }
    }
    val toColor = remember(selectedTo?.colorHex) {
        try { Color(android.graphics.Color.parseColor(selectedTo?.colorHex ?: "#3949AB")) }
        catch (_: Exception) { Color(0xFF3949AB) }
    }
    val transferColor = Color(0xFF5C6BC0)
    val keyBg = MaterialTheme.colorScheme.surfaceVariant
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.72f)) {

            // ── Панелі: З рахунку / На рахунок ──────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(fromColor).clickable { showFromAccSheet = true }
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)) {
                        Text("З рахунку", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(selectedFrom.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(toColor).clickable { showToAccSheet = true }
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.CreditCard, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp), horizontalAlignment = Alignment.End) {
                        Text("На рахунок", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(selectedTo?.name ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ── Назва + сума ──────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Переказ", style = MaterialTheme.typography.labelMedium, color = transferColor)
                Text(calc.displayExpr("₴"), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = transferColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // ── Нотатка ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                placeholder = { Text("Нотатки...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(6.dp))

            // ── Калькулятор ───────────────────────────────────────────────────
            org.pixelrush.moneyiq.ui.categories.SharedCalcKeypad(
                calc         = calc,
                modifier     = Modifier.weight(1f).fillMaxWidth(),
                confirmColor = transferColor,
                onConfirm    = {
                    val amt   = calc.result()
                    val toAcc = selectedTo ?: return@SharedCalcKeypad
                    if (amt > 0) onSave(toAcc.id, amt, selectedDate)
                },
                row2ExtraKey = {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp)).background(keyBg)
                            .clickable { showDateSheet = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp)) }
                }
            )

            // ── Дата ──────────────────────────────────────────────────────────
            Text(
                org.pixelrush.moneyiq.ui.categories.txDateLabelPublic(selectedDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    if (showDateSheet) {
        org.pixelrush.moneyiq.ui.categories.CalcDateSheet(
            currentDate = selectedDate, repeatMode = "NEVER", reminderMode = "NEVER",
            onDateSelected  = { selectedDate = it; showDateSheet = false },
            onRepeatClick   = { showDateSheet = false },
            onReminderClick = { showDateSheet = false },
            onPickDate      = { showDateSheet = false; showFullDate = true },
            onDismiss       = { showDateSheet = false }
        )
    }
    if (showFullDate) {
        org.pixelrush.moneyiq.ui.categories.FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }
    if (showFromAccSheet) {
        org.pixelrush.moneyiq.ui.categories.AccountPickerSheet(
            accounts   = allAccounts.filter { it.id != selectedTo?.id },
            selectedId = selectedFrom.id,
            label      = "З рахунку",
            onSelect   = { acc -> selectedFrom = acc; showFromAccSheet = false },
            onDismiss  = { showFromAccSheet = false }
        )
    }
    if (showToAccSheet) {
        org.pixelrush.moneyiq.ui.categories.AccountPickerSheet(
            accounts   = allAccounts.filter { it.id != selectedFrom.id },
            selectedId = selectedTo?.id,
            label      = "На рахунок",
            onSelect   = { acc -> selectedTo = acc; showToAccSheet = false },
            onDismiss  = { showToAccSheet = false }
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

// ── Деталі / редагування транзакції ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    tx:          TransactionWithDetails,
    onDismiss:   () -> Unit,
    onDelete:    () -> Unit,
    onDuplicate: () -> Unit,
    onSave:      (note: String, amount: Double, date: Long) -> Unit
) {
    val calc = org.pixelrush.moneyiq.ui.categories.rememberCalcState()

    var note         by remember(tx.id) { mutableStateOf(tx.note) }
    var selectedDate by remember(tx.id) { mutableStateOf(tx.date) }
    var isDirty      by remember(tx.id) { mutableStateOf(false) }
    var showCalc     by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDateSheet    by remember { mutableStateOf(false) }
    var showFullDate     by remember { mutableStateOf(false) }

    LaunchedEffect(tx.id) {
        val v = tx.amount
        calc.currentStr = when {
            v == v.toLong().toDouble() -> v.toLong().toString()
            else -> v.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
        }
    }

    val accountColor = remember(tx.accountColor) {
        try { Color(android.graphics.Color.parseColor(tx.accountColor)) }
        catch (_: Exception) { Color(0xFF3949AB) }
    }
    val catColor = remember(tx.categoryColor) {
        tx.categoryColor?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
        }
    }
    val isTransfer  = tx.type == TransactionType.TRANSFER
    val leftColor   = if (isTransfer) Color(0xFF009688) else accountColor
    val rightColor  = when {
        isTransfer       -> Color(0xFF3949AB)
        catColor != null -> catColor
        else             -> Color(0xFF757575)
    }
    val accentColor = when (tx.type) {
        TransactionType.TRANSFER -> Color(0xFF009688)
        TransactionType.INCOME   -> Color(0xFF43A047)
        else                     -> Color(0xFFE53935)
    }
    val keyBg   = MaterialTheme.colorScheme.surfaceVariant
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = {
            if (isDirty) onSave(note, tx.amount, selectedDate)
            else onDismiss()
        },
        sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.75f)) {

            // ── Двопанельний заголовок ────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(leftColor)) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)) {
                        Text("З рахунку", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(tx.accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(rightColor)) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .size(32.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isTransfer           -> Icons.Outlined.CreditCard
                                tx.categoryIcon != null -> categoryIconFor(tx.categoryIcon)
                                else                 -> Icons.Outlined.Category
                            },
                            null, tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            if (isTransfer) "На рахунок" else "Категорія",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            when {
                                isTransfer              -> tx.toAccountName ?: "—"
                                tx.categoryName != null -> tx.categoryName
                                else                    -> "Без категорії"
                            },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Сума (тап — перемикає калькулятор) ────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().clickable { showCalc = !showCalc }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when (tx.type) {
                        TransactionType.TRANSFER -> "Переказ"
                        TransactionType.INCOME   -> "Дохід"
                        else                     -> "Витрата"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
                Text(
                    if (showCalc) calc.displayExpr("₴") else "${formatMoney(tx.amount)} ₴",
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            if (!showCalc) {
                // ── Нотатки ──────────────────────────────────────────────────────
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it; isDirty = true },
                    placeholder   = { Text("Нотатки...") },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(6.dp))

                // ── Дата ─────────────────────────────────────────────────────────
                Text(
                    org.pixelrush.moneyiq.ui.categories.txDateLabelPublic(selectedDate),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                )

                Spacer(Modifier.weight(1f))

                HorizontalDivider()

                // ── Кнопки дій ───────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Видалити")
                    }
                    TextButton(onClick = { showDateSheet = true }) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Дата")
                    }
                    TextButton(onClick = onDuplicate) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Дублювати")
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                // ── Калькулятор ───────────────────────────────────────────────────
                org.pixelrush.moneyiq.ui.categories.SharedCalcKeypad(
                    calc         = calc,
                    modifier     = Modifier.weight(1f).fillMaxWidth(),
                    confirmColor = accentColor,
                    onConfirm    = {
                        val amt = calc.result()
                        if (amt > 0) onSave(note, amt, selectedDate)
                    },
                    row2ExtraKey = {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp)).background(keyBg)
                                .clickable { showDateSheet = true },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp)) }
                    }
                )
                Text(
                    org.pixelrush.moneyiq.ui.categories.txDateLabelPublic(selectedDate),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Видалити транзакцію?") },
            text  = { Text("Транзакцію буде видалено, а баланс рахунку скориговано. Цю дію не можна скасувати.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") } }
        )
    }

    if (showDateSheet) {
        org.pixelrush.moneyiq.ui.categories.CalcDateSheet(
            currentDate     = selectedDate,
            repeatMode      = "NEVER",
            reminderMode    = "NEVER",
            onDateSelected  = { selectedDate = it; isDirty = true; showDateSheet = false },
            onRepeatClick   = { showDateSheet = false },
            onReminderClick = { showDateSheet = false },
            onPickDate      = { showDateSheet = false; showFullDate = true },
            onDismiss       = { showDateSheet = false }
        )
    }
    if (showFullDate) {
        org.pixelrush.moneyiq.ui.categories.FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; isDirty = true; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }
}
