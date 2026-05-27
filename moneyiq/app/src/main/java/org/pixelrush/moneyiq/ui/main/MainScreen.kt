package org.pixelrush.moneyiq.ui.main

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.accounts.AccountFormSheet
import org.pixelrush.moneyiq.ui.accounts.AccountsScreen
import org.pixelrush.moneyiq.ui.accounts.AccountsViewModel
import org.pixelrush.moneyiq.ui.accounts.NewAccountTypeSheet
import org.pixelrush.moneyiq.ui.budget.BudgetScreen
import org.pixelrush.moneyiq.ui.categories.CategoriesScreen
import org.pixelrush.moneyiq.ui.categories.CategoriesViewModel
import org.pixelrush.moneyiq.ui.categories.EditCategoriesScreen
import org.pixelrush.moneyiq.ui.overview.OverviewScreen
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
    onAddTransaction:   () -> Unit,
    onEditTransaction:  (Long) -> Unit   = {},
    mainViewModel:      MainViewModel    = hiltViewModel(),
    accountsViewModel:  AccountsViewModel    = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel()
) {
    val pagerState  = rememberPagerState(pageCount = { TABS.size })
    val scope       = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val mainState  by mainViewModel.state.collectAsState()
    val totalBalance = mainState.totalBalance

    // ── Новий рахунок ────────────────────────────────────────────────────────
    var showAccTypeSheet by remember { mutableStateOf(false) }
    var pendingAccType   by remember { mutableStateOf<AccountType?>(null) }
    val triggerNewAccount: () -> Unit = { showAccTypeSheet = true }

    // ── Редагування категорій ────────────────────────────────────────────────
    var showEditCategories by remember { mutableStateOf(false) }
    val triggerEditCategories: () -> Unit = { showEditCategories = true }
    val categoriesState by categoriesViewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (currentPage == 2) {
                FloatingActionButton(
                    onClick        = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Додати транзакцію",
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                TABS.forEachIndexed { index, tab ->
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

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SharedTopBar(
                totalBalance      = totalBalance,
                currentPage       = currentPage,
                onPlusClick       = triggerNewAccount,
                onEditCategories  = triggerEditCategories
            )

            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> AccountsScreen(
                             padding      = bottomPadding,
                             embeddedMode = true,
                             onRequestAdd = triggerNewAccount
                         )
                    1 -> CategoriesScreen(
                             padding      = bottomPadding,
                             embeddedMode = true
                         )
                    2 -> TransactionsListScreen(
                             padding           = bottomPadding,
                             onEditTransaction = onEditTransaction,
                             embeddedMode      = true
                         )
                    3 -> BudgetScreen(
                             padding      = bottomPadding,
                             embeddedMode = true
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
            onSave      = { name, accType, balance, color, currency, description, includeInTotal ->
                accountsViewModel.add(name, accType, balance, color, currency, description, includeInTotal)
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
}

// ── Shared top bar ────────────────────────────────────────────────────────────

@Composable
fun SharedTopBar(
    totalBalance:     Double,
    currentPage:      Int,
    onPlusClick:      () -> Unit,
    onEditCategories: () -> Unit = {}
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

        // Права кнопка: «+» (рахунки), олівець (категорії), порожньо (інші)
        val (icon, description, action) = when (currentPage) {
            0    -> Triple(Icons.Default.Add,  "Новий рахунок",          onPlusClick)
            1    -> Triple(Icons.Default.Edit, "Редагувати категорії",   onEditCategories)
            else -> Triple(Icons.Outlined.Settings, "Налаштування",      {})
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
