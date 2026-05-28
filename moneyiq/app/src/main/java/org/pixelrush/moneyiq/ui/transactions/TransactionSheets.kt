@file:OptIn(ExperimentalLayoutApi::class)

package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.accounts.accountIconFromKey
import org.pixelrush.moneyiq.ui.accounts.accountTypeIcon
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.components.calculator.AccountPickerSheet
import org.pixelrush.moneyiq.ui.components.calculator.CalcDateSheet
import org.pixelrush.moneyiq.ui.components.calculator.FullDatePickerDialog
import org.pixelrush.moneyiq.ui.components.calculator.SharedCalcKeypad
import org.pixelrush.moneyiq.ui.components.calculator.rememberCalcState
import org.pixelrush.moneyiq.ui.components.calculator.txDateLabelPublic
import org.pixelrush.moneyiq.ui.main.formatMoney

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
internal fun TxSearchScreen(
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
                                    icon     = accountIconFromKey(acc.icon),
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
internal fun SearchSectionHeader(title: String, color: Color) {
    Text(
        title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = color
    )
}

// ── Большая карточка типа операции ───────────────────────────────────────────

@Composable
internal fun TypeFilterCard(
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
        border       = BorderStroke(
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
internal fun ColoredFilterChip(
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
        border   = BorderStroke(1.5.dp, color)
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
internal fun CategoryPickerSheet(
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
internal fun CategoryPickerCell(
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
internal fun AccountPickerRow(account: AccountEntity, onClick: () -> Unit) {
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
                        accountTypeIcon(account.type),
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
internal fun TransferQuickSheet(
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

    val calc = rememberCalcState()

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
            SharedCalcKeypad(
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
                txDateLabelPublic(selectedDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    if (showDateSheet) {
        CalcDateSheet(
            currentDate = selectedDate, repeatMode = "NEVER", reminderMode = "NEVER",
            onDateSelected  = { selectedDate = it; showDateSheet = false },
            onRepeatClick   = { showDateSheet = false },
            onReminderClick = { showDateSheet = false },
            onPickDate      = { showDateSheet = false; showFullDate = true },
            onDismiss       = { showDateSheet = false }
        )
    }
    if (showFullDate) {
        FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }
    if (showFromAccSheet) {
        AccountPickerSheet(
            accounts   = allAccounts.filter { it.id != selectedTo?.id },
            selectedId = selectedFrom.id,
            label      = "З рахунку",
            onSelect   = { acc -> selectedFrom = acc; showFromAccSheet = false },
            onDismiss  = { showFromAccSheet = false }
        )
    }
    if (showToAccSheet) {
        AccountPickerSheet(
            accounts   = allAccounts.filter { it.id != selectedFrom.id },
            selectedId = selectedTo?.id,
            label      = "На рахунок",
            onSelect   = { acc -> selectedTo = acc; showToAccSheet = false },
            onDismiss  = { showToAccSheet = false }
        )
    }
}


// ── Деталі / редагування транзакції ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDetailSheet(
    tx:          TransactionWithDetails,
    onDismiss:   () -> Unit,
    onDelete:    () -> Unit,
    onDuplicate: () -> Unit,
    onSave:      (note: String, amount: Double, date: Long) -> Unit
) {
    val calc = rememberCalcState()

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
                    txDateLabelPublic(selectedDate),
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
                SharedCalcKeypad(
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
                    txDateLabelPublic(selectedDate),
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
        CalcDateSheet(
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
        FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; isDirty = true; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }
}
