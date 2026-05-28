package org.pixelrush.moneyiq.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.ui.main.formatMoney

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun AccountsScreen(
    padding:        PaddingValues = PaddingValues(),
    onNavigateBack: () -> Unit    = {},
    embeddedMode:   Boolean       = false,
    onRequestAdd:   () -> Unit    = {},          // called by "Додати рахунок" & top "+"
    viewModel:      AccountsViewModel = hiltViewModel()
) {
    val state          by viewModel.state.collectAsState()
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var editAccount    by remember { mutableStateOf<AccountEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
    ) {
        if (!embeddedMode) {
            AccountsTopBar(
                totalBalance = state.totalBalance,
                onAddClick   = onRequestAdd
            )
        }

        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor   = MaterialTheme.colorScheme.surface,
            contentColor     = MaterialTheme.colorScheme.primary,
            divider          = {
                HorizontalDivider(
                    thickness = 1.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick  = { selectedSubTab = 0 },
                icon     = { Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(20.dp)) },
                text     = {
                    Text(
                        "Рахунки",
                        fontWeight = if (selectedSubTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        style      = MaterialTheme.typography.bodyMedium
                    )
                }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick  = { selectedSubTab = 1 },
                icon     = { Icon(Icons.Outlined.BarChart, null, modifier = Modifier.size(20.dp)) },
                text     = {
                    Text(
                        "Мої фінанси",
                        fontWeight = if (selectedSubTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        style      = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }

        when (selectedSubTab) {
            0 -> AccountsListTab(
                    state         = state,
                    bottomPadding = padding.calculateBottomPadding(),
                    onAdd         = onRequestAdd,
                    onEdit        = { editAccount = it },
                    onDelete      = { viewModel.delete(it) },
                    onSetDefault  = { viewModel.setDefault(it) }
                 )
            1 -> MyFinancesTab(
                    state         = state,
                    bottomPadding = padding.calculateBottomPadding()
                 )
        }
    }

    // Edit dialog
    editAccount?.let { acc ->
        AccountFormSheet(
            initialType = acc.type,
            existing    = acc,
            onSave      = { name, type, balance, color, currency, description, includeInTotal ->
                viewModel.update(
                    acc.copy(
                        name           = name,
                        type           = type,
                        balance        = balance,
                        colorHex       = color,
                        currency       = currency,
                        description    = description,
                        includeInTotal = includeInTotal
                    )
                )
                editAccount = null
            },
            onDismiss = { editAccount = null }
        )
    }
}

// ── Top bar (non-embedded) ────────────────────────────────────────────────────

@Composable
private fun AccountsTopBar(
    totalBalance: Double,
    onAddClick:   () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onAddClick, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Add, "Додати рахунок",
                tint     = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Вкладка "Рахунки" ─────────────────────────────────────────────────────────

@Composable
private fun AccountsListTab(
    state:         AccountsUiState,
    bottomPadding: Dp,
    onAdd:         () -> Unit,
    onEdit:        (AccountEntity) -> Unit,
    onDelete:      (AccountEntity) -> Unit,
    onSetDefault:  (AccountEntity) -> Unit
) {
    LazyColumn(
        contentPadding      = PaddingValues(
            start  = 16.dp, end = 16.dp,
            top    = 8.dp,  bottom = bottomPadding + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Рахунки",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatMoney(state.totalBalance),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (state.totalBalance < 0) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary
                )
            }
        }

        items(state.accounts) { account ->
            AccountListItem(
                account      = account,
                onEdit       = { onEdit(account) },
                onDelete     = { onDelete(account) },
                onSetDefault = { onSetDefault(account) }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            Spacer(Modifier.height(4.dp))
            AddAccountItem(onClick = onAdd)
        }
    }
}

// ── Елемент рахунку ───────────────────────────────────────────────────────────

@Composable
private fun AccountListItem(
    account:     AccountEntity,
    onEdit:      () -> Unit,
    onDelete:    () -> Unit,
    onSetDefault: () -> Unit
) {
    val accentColor = remember(account.colorHex) {
        try { Color(android.graphics.Color.parseColor(account.colorHex)) }
        catch (_: Exception) { Color(0xFF4361EE) }
    }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccountIconBox(account = account, accentColor = accentColor)

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.name,
                style     = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                color     = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            val balColor = when {
                account.balance < 0    -> MaterialTheme.colorScheme.error
                account.balance == 0.0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                else                   -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                "${formatMoney(account.balance)} ${account.currency}",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = balColor
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text        = { Text("Редагувати") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick     = { onEdit(); showMenu = false }
                )
                if (!account.isDefault) {
                    DropdownMenuItem(
                        text        = { Text("Зробити основним") },
                        leadingIcon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700)) },
                        onClick     = { onSetDefault(); showMenu = false }
                    )
                }
                DropdownMenuItem(
                    text        = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = { onDelete(); showMenu = false }
                )
            }
        }
    }
}

