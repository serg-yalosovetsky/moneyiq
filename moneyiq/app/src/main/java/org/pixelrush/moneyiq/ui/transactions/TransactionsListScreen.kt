
package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.ui.categories.QuickExpenseSheet
import org.pixelrush.moneyiq.ui.main.SectionHeader
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.TransactionListItem
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
import java.text.SimpleDateFormat
import java.util.*


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

