package org.pixelrush.moneyiq.ui.accounts

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
    padding: PaddingValues = PaddingValues(),
    onNavigateBack: () -> Unit = {},
    embeddedMode: Boolean = false,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<AccountEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
    ) {
        // ── Шапка (аналог оригинала) ────────────────────────────────────────
        AccountsTopBar(
            totalBalance = state.totalBalance,
            onAddClick = { showAddDialog = true }
        )

        // ── Две вкладки: Счета | Мои финансы ────────────────────────────────
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Text(
                        "Счета",
                        fontWeight = if (selectedSubTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Text(
                        "Мои финансы",
                        fontWeight = if (selectedSubTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }

        when (selectedSubTab) {
            0 -> AccountsListTab(
                state = state,
                bottomPadding = padding.calculateBottomPadding(),
                onAdd = { showAddDialog = true },
                onEdit = { editAccount = it },
                onDelete = { viewModel.delete(it) },
                onSetDefault = { viewModel.setDefault(it) }
            )
            1 -> MyFinancesTab(
                state = state,
                bottomPadding = padding.calculateBottomPadding()
            )
        }
    }

    // ── Диалоги ──────────────────────────────────────────────────────────────
    if (showAddDialog || editAccount != null) {
        AccountEditSheet(
            existing = editAccount,
            onSave = { name, type, balance, color, currency ->
                if (editAccount != null) {
                    viewModel.update(
                        editAccount!!.copy(
                            name = name, type = type,
                            balance = balance, colorHex = color, currency = currency
                        )
                    )
                } else {
                    viewModel.add(name, type, balance, color, currency)
                }
                showAddDialog = false
                editAccount = null
            },
            onDismiss = { showAddDialog = false; editAccount = null }
        )
    }
}

// ── Top bar (шапка как в оригинале) ──────────────────────────────────────────

@Composable
private fun AccountsTopBar(
    totalBalance: Double,
    onAddClick: () -> Unit
) {
    val balanceColor = if (totalBalance < 0)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар / профиль
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = "Профиль",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Заголовок + сумма
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Все счета",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(totalBalance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
        }

        Spacer(Modifier.width(12.dp))

        // Кнопка добавить
        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Добавить счёт",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Вкладка "Счета" ───────────────────────────────────────────────────────────

@Composable
private fun AccountsListTab(
    state: AccountsUiState,
    bottomPadding: Dp,
    onAdd: () -> Unit,
    onEdit: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit,
    onSetDefault: (AccountEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = bottomPadding + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Заголовок секции
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Счета",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatMoney(state.totalBalance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.totalBalance < 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Список счетов
        items(state.accounts) { account ->
            AccountListItem(
                account = account,
                onEdit = { onEdit(account) },
                onDelete = { onDelete(account) },
                onSetDefault = { onSetDefault(account) }
            )
            Spacer(Modifier.height(4.dp))
        }

        // Кнопка добавить счёт
        item {
            Spacer(Modifier.height(4.dp))
            AddAccountItem(onClick = onAdd)
        }
    }
}

// ── Элемент счёта ─────────────────────────────────────────────────────────────

@Composable
private fun AccountListItem(
    account: AccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val accentColor = remember(account.colorHex) {
        try { Color(android.graphics.Color.parseColor(account.colorHex)) }
        catch (_: Exception) { Color(0xFF4361EE) }
    }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка счёта (квадратная)
        AccountIconBox(account = account, accentColor = accentColor)

        Spacer(Modifier.width(16.dp))

        // Название + баланс
        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            val balanceColor = when {
                account.balance < 0  -> MaterialTheme.colorScheme.error
                account.balance == 0.0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                else                 -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                "${formatMoney(account.balance)} ${account.currency}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = balanceColor
            )
        }

        // Контекстное меню
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { onEdit(); showMenu = false }
                )
                if (!account.isDefault) {
                    DropdownMenuItem(
                        text = { Text("Сделать основным") },
                        leadingIcon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700)) },
                        onClick = { onSetDefault(); showMenu = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = { onDelete(); showMenu = false }
                )
            }
        }
    }
}

// ── Квадратная иконка счёта со звёздочкой ────────────────────────────────────

