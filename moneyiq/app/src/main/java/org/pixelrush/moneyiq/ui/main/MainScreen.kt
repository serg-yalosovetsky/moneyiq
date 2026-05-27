package org.pixelrush.moneyiq.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.accounts.AccountsScreen
import org.pixelrush.moneyiq.ui.budget.BudgetScreen
import org.pixelrush.moneyiq.ui.categories.CategoriesScreen
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

// Порядок вкладок — как в оригинале 1Money
private val TABS = listOf(
    BottomTab("Счета",     Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BottomTab("Категории", Icons.Filled.Category,             Icons.Outlined.Category),
    BottomTab("Операции",  Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    BottomTab("Бюджет",    Icons.Filled.PieChart,             Icons.Outlined.PieChart),
    BottomTab("Обзор",     Icons.Filled.Home,                 Icons.Outlined.Home),
)

// ── Main container ────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        floatingActionButton = {
            // FAB только на вкладке "Операции" (tab 2)
            if (selectedTab == 2) {
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
                // 4 → Обзор (дашборд по категориям)
                4 -> OverviewScreen(
                        padding          = padding,
                        onAddTransaction = onAddTransaction
                     )
            }
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

