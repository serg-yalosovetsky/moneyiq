package org.pixelrush.moneyiq.ui.components.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.text.SimpleDateFormat
import java.util.*

// ── Date label helpers ────────────────────────────────────────────────────────

fun txDateLabelPublic(date: Long): String = txDateLabel(date)

internal fun txDateLabel(date: Long): String {
    val fmt  = SimpleDateFormat("d MMM yyyy 'р.'", Locale.forLanguageTag("uk"))
    val cal  = Calendar.getInstance().apply { timeInMillis = date }
    val now  = Calendar.getInstance()
    val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val short = fmt.format(Date(date))
    return when {
        sameDay(cal, now)  -> "Сьогодні, $short"
        sameDay(cal, yest) -> "Вчора, $short"
        else               -> short
    }
}

internal fun sameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

// ── Repeat options ────────────────────────────────────────────────────────────

private val REPEAT_OPTIONS = listOf(
    "NEVER"          to "Ніколи",
    "DAILY"          to "Щодня",
    "EVERY_2_DAYS"   to "Кожні 2 дні",
    "WEEKDAYS"       to "Будні",
    "WEEKENDS"       to "Вихідні дні",
    "WEEKLY"         to "Щотижня",
    "EVERY_2_WEEKS"  to "Кожні 2 тижні",
    "EVERY_4_WEEKS"  to "Кожні 4 тижні",
    "MONTHLY"        to "Щомісяця",
    "EVERY_2_MONTHS" to "Кожні 2 місяці",
    "EVERY_3_MONTHS" to "Кожні 3 місяці",
    "EVERY_6_MONTHS" to "Кожні 6 місяців",
    "YEARLY"         to "Щорічно"
)

internal fun repeatLabelFor(mode: String) =
    REPEAT_OPTIONS.firstOrNull { it.first == mode }?.second ?: "Ніколи"

// ── Reminder options ──────────────────────────────────────────────────────────

private val REMINDER_OPTIONS = listOf(
    "NEVER"    to "Ніколи",
    "SAME_DAY" to "Того ж дня",
    "1_DAY"    to "за 1 день до",
    "2_DAYS"   to "за 2 дні до",
    "3_DAYS"   to "за 3 дні до",
    "4_DAYS"   to "за 4 дні до",
    "5_DAYS"   to "за 5 дні до",
    "6_DAYS"   to "за 6 дні до",
    "7_DAYS"   to "за 7 дні до"
)

internal fun reminderLabelFor(mode: String) =
    REMINDER_OPTIONS.firstOrNull { it.first == mode }?.second ?: "Ніколи"

// ── Date sheet ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcDateSheet(
    currentDate:     Long,
    repeatMode:      String,
    reminderMode:    String,
    onDateSelected:  (Long) -> Unit,
    onRepeatClick:   () -> Unit,
    onReminderClick: () -> Unit,
    onPickDate:      () -> Unit,
    onDismiss:       () -> Unit
) {
    val todayMs     = System.currentTimeMillis()
    val yesterdayMs = todayMs - 86_400_000L
    val isToday     = sameDay(
        Calendar.getInstance().apply { timeInMillis = currentDate },
        Calendar.getInstance()
    )
    val isYesterday = sameDay(
        Calendar.getInstance().apply { timeInMillis = currentDate },
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    )
    val dFmt = SimpleDateFormat("d MMMM", Locale.forLanguageTag("uk"))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Дата", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Surface(
                onClick   = onPickDate,
                shape     = RoundedCornerShape(14.dp),
                color     = MaterialTheme.colorScheme.surfaceVariant,
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Виберіть день", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick  = { onDateSelected(yesterdayMs) },
                    shape    = RoundedCornerShape(14.dp),
                    color    = if (isYesterday) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.DarkMode, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Вчора", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(dFmt.format(Date(yesterdayMs)), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Surface(
                    onClick  = { onDateSelected(todayMs) },
                    shape    = RoundedCornerShape(14.dp),
                    color    = if (isToday) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.WbSunny, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Сьогодні", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(dFmt.format(Date(todayMs)), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick  = onRepeatClick,
                    shape    = RoundedCornerShape(14.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier            = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Repeat, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Повторення", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(repeatLabelFor(repeatMode), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Surface(
                    onClick  = onReminderClick,
                    shape    = RoundedCornerShape(14.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier            = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Нагадування", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(reminderLabelFor(reminderMode), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// ── Full DatePicker dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullDatePickerDialog(
    initial:       Long,
    onDateSelected: (Long) -> Unit,
    onDismiss:     () -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onDateSelected)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    ) {
        DatePicker(state = state)
    }
}

// ── Repeat dialog ─────────────────────────────────────────────────────────────

@Composable
internal fun RepeatDialog(
    current:  String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var sel by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Повторення", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            LazyColumn {
                items(REPEAT_OPTIONS) { (key, label) ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().clickable { sel = key }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sel == key, onClick = { sel = key })
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton   = { TextButton(onClick = { onSelect(sel) }) { Text("OK") } },
        dismissButton   = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

// ── Reminder dialog ───────────────────────────────────────────────────────────

@Composable
internal fun ReminderDialog(
    current:  String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var sel by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Нагадування", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            LazyColumn {
                items(REMINDER_OPTIONS) { (key, label) ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().clickable { sel = key }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sel == key, onClick = { sel = key })
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton   = { TextButton(onClick = { onSelect(sel) }) { Text("OK") } },
        dismissButton   = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

// ── Account picker sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts:   List<AccountEntity>,
    selectedId: Long?,
    label:      String = "З рахунку",
    onSelect:   (AccountEntity) -> Unit,
    onDismiss:  () -> Unit
) {
    val selected = accounts.firstOrNull { it.id == selectedId } ?: accounts.firstOrNull()
    val selColor = selected?.let {
        try { Color(android.graphics.Color.parseColor(it.colorHex)) }
        catch (_: Exception) { Color(0xFF3949AB) }
    } ?: Color(0xFF3949AB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(selColor)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CreditCard, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 10.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(selected?.name ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Баланс: ${formatMoney(selected?.balance ?: 0.0)} ₴",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Рахунки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatMoney(accounts.sumOf { it.balance })} ₴",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider()

            accounts.forEach { acc ->
                val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) }
                               catch (_: Exception) { Color(0xFF3949AB) }
                val isSelected = acc.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                        .clickable { onSelect(acc) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CreditCard, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(acc.name, fontWeight = FontWeight.Medium)
                        Text(
                            "${formatMoney(acc.balance)} ₴",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
