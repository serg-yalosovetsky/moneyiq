package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.categories.AmountCalculatorSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAmountCalc   by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Редактирование" else "Новая транзакция") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // Кнопка удаления — только в режиме редактирования
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        else Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Тип транзакции — основные 3 на сегментированной кнопке
            val mainTypes = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                mainTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, mainTypes.size),
                        label = {
                            Text(when (type) {
                                TransactionType.EXPENSE  -> "Расход"
                                TransactionType.INCOME   -> "Доход"
                                TransactionType.TRANSFER -> "Перевод"
                                else -> ""
                            })
                        }
                    )
                }
            }

            // Долговые типы — дополнительные кнопки
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    TransactionType.BORROW to "Взять в долг",
                    TransactionType.LEND   to "Дать в долг",
                    TransactionType.REPAY  to "Вернуть долг"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Сума (відкриває калькулятор)
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value         = if (state.amount.isEmpty()) "0 ₴" else "${state.amount} ₴",
                    onValueChange = {},
                    label         = { Text("Сума") },
                    modifier      = Modifier.fillMaxWidth(),
                    readOnly      = true,
                    enabled       = false,
                    leadingIcon   = { Icon(Icons.Default.AttachMoney, null) },
                    singleLine    = true,
                    isError       = state.error?.contains("сумму") == true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledTextColor        = if (state.error?.contains("сумму") == true)
                                                       MaterialTheme.colorScheme.error
                                                   else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor      = if (state.error?.contains("сумму") == true)
                                                       MaterialTheme.colorScheme.error
                                                   else MaterialTheme.colorScheme.outline,
                        disabledLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(Modifier.matchParentSize().clickable { showAmountCalc = true })
            }

            // Счёт
            ExposedDropdownMenuBox(
                items = state.accounts.map { it.id to it.name },
                selectedId = state.selectedAccountId,
                label = "Счёт",
                onSelect = viewModel::setAccount
            )

            // Счёт назначения (для перевода и погашения долга)
            if (state.type == TransactionType.TRANSFER || state.type == TransactionType.REPAY) {
                ExposedDropdownMenuBox(
                    items = state.accounts
                        .filter { it.id != state.selectedAccountId }
                        .map { it.id to it.name },
                    selectedId = state.selectedToAccountId,
                    label = if (state.type == TransactionType.REPAY) "Счёт-получатель (необязательно)" else "Счёт назначения",
                    onSelect = { viewModel.setToAccount(it) }
                )
            }

            // Категория (только для доходов и расходов)
            val categoryType = when (state.type) {
                TransactionType.INCOME -> TransactionType.INCOME
                TransactionType.EXPENSE, TransactionType.BORROW,
                TransactionType.LEND, TransactionType.REPAY -> TransactionType.EXPENSE
                else -> null
            }
            if (categoryType != null) {
                val filteredCategories = state.categories.filter { it.type == categoryType }
                ExposedDropdownMenuBox(
                    items = listOf(null to "Без категории") +
                            filteredCategories.map { it.id to it.name },
                    selectedId = state.selectedCategoryId,
                    label = "Категория",
                    onSelect = { viewModel.setCategory(it) }
                )
            }

            // Заметка
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Заметка (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                singleLine = true
            )

            // Дата
            val dateStr = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(state.date))
            OutlinedTextField(
                value = dateStr,
                onValueChange = {},
                label = { Text("Дата") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                readOnly = true,
                singleLine = true
            )

            // Ошибка
            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }

    // Калькулятор для суми
    if (showAmountCalc) {
        AmountCalculatorSheet(
            initial   = state.amount.replace(",", ".").toDoubleOrNull() ?: 0.0,
            title     = "Сума транзакції",
            onResult  = { v ->
                viewModel.setAmount(v.toBigDecimal().stripTrailingZeros().toPlainString())
                showAmountCalc = false
            },
            onDismiss = { showAmountCalc = false }
        )
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить транзакцию?") },
            text = { Text("Транзакция будет удалена, а баланс счёта скорректирован. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBox(
    items: List<Pair<Long?, String>>,
    selectedId: Long?,
    label: String,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.find { it.first == selectedId }?.second ?: items.firstOrNull()?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}
