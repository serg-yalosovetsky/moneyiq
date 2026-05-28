package org.pixelrush.moneyiq.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.HomeScreenTab
import org.pixelrush.moneyiq.ui.accounts.AccountFormSheet
import org.pixelrush.moneyiq.ui.accounts.AccountsScreen
import org.pixelrush.moneyiq.ui.accounts.AccountsViewModel
import org.pixelrush.moneyiq.ui.accounts.NewAccountTypeSheet
import org.pixelrush.moneyiq.ui.budget.BudgetScreen
import org.pixelrush.moneyiq.ui.categories.CategoriesScreen
import org.pixelrush.moneyiq.ui.categories.CategoriesViewModel
import org.pixelrush.moneyiq.ui.categories.EditCategoriesScreen
import org.pixelrush.moneyiq.ui.data.DataScreen
import org.pixelrush.moneyiq.ui.overview.OverviewScreen
import org.pixelrush.moneyiq.ui.settings.SettingsScreen
import org.pixelrush.moneyiq.ui.settings.SettingsViewModel
import org.pixelrush.moneyiq.ui.theme.DebtOrange
import org.pixelrush.moneyiq.ui.theme.ExpenseRed
import org.pixelrush.moneyiq.ui.theme.IncomeGreen
import org.pixelrush.moneyiq.ui.theme.TransferBlue
import org.pixelrush.moneyiq.ui.transactions.TransactionsListScreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ── Bottom tab definition ─────────────────────────────────────────────────────

private data class BottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// Порядок вкладок — як в оригіналі 1Money
private val TABS = listOf(
    BottomTab("Рахунки",   Icons.Filled.AccountBalanceWallet,      Icons.Outlined.AccountBalanceWallet),
    BottomTab("Категорії", Icons.Filled.DonutLarge,                Icons.Outlined.DonutLarge),
    BottomTab("Операції",  Icons.AutoMirrored.Filled.ReceiptLong,  Icons.AutoMirrored.Outlined.ReceiptLong),
    BottomTab("Бюджет",    Icons.Filled.Speed,                     Icons.Outlined.Speed),
    BottomTab("Огляд",     Icons.AutoMirrored.Filled.TrendingUp,   Icons.AutoMirrored.Filled.TrendingUp),
)

