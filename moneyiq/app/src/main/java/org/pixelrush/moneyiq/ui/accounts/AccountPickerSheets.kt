package org.pixelrush.moneyiq.ui.accounts

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.background
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.border
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.clickable
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.layout.*
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.CircleShape
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.Icons
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.filled.*
import androidx.core.graphics.toColorInt
import androidx.compose.material3.*
import androidx.core.graphics.toColorInt
import androidx.compose.runtime.*
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Alignment
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Modifier
import androidx.core.graphics.toColorInt
import androidx.compose.ui.draw.clip
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.toColorInt
import androidx.compose.ui.unit.dp
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.ui.components.calculator.AmountCalculatorSheet
import org.pixelrush.moneyiq.ui.components.icons.RoundedIconBox

// ── TypePickerSheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypePickerSheet(
    selected:  AccountType,
    onSelect:  (AccountType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                "Тип рахунку",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
            AccountType.entries.forEach { t ->
                ListItem(
                    modifier          = Modifier.clickable { onSelect(t) },
                    leadingContent    = {
                        Icon(accountTypeIcon(t), null,
                            tint = if (t == selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    },
                    headlineContent   = {
                        Text(
                            accountTypeNameUA(t),
                            color      = if (t == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (t == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingContent   = {
                        if (t == selected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

// ── ColorPickerSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    colors:    List<String>,
    selected:  String,
    onSelect:  (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
            Text(
                "Колір рахунку",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
            // Grid: 4 per row
            colors.chunked(4).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { hex ->
                        val c = try { Color(hex.toColorInt()) }
                                catch (_: Exception) { Color.Gray }
                        Box(
                            modifier         = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { onSelect(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected.equals(hex, ignoreCase = true)) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    // Fill empty cells
                    repeat(4 - row.size) {
                        Spacer(Modifier.size(52.dp))
                    }
                }
            }
        }
    }
}

// ── DescEditorDialog ──────────────────────────────────────────────────────────

@Composable
fun DescEditorDialog(
    initial:   String,
    onSave:    (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Опис") },
        text    = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Введіть опис...") },
                maxLines      = 4
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ── BalanceInputDialog ────────────────────────────────────────────────────────

@Composable
fun BalanceInputDialog(
    initial:   String,
    symbol:    String,
    onSave:    (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Баланс рахунку") },
        text    = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("0") },
                singleLine    = true,
                suffix        = { Text(" $symbol") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ── AccountActionSheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActionSheet(
    account:         org.pixelrush.moneyiq.data.db.entities.AccountEntity,
    onDismiss:       () -> Unit,
    onEdit:          () -> Unit,
    onAdjustBalance: () -> Unit,
    onTransactions:  () -> Unit,
    onIncome:        () -> Unit,
    onExpense:       () -> Unit,
    onTransfer:      () -> Unit,
    onSetDefault:    () -> Unit
) {
    val accentColor = remember(account.colorHex) {
        try { Color(account.colorHex.toColorInt()) }
        catch (_: Exception) { Color(0xFF4361EE) }
    }
    val isLightCard = accentColor.luminance() > 0.5f
    val onCard      = if (isLightCard) Color(0xFF1C1B1F) else Color.White
    val sym = currencySymbol(account.currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // ── Картка рахунку ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor)
                    .padding(20.dp)
            ) {
                // Іконка + назва зліва
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundedIconBox(
                        icon     = accountIconFromKey(account.icon),
                        color    = onCard.copy(alpha = 0.15f),
                        iconSize = 28.dp,
                        tint     = onCard
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        account.name,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = onCard
                    )
                }

                // Зірочка справа
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(onCard.copy(alpha = 0.15f))
                        .clickable { onSetDefault(); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Основний рахунок",
                        tint     = if (account.isDefault) Color(0xFFFFD700) else onCard,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Баланс по центру знизу
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Баланс рахунку",
                        style = MaterialTheme.typography.bodySmall,
                        color = onCard.copy(alpha = 0.8f)
                    )
                    Text(
                        "${org.pixelrush.moneyiq.ui.main.formatMoney(account.balance)} $sym",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = onCard
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Рядки кнопок ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccountActionButton(Icons.Default.Edit,                 "Редагувати", Color(0xFFFFAB00)) { onEdit(); onDismiss() }
                AccountActionButton(Icons.Default.SwapVert,             "Баланс",     Color(0xFF9E9E9E)) { onAdjustBalance(); onDismiss() }
                AccountActionButton(Icons.AutoMirrored.Filled.ReceiptLong, "Операції", Color(0xFF5C6BC0)) { onTransactions(); onDismiss() }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccountActionButton(Icons.Default.ArrowUpward,              "Поповнення", Color(0xFF00897B)) { onIncome(); onDismiss() }
                AccountActionButton(Icons.Default.ArrowDownward,            "Списати",    Color(0xFFE91E63)) { onExpense(); onDismiss() }
                AccountActionButton(Icons.AutoMirrored.Filled.ArrowForward, "Переказ",   Color(0xFF9E9E9E)) { onTransfer(); onDismiss() }
            }
        }
    }
}

@Composable
private fun AccountActionButton(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    color:   Color,
    onClick: () -> Unit
) {
    Column(
        modifier            = Modifier
            .widthIn(min = 80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
