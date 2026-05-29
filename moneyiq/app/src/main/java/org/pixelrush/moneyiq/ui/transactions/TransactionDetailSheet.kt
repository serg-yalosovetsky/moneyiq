package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.components.calculator.CalcDateSheet
import org.pixelrush.moneyiq.ui.components.dialogs.ConfirmationDialog
import org.pixelrush.moneyiq.ui.components.calculator.FullDatePickerDialog
import org.pixelrush.moneyiq.ui.components.calculator.SharedCalcKeypad
import org.pixelrush.moneyiq.ui.components.calculator.rememberCalcState
import org.pixelrush.moneyiq.ui.components.calculator.txDateLabelPublic
import org.pixelrush.moneyiq.ui.main.formatMoney

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
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it; isDirty = true },
                    placeholder   = { Text("Нотатки...") },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(6.dp))

                Text(
                    txDateLabelPublic(selectedDate),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                )

                Spacer(Modifier.weight(1f))

                HorizontalDivider()

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
        ConfirmationDialog(
            title     = "Видалити транзакцію?",
            message   = "Транзакцію буде видалено, а баланс рахунку скориговано. Цю дію не можна скасувати.",
            icon      = Icons.Outlined.DeleteForever,
            onConfirm = { showDeleteDialog = false; onDelete() },
            onDismiss = { showDeleteDialog = false }
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