// ── Main container ────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    onAddTransaction:    () -> Unit,
    mainViewModel:       MainViewModel      = hiltViewModel(),
    accountsViewModel:   AccountsViewModel  = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),
    settingsViewModel:   SettingsViewModel  = hiltViewModel()
) {
    val settings       by settingsViewModel.settings.collectAsState()
    val budgetVisible   = settings.budgetVisible
    val initialPage     = settings.homeScreen.index

    val activeTabs = if (budgetVisible) TABS else TABS.filterIndexed { i, _ -> i != 3 }
    // initialPage used only on first composition — subsequent setting changes don't force scroll
    val safeInitial = if (!budgetVisible && initialPage == HomeScreenTab.BUDGET.index)
        HomeScreenTab.CATEGORIES.index
    else initialPage.coerceIn(0, activeTabs.lastIndex)
    val pagerState  = rememberPagerState(initialPage = safeInitial,
                                        pageCount    = { activeTabs.size })
    val scope       = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val mainState  by mainViewModel.state.collectAsState()
    val totalBalance = mainState.totalBalance
    val drawerState  = rememberDrawerState(DrawerValue.Closed)

    // ── Новий рахунок ────────────────────────────────────────────────────────
    var showAccTypeSheet by remember { mutableStateOf(false) }
    var pendingAccType   by remember { mutableStateOf<AccountType?>(null) }
    val triggerNewAccount: () -> Unit = { showAccTypeSheet = true }

    // ── Редагування категорій ────────────────────────────────────────────────
    var showEditCategories by remember { mutableStateOf(false) }
    val triggerEditCategories: () -> Unit = { showEditCategories = true }
    val categoriesState by categoriesViewModel.state.collectAsState()

    // ── Екран Дані / Налаштування / Пошук / Фільтр за категорією ─────────────
    var showDataScreen       by remember { mutableStateOf(false) }
    var showSettingsScreen   by remember { mutableStateOf(false) }
    var openTxSearch         by remember { mutableStateOf(false) }
    var showBudgetSettings   by remember { mutableStateOf(false) }
    var filterByCategoryId   by remember { mutableStateOf<Long?>(null) }

    if (showDataScreen) {
        DataScreen(onNavigateBack = { showDataScreen = false })
        return
    }

    if (showSettingsScreen) {
        SettingsScreen(
            onNavigateBack = { showSettingsScreen = false },
            onData         = { showSettingsScreen = false; showDataScreen = true }
        )
        return
    }

    // Жест «назад» — повертаємося на вкладку «Операції» (індекс 2), звідти — виходимо з додатку
    val txTabIndex     = activeTabs.indexOfFirst { it.label == "Операції" }.takeIf { it >= 0 } ?: 2
    val budgetTabIndex = activeTabs.indexOfFirst { it.label == "Бюджет"   }.takeIf { it >= 0 } ?: -1
    val goBack: () -> Unit = {
        if (currentPage != txTabIndex) scope.launch { pagerState.animateScrollToPage(txTabIndex) }
    }
    BackHandler(enabled = currentPage != txTabIndex) { goBack() }

    ModalNavigationDrawer(
        drawerState     = drawerState,
        gesturesEnabled = false,
        drawerContent   = {
            AppDrawerContent(
                onClose         = { scope.launch { drawerState.close() } },
                onDataClick     = { scope.launch { drawerState.close() }; showDataScreen = true },
                onSettingsClick = { scope.launch { drawerState.close() }; showSettingsScreen = true }
            )
        }
    ) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                activeTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentPage == index,
                        onClick  = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = {
                            Icon(
                                if (currentPage == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label  = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        }
    ) { padding ->
        val bottomPadding = PaddingValues(bottom = padding.calculateBottomPadding())

        // Logical tab index → content page (accounting for hidden Budget tab)
        val pageToContent: (Int) -> Int = { page ->
            if (!budgetVisible && page >= 3) page + 1 else page
        }

        val contentPage = if (!budgetVisible && currentPage >= 3) currentPage + 1 else currentPage

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .edgeSwipe(
                    onLeftEdge  = { scope.launch { drawerState.open() } },
                    onRightEdge = goBack
                )
        ) {
            SharedTopBar(
                totalBalance      = totalBalance,
                currentPage       = contentPage,
                onAvatarClick     = { scope.launch { drawerState.open() } },
                onPlusClick       = triggerNewAccount,
                onEditCategories  = triggerEditCategories,
                onSettings        = { showSettingsScreen = true },
                onSearchTx        = { openTxSearch = true },
                onBudgetSettings  = { showBudgetSettings = true }
            )

            HorizontalPager(
                state             = pagerState,
                modifier          = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (pageToContent(page)) {
                    0 -> AccountsScreen(
                             padding      = bottomPadding,
                             embeddedMode = true,
                             onRequestAdd = triggerNewAccount
                         )
                    1 -> CategoriesScreen(
                             padding          = bottomPadding,
                             embeddedMode     = true,
                             onViewCategoryTx = { cat ->
                                 filterByCategoryId = cat.id
                                 scope.launch { pagerState.animateScrollToPage(txTabIndex) }
                             },
                             onViewBudget     = {
                                 if (budgetTabIndex >= 0)
                                     scope.launch { pagerState.animateScrollToPage(budgetTabIndex) }
                             }
                         )
                    2 -> TransactionsListScreen(
                             padding                = bottomPadding,
                             embeddedMode           = true,
                             openSearch             = openTxSearch,
                             onSearchDismissed      = { openTxSearch = false },
                             initialCategoryFilter  = filterByCategoryId,
                             onInitialFilterApplied = { filterByCategoryId = null }
                         )
                    3 -> BudgetScreen(
                             padding           = bottomPadding,
                             embeddedMode      = true,
                             showSettings      = showBudgetSettings,
                             onSettingsDismiss = { showBudgetSettings = false }
                         )
                    4 -> OverviewScreen(
                             padding          = bottomPadding,
                             onAddTransaction = onAddTransaction,
                             embeddedMode     = true
                         )
                    else -> Unit
                }
            }
        }
    }

    // ── Вибір типу рахунку ───────────────────────────────────────────────────
    if (showAccTypeSheet) {
        NewAccountTypeSheet(
            onSelect  = { type ->
                showAccTypeSheet = false
                pendingAccType   = type
            },
            onDismiss = { showAccTypeSheet = false }
        )
    }

    // ── Форма створення рахунку ──────────────────────────────────────────────
    pendingAccType?.let { type ->
        AccountFormSheet(
            initialType = type,
            existing    = null,
            onSave      = { name, accType, balance, color, currency, description, includeInTotal, icon ->
                accountsViewModel.add(name, accType, balance, color, currency, description, includeInTotal, icon)
                pendingAccType = null
            },
            onDismiss   = { pendingAccType = null }
        )
    }

    // ── Редагування категорій (з SharedTopBar — олівець на вкладці 1) ────────
    if (showEditCategories) {
        EditCategoriesScreen(
            expenseCategories = categoriesState.expenseCategories,
            incomeCategories  = categoriesState.incomeCategories,
            monthSpending     = categoriesState.monthSpending,
            monthIncome       = categoriesState.monthIncome,
            totalExpense      = categoriesState.totalExpense,
            totalIncome       = categoriesState.totalIncome,
            onSave = { name, type, color, icon, budget, period, archived, existing ->
                if (existing != null) {
                    categoriesViewModel.update(
                        existing.copy(
                            name         = name,
                            type         = type,
                            colorHex     = color,
                            icon         = icon,
                            budgetAmount = budget,
                            budgetPeriod = period,
                            archived     = archived
                        )
                    )
                } else {
                    categoriesViewModel.add(name, type, color, icon, budget, period)
                }
            },
            onDelete  = { categoriesViewModel.delete(it) },
            onDismiss = { showEditCategories = false }
        )
    }
    } // end ModalNavigationDrawer
}

