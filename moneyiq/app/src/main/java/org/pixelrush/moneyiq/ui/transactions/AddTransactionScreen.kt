package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.components.calculator.SharedCalcKeypad
import org.pixelrush.moneyiq.ui.components.calculator.rememberCalcState
import java.text.SimpleDateFormat
import java.util.*

private val PanelFromColor   = Color(0xFF3949AB)  // indigo — рахунок-джерело (витрата/дохід)
private val TransferFromColor = Color(0xFF009688)  // teal — рахунок-джерело (переказ)
private val TransferToColor   = Color(0xFF3949AB)  // indigo — рахунок-призначення
private val ExpenseColor      = Color(0xFFE53935)  // red
private val IncomeColor       = Color(0xFF43A047)  // green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val calc  = rememberCalcState()

    // Ініціалізуємо калькулятор у режимі редагування
    var editInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(state.isEditMode, state.amount) {
        if (state.isEditMode && !editInitialized && state.amount.isNotBlank()) {
            val amt = state.amount.replace(",", ".").toDoubleOrNull() ?: return@LaunchedEffect
            calc.currentStr = when {
                amt == amt.toLong().toDouble() -> amt.toLong().toString()
                else -> amt.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
            }
            editInitialized = true
        }
    }

    LaunchedEffect(state.saved) { if (state.saved) onNavigateBack() }

    var showDeleteDialog  by remember { mutableStateOf(false) }
    var showDatePicker    by remember { mutableStateOf(false) }
    var showFromAccPicker by remember { mutableStateOf(false) }
    var showToAccPicker   by remember { mutableStateOf(false) }
    var showCatPicker     by remember { mutableStateOf(false) }

    val isTransfer = state.type == TransactionType.TRANSFER

    val fromAccount  = state.accounts.firstOrNull { it.id == state.selectedAccountId }
    val toAccount    = state.accounts.firstOrNull { it.id == state.selectedToAccountId }
    val catType      = if (state.type == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
    val fromCategory = state.categories.filter { it.type == catType && !it.archived }
                           .firstOrNull { it.id == state.selectedCategoryId }

    val fromCatColor = remember(fromCategory?.colorHex) {
        fromCategory?.colorHex?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
        }
    }

    val leftPanelColor  = if (isTransfer) TransferFromColor else PanelFromColor
    val rightPanelColor = when {
        isTransfer -> TransferToColor
        else       -> fromCatColor ?: Color(0xFF757575)
    }
    val accentColor = when (state.type) {
        TransactionType.TRANSFER -> TransferToColor
        TransactionType.INCOME   -> IncomeColor
        else                     -> ExpenseColor
    }

    fun onSave() {
        val v = calc.result()
        viewModel.setAmount(if (v <= 0.0) "" else v.toBigDecimal().stripTrailingZeros().toPlainString())
        viewModel.save()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── 1. Навігація + перемикач типів ───────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                listOf(
                    TransactionType.EXPENSE  to "Витрата",
                    TransactionType.INCOME   to "Дохід",
                    TransactionType.TRANSFER to "Переказ"
                ).forEach { (type, label) ->
                    val selected = state.type == type
                    TextButton(
                        onClick          = { viewModel.setType(type) },
                        contentPadding   = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            label,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (selected) accentColor
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (state.isEditMode) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ── 2. Двопанельний заголовок ─────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {

                // Ліва панель: рахунок-джерело
                Box(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .background(leftPanelColor)
                        .clickable { showFromAccPicker = true }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                            .size(34.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)) {
                        Text("З рахунку", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            fromAccount?.name ?: "—",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(expanded = showFromAccPicker, onDismissRequest = { showFromAccPicker = false }) {
                        state.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text    = { Text(acc.name) },
                                onClick = { viewModel.setAccount(acc.id); showFromAccPicker = false }
                            )
                        }
                    }
                }

                // Права панель: рахунок-призначення (переказ) або категорія
                Box(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .background(rightPanelColor)
                        .clickable { if (isTransfer) showToAccPicker = true else showCatPicker = true }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart).padding(top = 8.dp, start = 8.dp)
                            .size(34.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isTransfer            -> Icons.Outlined.CreditCard
                                fromCategory != null  -> categoryIconFor(fromCategory.icon)
                                else                  -> Icons.Outlined.Category
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
                                isTransfer           -> toAccount?.name ?: "Обрати..."
                                fromCategory != null -> fromCategory.name
                                else                 -> "Без категорії"
                            },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isTransfer) {
                        DropdownMenu(expanded = showToAccPicker, onDismissRequest = { showToAccPicker = false }) {
                            state.accounts.filter { it.id != state.selectedAccountId }.forEach { acc ->
                                DropdownMenuItem(
                                    text    = { Text(acc.name) },
                                    onClick = { viewModel.setToAccount(acc.id); showToAccPicker = false }
                                )
                            }
                        }
                    } else {
                        DropdownMenu(expanded = showCatPicker, onDismissRequest = { showCatPicker = false }) {
                            DropdownMenuItem(
                                text    = { Text("Без категорії") },
                                onClick = { viewModel.setCategory(null); showCatPicker = false }
                            )
                            state.categories.filter { it.type == catType && !it.archived }.forEach { cat ->
                                DropdownMenuItem(
                                    text    = { Text(cat.name) },
                                    onClick = { viewModel.setCategory(cat.id); showCatPicker = false }
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Сума ───────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when (state.type) {
                        TransactionType.TRANSFER -> "Переказ"
                        TransactionType.INCOME   -> "Дохід"
                        else                     -> "Витрата"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
                Text(
                    text       = calc.displayExpr("₴"),
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            // ── 4. Нотатки ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = state.note,
                onValueChange = viewModel::setNote,
                placeholder   = { Text("Нотатки...") },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(6.dp))

            // ── 5. Калькулятор ────────────────────────────────────────────────
            val keyBg = MaterialTheme.colorScheme.surfaceVariant
            SharedCalcKeypad(
                calc          = calc,
                modifier      = Modifier.weight(1f).fillMaxWidth(),
                confirmColor  = accentColor,
                onConfirm     = { onSave() },
                row2ExtraKey  = {
                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(keyBg)
                            .clickable { showDatePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp))
                    }
                }
            )

            // ── 6. Дата ───────────────────────────────────────────────────────
            Text(
                text      = formatTxDate(state.date),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    // Вибір дати
    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = state.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    // Видалення
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Видалити транзакцію?") },
            text  = { Text("Транзакцію буде видалено, а баланс рахунку скориговано. Цю дію не можна скасувати.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.delete() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") } }
        )
    }
}

private fun formatTxDate(date: Long): String {
    val fmt  = SimpleDateFormat("d MMM yyyy 'р.'", Locale.forLanguageTag("uk"))
    val cal  = Calendar.getInstance().apply { timeInMillis = date }
    val now  = Calendar.getInstance()
    val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val short = fmt.format(Date(date))
    return when {
        sameDayTx(cal, now)  -> "Сьогодні, $short"
        sameDayTx(cal, yest) -> "Вчора, $short"
        else                 -> short
    }
}

private fun sameDayTx(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
