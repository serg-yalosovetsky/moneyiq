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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.ui.main.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    embeddedMode: Boolean = false,       // true = встроен в MainScreen (без TopAppBar/Scaffold)
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<AccountEntity?>(null) }

    if (embeddedMode) {
        // Встроенный режим — без Scaffold, FAB добавляется через иконку в шапке
        Box(Modifier.fillMaxSize()) {
            AccountsContent(
                state = state,
                padding = PaddingValues(bottom = 88.dp),
                onEdit = { editAccount = it },
                onDelete = { viewModel.delete(it) }
            )
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Добавить счёт")
            }
        }
        if (showAddDialog || editAccount != null) {
            AccountDialog(
                existing = editAccount,
                onSave = { name, type, balance, color ->
                    if (editAccount != null) viewModel.update(editAccount!!.copy(name = name, type = type, balance = balance, colorHex = color))
                    else viewModel.add(name, type, balance, color)
                    showAddDialog = false; editAccount = null
                },
                onDismiss = { showAddDialog = false; editAccount = null }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счета") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Добавить счёт")
            }
        }
    ) { padding ->
        AccountsContent(
            state = state,
            padding = padding,
            onEdit = { editAccount = it },
            onDelete = { viewModel.delete(it) }
        )
    }

    if (showAddDialog || editAccount != null) {
        AccountDialog(
            existing = editAccount,
            onSave = { name, type, balance, color ->
                if (editAccount != null) {
                    viewModel.update(
                        editAccount!!.copy(
                            name = name, type = type,
                            balance = balance, colorHex = color
                        )
                    )
                } else {
                    viewModel.add(name, type, balance, color)
                }
                showAddDialog = false
                editAccount = null
            },
            onDismiss = {
                showAddDialog = false
                editAccount = null
            }
        )
    }
}

@Composable
private fun AccountsContent(
    state: AccountsUiState,
    padding: PaddingValues,
    onEdit: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Общий баланс",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(formatMoney(state.totalBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        items(state.accounts) { account ->
            AccountItem(account = account, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun AccountItem(
    account: AccountEntity,
    onEdit: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = color)
            }
        },
        headlineContent = { Text(account.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                accountTypeName(account.type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMoney(account.balance),
                    fontWeight = FontWeight.Bold,
                    color = if (account.balance >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { onEdit(account); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { onDelete(account); showMenu = false }
                        )
                    }
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDialog(
    existing: AccountEntity?,
    onSave: (String, AccountType, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var balance by remember { mutableStateOf(existing?.balance?.toString() ?: "0") }
    var type by remember { mutableStateOf(existing?.type ?: AccountType.CASH) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: "#4CAF50") }

    val colorPalette = listOf(
        "#4CAF50", "#2196F3", "#FF5722", "#9C27B0",
        "#FF9800", "#009688", "#F44336", "#607D8B"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Редактировать счёт" else "Новый счёт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название счёта") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Начальный баланс") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Тип счёта", style = MaterialTheme.typography.labelMedium)
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountTypeName(type),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AccountType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(accountTypeName(t)) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }

                Text("Цвет", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorPalette.forEach { hex ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = balance.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name, type, b, colorHex)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun accountTypeName(type: AccountType) = when (type) {
    AccountType.CASH       -> "Наличные"
    AccountType.CARD       -> "Карта"
    AccountType.SAVING     -> "Сбережения"
    AccountType.INVESTMENT -> "Инвестиции"
    AccountType.DEBT       -> "Долг"
    AccountType.OTHER      -> "Другое"
}