// ── App drawer ────────────────────────────────────────────────────────────────

@Composable
private fun AppDrawerContent(
    onClose:         () -> Unit,
    onDataClick:     () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                modifier         = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF4361EE), Color(0xFF7B2FBE)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("1", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "1Money",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CloudOff, null,
                        modifier = Modifier.size(14.dp),
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Синхронізацію вимкнено…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardDoubleArrowRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // Premium button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFFF7043)))
                )
                .clickable {}
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Преміум-версія",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Menu items
        DrawerMenuItem(Icons.Outlined.Person,     "Увійти")
        DrawerMenuItem(Icons.Outlined.Settings,   "Налаштування", onClick = onSettingsClick)
        DrawerMenuItem(Icons.Outlined.Storage,    "Дані",         onClick = onDataClick)
        DrawerMenuItem(Icons.Outlined.StarBorder, "Оцініть нас")
        DrawerMenuItem(Icons.Outlined.Headset,    "Підтримка")
        DrawerMenuItem(Icons.Outlined.Info,       "Про застосунок")
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    ListItem(
        modifier          = Modifier.clickable(onClick = onClick),
        leadingContent    = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        },
        headlineContent   = {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    )
}

// ── Shared top bar ────────────────────────────────────────────────────────────

@Composable
fun SharedTopBar(
    totalBalance:     Double,
    currentPage:      Int,
    onAvatarClick:    () -> Unit = {},
    onPlusClick:      () -> Unit,
    onEditCategories: () -> Unit = {},
    onSettings:       () -> Unit = {},
    onSearchTx:       () -> Unit = {},
    onBudgetSettings: () -> Unit = {}
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Центр: «Всі рахунки» + баланс
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

        // Права кнопка
        val (icon, description, action) = when (currentPage) {
            0    -> Triple(Icons.Default.Add,       "Новий рахунок",           onPlusClick)
            1    -> Triple(Icons.Default.Edit,      "Редагувати категорії",    onEditCategories)
            2    -> Triple(Icons.Default.Search,    "Пошук операцій",          onSearchTx)
            3    -> Triple(Icons.Outlined.Speed,    "Налаштування бюджету",    onBudgetSettings)
            else -> Triple(Icons.Outlined.Settings, "Налаштування",            onSettings)
        }

        IconButton(
            onClick  = action,
            modifier = Modifier.size(44.dp).clip(CircleShape)
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint               = MaterialTheme.colorScheme.onSurface,
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

// ── Reusable UI blocks ────────────────────────────────────────────────────────

@Composable
fun TransactionListItem(tx: TransactionWithDetails, onClick: () -> Unit) {
    val (amountColor, amountPrefix, typeIcon) = when (tx.type) {
        TransactionType.INCOME   -> Triple(IncomeGreen,                "+", Icons.Default.ArrowDownward)
        TransactionType.EXPENSE  -> Triple(ExpenseRed,                 "−", Icons.Default.ArrowUpward)
        TransactionType.TRANSFER -> Triple(TransferBlue,               "⇄", Icons.Default.SwapHoriz)
        TransactionType.BORROW   -> Triple(DebtOrange,                 "+", Icons.Default.MoveToInbox)
        TransactionType.LEND     -> Triple(DebtOrange,                 "−", Icons.Default.Outbox)
        TransactionType.REPAY    -> Triple(Color(0xFF9C27B0),          "−", Icons.AutoMirrored.Filled.AssignmentReturn)
    }
    val fallbackColor = MaterialTheme.colorScheme.secondaryContainer
    val catColor = tx.categoryColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) }
        catch (_: Exception) { fallbackColor }
    } ?: fallbackColor

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(catColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, null, tint = amountColor, modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = {
            Text(
                text = tx.categoryName ?: tx.type.defaultLabel(),
                fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val dateStr = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(tx.date))
            val sub = buildString {
                append(dateStr)
                if (tx.note.isNotBlank()) append(" · ${tx.note}")
                append(" · ${tx.accountName}")
                tx.toAccountName?.let { append(" → $it") }
            }
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(
                "$amountPrefix${formatMoney(tx.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    )
    HorizontalDivider(
        modifier  = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

private fun TransactionType.defaultLabel() = when (this) {
    TransactionType.TRANSFER -> "Перевід"
    TransactionType.BORROW   -> "Взяти в борг"
    TransactionType.LEND     -> "Дати в борг"
    TransactionType.REPAY    -> "Повернути борг"
    TransactionType.INCOME   -> "Дохід"
    TransactionType.EXPENSE  -> "Витрата"
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title, modifier = modifier,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurface
    )
}

// ── Formatting ────────────────────────────────────────────────────────────────

fun formatMoney(amount: Double, currency: String = ""): String {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault())
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return if (currency.isNotBlank()) "${nf.format(amount)} $currency" else nf.format(amount)
}

// ── Swipe gesture helpers ─────────────────────────────────────────────────────

private const val SWIPE_THRESHOLD = 60f
private const val EDGE_DP         = 80

/** Центральний свайп — ігнорує крайкові зони (їх обробляє edgeSwipe). */
fun Modifier.horizontalSwipe(
    onSwipeLeft:  () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = pointerInput(onSwipeLeft, onSwipeRight) {
    val edgePx = EDGE_DP.dp.roundToPx().toFloat()
    awaitEachGesture {
        val down   = awaitFirstDown(requireUnconsumed = false)
        val startX = down.position.x
        // Крайкові зони — пропускаємо; вони обробляються edgeSwipe на рівні MainScreen
        if (startX < edgePx || startX > size.width - edgePx) return@awaitEachGesture
        var endX = startX
        while (true) {
            val event  = awaitPointerEvent()
            val change = event.changes.lastOrNull() ?: break
            endX = change.position.x
            if (!change.pressed) break
        }
        val delta = endX - startX
        when {
            delta < -SWIPE_THRESHOLD -> onSwipeLeft()
            delta >  SWIPE_THRESHOLD -> onSwipeRight()
        }
    }
}

/** Крайковий свайп: ліва крайка→вправо = відкрити drawer; права крайка→вліво = назад. */
fun Modifier.edgeSwipe(
    onLeftEdge:  () -> Unit = {},
    onRightEdge: () -> Unit = {},
): Modifier = pointerInput(onLeftEdge, onRightEdge) {
    val edgePx = EDGE_DP.dp.roundToPx().toFloat()
    awaitEachGesture {
        val down   = awaitFirstDown(requireUnconsumed = false)
        val startX = down.position.x
        // Центральна зона — ігноруємо; її обробляє horizontalSwipe всередині екрану
        if (startX in edgePx..(size.width - edgePx)) return@awaitEachGesture
        var endX = startX
        while (true) {
            val event  = awaitPointerEvent()
            val change = event.changes.lastOrNull() ?: break
            endX = change.position.x
            if (!change.pressed) break
        }
        val delta = endX - startX
        if (kotlin.math.abs(delta) < SWIPE_THRESHOLD) return@awaitEachGesture
        when {
            startX < edgePx && delta > 0               -> onLeftEdge()
            startX > size.width - edgePx && delta < 0  -> onRightEdge()
        }
    }
}
