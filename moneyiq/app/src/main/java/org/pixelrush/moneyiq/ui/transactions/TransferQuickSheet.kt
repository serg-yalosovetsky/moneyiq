package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.outlined.CalendarMonth
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
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.ui.components.calculator.AccountPickerSheet
import org.pixelrush.moneyiq.ui.components.calculator.CalcDateSheet
import org.pixelrush.moneyiq.ui.components.calculator.FullDatePickerDialog
import org.pixelrush.moneyiq.ui.components.calculator.SharedCalcKeypad
import org.pixelrush.moneyiq.ui.components.calculator.rememberCalcState
import org.pixelrush.moneyiq.ui.components.calculator.txDateLabelPublic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransferQuickSheet(
    fromAccount: AccountEntity,
    allAccounts: List<AccountEntity>,
    onSave:      (toAccountId: Long, amount: Double, date: Long) -> Unit,
    onDismiss:   () -> Unit
) {
    var selectedFrom by remember { mutableStateOf(fromAccount) }
    var selectedTo   by remember { mutableStateOf(allAccounts.firstOrNull { it.id != fromAccount.id }) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var note         by remember { mutableStateOf("") }
    var showDateSheet    by remember { mutableStateOf(false) }
    var showFullDate     by remember { mutableStateOf(false) }
    var showFromAccSheet by remember { mutableStateOf(false) }
    var showToAccSheet   by remember { mutableStateOf(false) }

    val calc = rememberCalcState()

    val fromColor = remember(selectedFrom.colorHex) {
        try { Color(android.graphics.Color.parseColor(selectedFrom.colorHex)) }
        catch (_: Exception) { Color(0xFF26A69A) }
    }
    val toColor = remember(selectedTo?.colorHex) {
        try { Color(android.graphics.Color.parseColor(selectedTo?.colorHex ?: "#3949AB")) }
        catch (_: Exception) { Color(0xFF3949AB) }
    }
    val transferColor = Color(0xFF5C6BC0)
    val keyBg = MaterialTheme.colorScheme.surfaceVariant
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.72f)) {

            Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(fromColor).clickable { showFromAccSheet = true }
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp)) {
                        Text("З рахунку", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(selectedFrom.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(toColor).clickable { showToAccSheet = true }
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.CreditCard, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp), horizontalAlignment = Alignment.End) {
                        Text("На рахунок", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(selectedTo?.name ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Переказ", style = MaterialTheme.typography.labelMedium, color = transferColor)
                Text(calc.displayExpr("₴"), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = transferColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                placeholder = { Text("Нотатки...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(6.dp))

            SharedCalcKeypad(
                calc         = calc,
                modifier     = Modifier.weight(1f).fillMaxWidth(),
                confirmColor = transferColor,
                onConfirm    = {
                    val amt   = calc.result()
                    val toAcc = selectedTo ?: return@SharedCalcKeypad
                    if (amt > 0) onSave(toAcc.id, amt, selectedDate)
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    if (showDateSheet) {
        CalcDateSheet(
            currentDate = selectedDate, repeatMode = "NEVER", reminderMode = "NEVER",
            onDateSelected  = { selectedDate = it; showDateSheet = false },
            onRepeatClick   = { showDateSheet = false },
            onReminderClick = { showDateSheet = false },
            onPickDate      = { showDateSheet = false; showFullDate = true },
            onDismiss       = { showDateSheet = false }
        )
    }
    if (showFullDate) {
        FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }
    if (showFromAccSheet) {
        AccountPickerSheet(
            accounts   = allAccounts.filter { it.id != selectedTo?.id },
            selectedId = selectedFrom.id,
            label      = "З рахунку",
            onSelect   = { acc -> selectedFrom = acc; showFromAccSheet = false },
            onDismiss  = { showFromAccSheet = false }
        )
    }
    if (showToAccSheet) {
        AccountPickerSheet(
            accounts   = allAccounts.filter { it.id != selectedFrom.id },
            selectedId = selectedTo?.id,
            label      = "На рахунок",
            onSelect   = { acc -> selectedTo = acc; showToAccSheet = false },
            onDismiss  = { showToAccSheet = false }
        )
    }
}