@Composable
private fun AccountIconBox(account: AccountEntity, accentColor: Color) {
    Box(modifier = Modifier.size(64.dp)) {
        // Основной квадрат с иконкой типа счёта
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = accountTypeIcon(account.type),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Звёздочка "основной счёт" — внизу слева
        if (account.isDefault) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-2).dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Основной",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Элемент "Добавить счёт" ───────────────────────────────────────────────────

@Composable
private fun AddAccountItem(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Пунктирный квадрат с "+"
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .dashedBorder(
                        color = dashColor,
                        cornerRadius = 14.dp,
                        dashWidth = 8.dp,
                        dashGap = 5.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Text(
            "Добавить счёт",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ── Вкладка "Мои финансы" (упрощённая) ───────────────────────────────────────

@Composable
private fun MyFinancesTab(
    state: AccountsUiState,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = bottomPadding + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.accounts) { account ->
            val accentColor = remember(account.colorHex) {
                try { Color(android.graphics.Color.parseColor(account.colorHex)) }
                catch (_: Exception) { Color(0xFF4361EE) }
            }
            val balanceColor = when {
                account.balance < 0    -> MaterialTheme.colorScheme.error
                account.balance == 0.0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                else                   -> accentColor
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            accountTypeIcon(account.type), null,
                            tint = Color.White, modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            accountTypeName(account.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    Text(
                        "${formatMoney(account.balance)} ${account.currency}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }
        }

        if (state.accounts.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет счетов",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Диалог создания/редактирования счёта ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditSheet(
    existing: AccountEntity?,
    onSave: (name: String, type: AccountType, balance: Double, color: String, currency: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name     by remember { mutableStateOf(existing?.name     ?: "") }
    var balance  by remember { mutableStateOf(existing?.balance?.let {
        if (it == 0.0) "" else it.toString()
    } ?: "") }
    var type     by remember { mutableStateOf(existing?.type     ?: AccountType.CASH) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: "#4361EE") }
    var currency by remember { mutableStateOf(existing?.currency ?: "RUB") }

    val colorPalette = listOf(
        "#4361EE", "#3A86FF", "#8338EC", "#FF006E",
        "#FB5607", "#FFBE0B", "#06D6A0", "#118AB2",
        "#4CAF50", "#009688", "#607D8B", "#F44336"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing != null) "Редактировать счёт" else "Новый счёт")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Название
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.AccountBalanceWallet, null)
                    }
                )

                // Начальный баланс
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Начальный баланс") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) }
                )

                // Тип счёта
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountTypeName(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип счёта") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        leadingIcon = {
                            Icon(accountTypeIcon(type), null,
                                tint = try { Color(android.graphics.Color.parseColor(colorHex)) }
                                catch (_: Exception) { MaterialTheme.colorScheme.primary }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AccountType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(accountTypeName(t)) },
                                leadingIcon = { Icon(accountTypeIcon(t), null) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }

                // Валюта
                var currencyExpanded by remember { mutableStateOf(false) }
                val currencies = listOf("RUB", "USD", "EUR", "UAH", "GBP", "CNY", "JPY", "CHF")
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Валюта") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                        leadingIcon = { Icon(Icons.Outlined.Language, null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencies.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { currency = c; currencyExpanded = false }
                            )
                        }
                    }
                }

                // Выбор цвета
                Text("Цвет", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorPalette) { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (_: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(Icons.Default.Check, null,
                                    modifier = Modifier.size(18.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val b = balance.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name, type, b, colorHex, currency)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ── Вспомогательные функции ───────────────────────────────────────────────────

internal fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH       -> Icons.Outlined.Wallet
    AccountType.CARD       -> Icons.Outlined.CreditCard
    AccountType.SAVING     -> Icons.Outlined.Savings
    AccountType.INVESTMENT -> Icons.Outlined.TrendingUp
    AccountType.DEBT       -> Icons.Outlined.MoneyOff
    AccountType.OTHER      -> Icons.Outlined.AccountBalance
}

internal fun accountTypeName(type: AccountType) = when (type) {
    AccountType.CASH       -> "Наличные"
    AccountType.CARD       -> "Карта"
    AccountType.SAVING     -> "Сбережения"
    AccountType.INVESTMENT -> "Инвестиции"
    AccountType.DEBT       -> "Долг"
    AccountType.OTHER      -> "Другое"
}

// Modifier — пунктирная рамка (Compose Canvas)
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    dashWidth: androidx.compose.ui.unit.Dp,
    dashGap: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.5.dp
): Modifier = this.drawBehind {
    val cr   = cornerRadius.toPx()
    val sw   = strokeWidth.toPx()
    val dw   = dashWidth.toPx()
    val dg   = dashGap.toPx()

    drawRoundRect(
        color = color,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(
            width = sw,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dw, dg), 0f)
        )
    )
}
