package org.pixelrush.moneyiq.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.accounts.AccountsScreen
import org.pixelrush.moneyiq.ui.budget.BudgetScreen
import org.pixelrush.moneyiq.ui.categories.CategoriesScreen
import org.pixelrush.moneyiq.ui.reports.ReportsScreen
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

// Порядок вкладок — как в оригинале 1Money
private val TABS = listOf(
    BottomTab("Счета",     Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BottomTab("Категории", Icons.Filled.Category,             Icons.Outlined.Category),
    BottomTab("Операции",  Icons.Filled.ReceiptLong,          Icons.Outlined.ReceiptLong),
    BottomTab("Бюджет",    Icons.Filled.PieChart,             Icons.Outlined.PieChart),
    BottomTab("Обзор",     Icons.Filled.Home,                 Icons.Outlined.Home),
)

// ── Main container ────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        floatingActionButton = {
            // FAB только на вкладке "Операции" (tab 2) и "Обзор" (tab 4)
            if (selectedTab == 2 || selectedTab == 4) {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить транзакцию",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                // 0 → Счета (как в оригинале — первая вкладка)
                0 -> AccountsScreen(padding = padding, embeddedMode = true)
                // 1 → Категории
                1 -> CategoriesScreen(
                        onNavigateBack = {},
                        embeddedMode = true,
                        padding = padding
                     )
                // 2 → Операции (транзакции)
                2 -> TransactionsListScreen(padding = padding, onEditTransaction = onEditTransaction)
                // 3 → Бюджет (прогресс по категориям)
                3 -> BudgetScreen(padding = padding)
                // 4 → Обзор (дашборд + последние операции)
                4 -> OverviewTab(state = state, padding = padding, onEditTransaction = onEditTransaction)
            }
        }
    }
}

// ── Tab 0: Overview ───────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    state: MainUiState,
    padding: PaddingValues,
    onEditTransaction: (Long) -> Unit = {}
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item { BalanceCard(total = state.totalBalance, income = state.monthIncome, expense = state.monthExpense) }

        if (state.accounts.isNotEmpty()) {
            item {
                SectionHeader(
                    "Счета",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.accounts) { AccountChipCard(it) }
                }
            }
        }

        if (state.recentTransactions.isNotEmpty()) {
            item {
                SectionHeader(
                    "Последние операции",
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                )
            }
            items(state.recentTransactions) { tx ->
                TransactionListItem(tx = tx, onClick = { onEditTransaction(tx.id) })
            }
        } else {
            item { EmptyTransactionsHint() }
        }
    }
}

// ── Reusable UI blocks ────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(total: Double, income: Double, expense: Double) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryLight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f).compositeOver(primary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(primary, primaryLight)),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    "Общий баланс",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatMoney(total),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth()) {
                    MiniStat(
                        label = "Доходы",
                        amount = income,
                        color = Color(0xFFA5D6A7),
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        label = "Расходы",
                        amount = expense,
                        color = Color(0xFFEF9A9A),
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(
    label: String, amount: Double, color: Color,
    icon: ImageVector, modifier: Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, color = color.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            Text(
                formatMoney(amount), color = color,
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun AccountChipCard(account: AccountEntity) {
    val bgColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) }
    catch (_: Exception) { MaterialTheme.colorScheme.primaryContainer }

    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, bgColor.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(bgColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalanceWallet, null,
                    modifier = Modifier.size(18.dp), tint = bgColor)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                account.name, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                formatMoney(account.balance),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = bgColor, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionListItem(tx: TransactionWithDetails, onClick: () -> Unit) {
    val (amountColor, amountPrefix, typeIcon) = when (tx.type) {
        TransactionType.INCOME   -> Triple(IncomeGreen,                "+", Icons.Default.ArrowDownward)
        TransactionType.EXPENSE  -> Triple(ExpenseRed,                 "−", Icons.Default.ArrowUpward)
        TransactionType.TRANSFER -> Triple(TransferBlue,               "⇄", Icons.Default.SwapHoriz)
        TransactionType.BORROW   -> Triple(DebtOrange,                 "+", Icons.Default.MoveToInbox)
        TransactionType.LEND     -> Triple(DebtOrange,                 "−", Icons.Default.Outbox)
        TransactionType.REPAY    -> Triple(Color(0xFF9C27B0),          "−", Icons.Default.AssignmentReturn)
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
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private fun TransactionType.defaultLabel() = when (this) {
    TransactionType.TRANSFER -> "Перевод"
    TransactionType.BORROW   -> "Взять в долг"
    TransactionType.LEND     -> "Дать в долг"
    TransactionType.REPAY    -> "Вернуть долг"
    TransactionType.INCOME   -> "Доход"
    TransactionType.EXPENSE  -> "Расход"
}

@Composable
private fun EmptyTransactionsHint() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ReceiptLong, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f))
            Spacer(Modifier.height(8.dp))
            Text("Нет транзакций",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text("Нажмите + чтобы добавить",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title, modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ── Formatting ────────────────────────────────────────────────────────────────

fun formatMoney(amount: Double, currency: String = ""): String {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault())
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return if (currency.isNotBlank()) "${nf.format(amount)} $currency" else nf.format(amount)
}

private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha
    return Color(
        red   = this.red   * a + background.red   * (1f - a),
        green = this.green * a + background.green * (1f - a),
        blue  = this.blue  * a + background.blue  * (1f - a),
        alpha = 1f
    )
}