// ── Квадратна іконка рахунку з зірочкою ──────────────────────────────────────

@Composable
private fun AccountIconBox(account: AccountEntity, accentColor: Color) {
    Box(modifier = Modifier.size(64.dp)) {
        Box(
            modifier         = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = accountTypeIcon(account.type),
                contentDescription = null,
                tint        = Color.White,
                modifier    = Modifier.size(28.dp)
            )
        }
        if (account.isDefault) {
            Box(
                modifier         = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-2).dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star, "Основний",
                    tint     = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Кнопка "Додати рахунок" ───────────────────────────────────────────────────

@Composable
private fun AddAccountItem(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier         = Modifier
                    .size(60.dp)
                    .dashedBorder(
                        color        = dashColor,
                        cornerRadius = 14.dp,
                        dashWidth    = 8.dp,
                        dashGap      = 5.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, null,
                    tint     = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            "Додати рахунок",
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ── Вкладка "Мої фінанси" ─────────────────────────────────────────────────────

@Composable
private fun MyFinancesTab(
    state:         AccountsUiState,
    bottomPadding: Dp
) {
    val totalAssets = state.accounts
        .filter { it.type != AccountType.DEBT && it.includeInTotal && it.balance > 0 }
        .sumOf { it.balance }
    val totalDebts = state.accounts
        .filter { it.includeInTotal }
        .sumOf { acc ->
            when {
                acc.type == AccountType.DEBT -> acc.balance
                acc.balance < 0             -> -acc.balance
                else                        -> 0.0
            }
        }
    val net = totalAssets - totalDebts

    LazyColumn(
        contentPadding = PaddingValues(
            start  = 16.dp, end = 16.dp,
            top    = 16.dp, bottom = bottomPadding + 16.dp
        )
    ) {
        item {
            Text(
                "Мої фінанси",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
                modifier   = Modifier.padding(bottom = 12.dp)
            )

            // ── Таблиця активи / борги ──────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(Modifier.weight(0.18f).padding(vertical = 10.dp))
                        FinanceHeaderCell("АКТИВИ", Modifier.weight(1f))
                        VerticalDividerLine()
                        FinanceHeaderCell("БОРГИ", Modifier.weight(1f))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Data row
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier         = Modifier
                                .weight(0.18f)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "=",
                                style      = MaterialTheme.typography.titleMedium,
                                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FinanceValueCell(totalAssets, Modifier.weight(1f))
                        VerticalDividerLine()
                        FinanceValueCell(totalDebts, Modifier.weight(1f), isDebt = true)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Net row
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            formatMoney(net),
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color      = if (net < 0) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceHeaderCell(text: String, modifier: Modifier) {
    Box(
        modifier         = modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun FinanceValueCell(amount: Double, modifier: Modifier, isDebt: Boolean = false) {
    Box(
        modifier         = modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            formatMoney(amount),
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color      = when {
                amount == 0.0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                isDebt        -> MaterialTheme.colorScheme.error
                else          -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ── Helper functions ──────────────────────────────────────────────────────────

internal fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH       -> Icons.Outlined.Wallet
    AccountType.CARD       -> Icons.Outlined.CreditCard
    AccountType.SAVING     -> Icons.Outlined.Savings
    AccountType.INVESTMENT -> Icons.Outlined.TrendingUp
    AccountType.DEBT       -> Icons.Outlined.MoneyOff
    AccountType.OTHER      -> Icons.Outlined.AccountBalance
}

internal fun accountTypeName(type: AccountType) = when (type) {
    AccountType.CASH       -> "Готівка"
    AccountType.CARD       -> "Карта"
    AccountType.SAVING     -> "Заощадження"
    AccountType.INVESTMENT -> "Інвестиції"
    AccountType.DEBT       -> "Борговий"
    AccountType.OTHER      -> "Інше"
}

// Modifier — пунктирна рамка
private fun Modifier.dashedBorder(
    color:        Color,
    cornerRadius: Dp,
    dashWidth:    Dp,
    dashGap:      Dp,
    strokeWidth:  Dp = 1.5.dp
): Modifier = this.drawBehind {
    val cr = cornerRadius.toPx()
    val sw = strokeWidth.toPx()
    val dw = dashWidth.toPx()
    val dg = dashGap.toPx()
    drawRoundRect(
        color        = color,
        size         = Size(size.width, size.height),
        cornerRadius = CornerRadius(cr, cr),
        style        = Stroke(width = sw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dw, dg), 0f))
    )
}
