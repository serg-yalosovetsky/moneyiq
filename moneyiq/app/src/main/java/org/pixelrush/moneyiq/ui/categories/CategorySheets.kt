package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.util.suggestCategoryStyle
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ── Кольорова палітра ─────────────────────────────────────────────────────────

internal val CATEGORY_FORM_COLORS = listOf(
    "#FF5722", "#F44336", "#E91E63", "#9C27B0",
    "#673AB7", "#3F51B5", "#2196F3", "#03A9F4",
    "#009688", "#4CAF50", "#FFEB3B", "#FF9800",
    "#6200EA", "#FF6D00", "#0288D1"   // AI / Aliexpress / Cloud
)

// ── Іконки категорій ──────────────────────────────────────────────────────────

internal val CATEGORY_ICONS_LIST: List<Pair<String, ImageVector>> = listOf(
    "category"   to Icons.Outlined.Category,
    "shopping"   to Icons.Outlined.ShoppingCart,
    "restaurant" to Icons.Outlined.Restaurant,
    "car"        to Icons.Outlined.DirectionsCar,
    "home"       to Icons.Outlined.Home,
    "work"       to Icons.Outlined.Work,
    "school"     to Icons.Outlined.School,
    "health"     to Icons.Outlined.LocalHospital,
    "flight"     to Icons.Outlined.Flight,
    "music"      to Icons.Outlined.MusicNote,
    "money"      to Icons.Outlined.AttachMoney,
    "coffee"     to Icons.Outlined.LocalCafe,
    "pets"       to Icons.Outlined.Pets,
    "gift"       to Icons.Outlined.CardGiftcard,
    "phone"      to Icons.Outlined.PhoneAndroid,
    "sports"     to Icons.Outlined.FitnessCenter,
    "wifi"       to Icons.Outlined.Wifi,
    "delivery"   to Icons.Outlined.LocalShipping,
    "devices"    to Icons.Outlined.Devices,
    "transfer"   to Icons.Outlined.SwapHoriz,
    "family"     to Icons.Outlined.FamilyRestroom,
    "receipt"    to Icons.Outlined.Receipt,
    "beauty"     to Icons.Outlined.SelfImprovement,
    "ai"         to Icons.Outlined.Psychology,
    "aliexpress" to Icons.Outlined.LocalMall,
    "cloud"      to Icons.Outlined.Cloud,
    "clothes"    to Icons.Outlined.Checkroom,
)

internal fun categoryIconFor(iconName: String): ImageVector =
    CATEGORY_ICONS_LIST.firstOrNull { it.first == iconName }?.second ?: Icons.Outlined.Category

// ── Category Action Sheet ─────────────────────────────────────────────────────

private fun txCountLabel(n: Int): String = when {
    n % 100 in 11..19 -> "$n операцій"
    n % 10 == 1        -> "$n операція"
    n % 10 in 2..4     -> "$n операції"
    else               -> "$n операцій"
}

@Composable
fun CategoryActionSheet(
    category:      CategoryEntity,
    spending:      Double,
    txCount:       Int,
    totalInPeriod: Double,
    pillLabel:     String,
    onEdit:        () -> Unit,
    onBudget:      () -> Unit,
    onOperations:  () -> Unit,
    onDismiss:     () -> Unit
) {
    val catColor = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFF4361EE) }
    }
    val percent  = if (totalInPeriod > 0.0) (spending / totalInPeriod * 100).toInt() else 0
    val progress = if (totalInPeriod > 0.0) (spending / totalInPeriod).coerceIn(0.0, 1.0).toFloat() else 0f

    // Повноекранний Dialog = скрим + кастомний шит знизу (без кліпінгу ModalBottomSheet)
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            // ── Скрим ────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onDismiss
                    )
            )

            // ── Панель знизу ─────────────────────────────────────────────
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                // Зовнішній Box — не обрізає контент, тому іконка «виплаває» вище
                Box(Modifier.fillMaxWidth()) {
                    // Кольорова шапка
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .background(catColor)
                            .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 20.dp)
                    ) {
                        Text(
                            category.name,
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                txCountLabel(txCount),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${formatMoney(spending)} ₴",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress   = { progress },
                                modifier   = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color      = Color.White,
                                trackColor = Color.White.copy(alpha = 0.28f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "$percent%",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                pillLabel,
                                color = Color.White.copy(0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${formatMoney(totalInPeriod)} ₴",
                                color      = Color.White.copy(0.8f),
                                fontWeight = FontWeight.Medium,
                                style      = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Іконка — «виплаває» над верхнім краєм панелі на 36 dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 20.dp)
                            .offset(y = (-36).dp)   // половина від розміру кола 72 dp
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(catColor)
                            .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIconFor(category.icon), null,
                            tint     = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // ── Кнопки дій (білий фон) ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CatActionButton(Icons.Default.Edit,     "Редагувати", catColor, onEdit,       Modifier.weight(1f))
                    CatActionButton(Icons.Outlined.Speed,   "Бюджет",     catColor, onBudget,     Modifier.weight(1f))
                    CatActionButton(Icons.Outlined.Receipt, "Операції",   catColor, onOperations, Modifier.weight(1f))
                }

                // Навігаційний відступ
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
private fun CatActionButton(
    icon:     ImageVector,
    label:    String,
    color:    Color,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style     = MaterialTheme.typography.labelMedium,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

// ── Quick Expense / Income Sheet ──────────────────────────────────────────────
// Лейаут: 2 кольорові панелі (рахунок / категорія) + сума + нотатка +
//         5×4 калькулятор (оператори зліва, ✓ праворуч на 2 рядки) + дата.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickExpenseSheet(
    category:  CategoryEntity,
    accounts:  List<AccountEntity>,
    onSave:    (accountId: Long, amount: Double, note: String, date: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val catColor = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val accountColor = Color(0xFF3949AB)  // indigo — колір панелі рахунку
    val isIncome     = category.type == TransactionType.INCOME

    // ── Стан калькулятора ──────────────────────────────────────────────────
    val calc = rememberCalcState()

    // ── Інший стан ────────────────────────────────────────────────────────
    var note            by remember { mutableStateOf("") }
    var selectedAccount by remember {
        mutableStateOf(accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())
    }
    var selectedDate    by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDateSheet   by remember { mutableStateOf(false) }
    var showRepeat      by remember { mutableStateOf(false) }
    var showReminder    by remember { mutableStateOf(false) }
    var showFullDate    by remember { mutableStateOf(false) }
    var showAccSheet    by remember { mutableStateOf(false) }
    var repeatMode     by remember { mutableStateOf("NEVER") }
    var reminderMode   by remember { mutableStateOf("NEVER") }

    // ── Логіка ────────────────────────────────────────────────────────────
    fun onConfirm() {
        val amt   = calc.result()
        val accId = selectedAccount?.id ?: return
        if (amt > 0.0) {
            onSave(accId, amt, note.trim(), selectedDate)
        }
    }

    // ── Висота аркуша ≈ 2/3 екрана ────────────────────────────────────────
    val screenH  = LocalConfiguration.current.screenHeightDp.dp
    val sheetH   = screenH * 0.67f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetH)
        ) {
            // ── 1. Панелі рахунку / категорії ─────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {

                // Ліва панель — рахунок (темно-синя)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(accountColor)
                        .clickable { if (accounts.size > 1) showAccSheet = true }
                ) {
                    // Іконка рахунку (вгорі праворуч)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CreditCard, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    // Текст: «З рахунку» + назва
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 8.dp)
                    ) {
                        Text("З рахунку", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            selectedAccount?.name ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Права панель — категорія (колір категорії)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(catColor)
                ) {
                    // Іконка категорії (вгорі зліва)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 8.dp, start = 8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIconFor(category.icon), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    // Текст: «До категорії» + назва
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("До категорії", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── 2. Відображення виразу / суми ─────────────────────────────
            val displayText = calc.displayExpr("₴")

            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isIncome) "Дохід" else "Витрата",
                    style = MaterialTheme.typography.labelMedium,
                    color = catColor
                )
                Text(
                    text       = displayText,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = catColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            // ── 3. Нотатка ────────────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                placeholder   = { Text("Нотатки...") },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(6.dp))

            // ── 4. Клавіатура-калькулятор ─────────────────────────────────
            val keyBg = MaterialTheme.colorScheme.surfaceVariant
            SharedCalcKeypad(
                calc         = calc,
                modifier     = Modifier.weight(1f).fillMaxWidth(),
                onConfirm    = { onConfirm() },
                row2ExtraKey = {
                    // Кнопка календаря у правому куті 2-го рядка
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp)).background(keyBg)
                            .clickable { showDateSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp))
                    }
                }
            )

            // ── 5. Дата внизу ─────────────────────────────────────────────
            Text(
                text      = txDateLabel(selectedDate),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    // ── Вибір дати (аркуш) ────────────────────────────────────────────────────
    if (showDateSheet) {
        CalcDateSheet(
            currentDate  = selectedDate,
            repeatMode   = repeatMode,
            reminderMode = reminderMode,
            onDateSelected  = { selectedDate = it; showDateSheet = false },
            onRepeatClick   = { showDateSheet = false; showRepeat = true },
            onReminderClick = { showDateSheet = false; showReminder = true },
            onPickDate      = { showDateSheet = false; showFullDate = true },
            onDismiss       = { showDateSheet = false }
        )
    }

    // ── Повний DatePicker ─────────────────────────────────────────────────────
    if (showFullDate) {
        FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }

    // ── Діалог повторення ─────────────────────────────────────────────────────
    if (showRepeat) {
        RepeatDialog(
            current   = repeatMode,
            onSelect  = { repeatMode = it; showRepeat = false },
            onDismiss = { showRepeat = false }
        )
    }

    // ── Діалог нагадування ────────────────────────────────────────────────────
    if (showReminder) {
        ReminderDialog(
            current   = reminderMode,
            onSelect  = { reminderMode = it; showReminder = false },
            onDismiss = { showReminder = false }
        )
    }

    // ── Вибір рахунку (аркуш) ────────────────────────────────────────────────
    if (showAccSheet) {
        AccountPickerSheet(
            accounts       = accounts,
            selectedId     = selectedAccount?.id,
            label          = "З рахунку",
            onSelect       = { acc -> selectedAccount = acc; showAccSheet = false },
            onDismiss      = { showAccSheet = false }
        )
    }
}

// ── Клавіша калькулятора ──────────────────────────────────────────────────────

@Composable
private fun QKey(
    label:   String,
    modifier: Modifier = Modifier,
    isOp:    Boolean   = false,
    bg:      Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 20.sp,
            fontWeight = if (isOp) FontWeight.Normal else FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Вибір рахунку — аркуш ────────────────────────────────────────────────────

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
            // Кольорова картка вибраного рахунку
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

            // Заголовок
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

            // Список рахунків
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

// ── Форматування дати для підпису ─────────────────────────────────────────────

fun txDateLabelPublic(date: Long): String = txDateLabel(date)

private fun txDateLabel(date: Long): String {
    val fmt  = SimpleDateFormat("d MMM yyyy 'р.'", Locale("uk"))
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

private fun sameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
    a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

// ── Аркуш вибору дати (Image #14) ────────────────────────────────────────────

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
    val dFmt = SimpleDateFormat("d MMMM", Locale("uk"))

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

            // «Виберіть день» картка
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

            // Вчора / Сьогодні
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

            // Повторення / Нагадування
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

// ── Повний DatePicker (Material3) ─────────────────────────────────────────────

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

// ── Діалог «Повторення» (Image #15) ──────────────────────────────────────────

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

private fun repeatLabelFor(mode: String) =
    REPEAT_OPTIONS.firstOrNull { it.first == mode }?.second ?: "Ніколи"

@Composable
private fun RepeatDialog(
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

// ── Діалог «Нагадування» (Image #16) ─────────────────────────────────────────

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

private fun reminderLabelFor(mode: String) =
    REMINDER_OPTIONS.firstOrNull { it.first == mode }?.second ?: "Ніколи"

@Composable
private fun ReminderDialog(
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

// ── Category Form Sheet (додавання / редагування категорії) ───────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    existing:    CategoryEntity?,
    defaultType: TransactionType = TransactionType.EXPENSE,
    onSave:      (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean) -> Unit,
    onDelete:    (() -> Unit)? = null,
    onDismiss:   () -> Unit
) {
    var name     by remember { mutableStateOf(existing?.name     ?: "") }
    var type     by remember { mutableStateOf(existing?.type     ?: defaultType) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: CATEGORY_FORM_COLORS.first()) }
    var iconKey  by remember { mutableStateOf(existing?.icon     ?: "category") }
    var budget   by remember {
        mutableStateOf(
            existing?.budgetAmount?.takeIf { it > 0.0 }
                ?.let { it.toBigDecimal().stripTrailingZeros().toPlainString() } ?: ""
        )
    }
    var period   by remember { mutableStateOf(existing?.budgetPeriod ?: "MONTHLY") }
    var archived by remember { mutableStateOf(existing?.archived ?: false) }

    var showNameDialog    by remember { mutableStateOf(false) }
    var tempName          by remember { mutableStateOf("") }
    var showIconPicker    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBudgetCalc    by remember { mutableStateOf(false) }

    // Для нової категорії — одразу відкриваємо введення назви
    LaunchedEffect(Unit) {
        if (existing == null) { tempName = ""; showNameDialog = true }
    }

    // Авто-підказка стилю при введенні назви нової категорії
    LaunchedEffect(name) {
        if (existing == null && name.length >= 3 && iconKey == "category") {
            val (sugIcon, sugColor) = suggestCategoryStyle(name, type)
            iconKey  = sugIcon
            colorHex = sugColor
        }
    }

    val catColor by remember { derivedStateOf {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    } }

    fun doSave() {
        if (name.isNotBlank()) {
            val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
            onSave(name, type, colorHex, iconKey, b, period, archived)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (existing != null) "Категорія" else "Нова категорія") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick  = ::doSave,
                            enabled  = name.isNotBlank()
                        ) {
                            Text("Зберегти", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Шапка: назва + іконка ────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(end = 76.dp)) {
                            Text(
                                "Назва",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text  = name.ifBlank { "Торкніться для введення" },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (name.isBlank())
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { tempName = name; showNameDialog = true }
                            )
                        }
                        // Велика кольорова іконка праворуч
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(catColor)
                                .clickable { showIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                categoryIconFor(iconKey), null,
                                tint     = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // ── Налаштування ─────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Налаштування",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }

                // Тип (витрати / доходи)
                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.Category, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent = { Text("Тип") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(TransactionType.EXPENSE to "Витрати", TransactionType.INCOME to "Доходи")
                                    .forEach { (t, label) ->
                                        FilterChip(
                                            selected = type == t,
                                            onClick  = { type = t },
                                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }

                // Валюта
                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.AttachMoney, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Валюта категорії") },
                        supportingContent = {
                            Text(
                                "Українська гривня – ₴",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier.clickable {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }

                // Бюджет
                item {
                    val budgetNum = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.Speed, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Бюджет") },
                        supportingContent = {
                            Text(
                                if (budgetNum > 0.0) "$budget ₴ / ${if (period == "MONTHLY") "місяць" else "тиждень"}"
                                else "Без ліміту",
                                color = if (budgetNum > 0.0) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        },
                        modifier = Modifier.clickable { showBudgetCalc = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }

                // ── Підкатегорії ─────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Підкатегорії",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Default.Add, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent = {
                            Text("Додати підкатегорію",
                                color = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable {}
                    )
                    HorizontalDivider()
                }

                // ── Архів ────────────────────────────────────────────────────
                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.Archive, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Архівна категорія") },
                        trailingContent  = {
                            Switch(checked = archived, onCheckedChange = { archived = it })
                        },
                        modifier = Modifier.clickable { archived = !archived }
                    )
                    HorizontalDivider()
                }

                // ── Видалити ─────────────────────────────────────────────────
                if (onDelete != null) {
                    item {
                        ListItem(
                            leadingContent  = {
                                Icon(Icons.Default.Delete, null,
                                    tint     = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp))
                            },
                            headlineContent = {
                                Text("Видалити категорію",
                                    color = MaterialTheme.colorScheme.error)
                            },
                            modifier = Modifier.clickable { showDeleteConfirm = true }
                        )
                    }
                }
            }
        }
    }

    // ── Діалог назви ─────────────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (name.isNotBlank()) showNameDialog = false },
            title   = { Text(if (existing != null) "Назва категорії" else "Нова категорія") },
            text    = {
                OutlinedTextField(
                    value         = tempName,
                    onValueChange = { tempName = it },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Назва") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = { name = tempName; showNameDialog = false },
                    enabled  = tempName.isNotBlank()
                ) { Text("OK") }
            },
            dismissButton = {
                if (name.isNotBlank()) {
                    TextButton(onClick = { showNameDialog = false }) { Text("Скасувати") }
                }
            }
        )
    }

    // ── Пікер кольору та іконки ───────────────────────────────────────────────
    if (showIconPicker) {
        ColorIconPickerSheet(
            currentColor = colorHex,
            currentIcon  = iconKey,
            onSave       = { newColor, newIcon ->
                colorHex = newColor
                iconKey  = newIcon
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }

    // ── Видалення ─────────────────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("Видалити категорію?") },
            text    = { Text("Транзакції залишаться, але без категорії.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete?.invoke() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Скасувати") } }
        )
    }

    // ── Бюджет-калькулятор ────────────────────────────────────────────────────
    if (showBudgetCalc) {
        AmountCalculatorSheet(
            initial   = budget.replace(",", ".").toDoubleOrNull() ?: 0.0,
            title     = "Бюджет",
            onResult  = { v ->
                budget = if (v <= 0.0) "" else v.toBigDecimal().stripTrailingZeros().toPlainString()
                showBudgetCalc = false
            },
            onDismiss = { showBudgetCalc = false }
        )
    }
}

// ── Пікер кольору та іконки (BottomSheet) ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorIconPickerSheet(
    currentColor: String,
    currentIcon:  String,
    onSave:       (color: String, icon: String) -> Unit,
    onDismiss:    () -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    var selectedIcon  by remember { mutableStateOf(currentIcon) }
    val color by remember { derivedStateOf {
        try { Color(android.graphics.Color.parseColor(selectedColor)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Превʼю
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIconFor(selectedIcon), null, tint = Color.White, modifier = Modifier.size(38.dp))
            }

            // Кольори
            Text("Колір", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_FORM_COLORS.chunked(6).forEach { rowColors ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowColors.forEach { hex ->
                            val c     = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                            val isSel = hex == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp).clip(CircleShape).background(c)
                                    .then(if (isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Іконки
            Text("Іконка", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_ICONS_LIST.chunked(4).forEach { rowIcons ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowIcons.forEach { (key, icon) ->
                            val isSel = key == selectedIcon
                            Box(
                                modifier = Modifier
                                    .size(44.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) color else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null,
                                    tint     = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        repeat(4 - rowIcons.size) { Spacer(Modifier.size(44.dp)) }
                    }
                }
            }

            // Кнопка збереження
            Button(
                onClick  = { onSave(selectedColor, selectedIcon) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Застосувати")
            }
        }
    }
}

// ── Edit Categories Screen (повноекранний діалог) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoriesScreen(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    monthSpending:     Map<Long, Double>    = emptyMap(),
    monthIncome:       Map<Long, Double>    = emptyMap(),
    totalExpense:      Double               = 0.0,
    totalIncome:       Double               = 0.0,
    onSave:   (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean, existing: CategoryEntity?) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab  by remember { mutableIntStateOf(0) }
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val categories  = if (selectedTab == 0) expenseCategories.filter { !it.archived }
                      else incomeCategories.filter { !it.archived }
    val spending    = if (selectedTab == 0) monthSpending else monthIncome
    val defaultType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Редагувати категорії") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        icon     = { Icon(Icons.Outlined.ArrowCircleDown, null, modifier = Modifier.size(20.dp)) },
                        text     = { Text("Витрати", fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        icon     = { Icon(Icons.Outlined.ArrowCircleUp, null, modifier = Modifier.size(20.dp)) },
                        text     = { Text("Доходи", fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }

                CategoriesGridContent(
                    categories    = categories,
                    spending      = spending,
                    totalExpense  = totalExpense,
                    totalIncome   = totalIncome,
                    selectedTab   = selectedTab,
                    onToggleTab   = { selectedTab = if (selectedTab == 0) 1 else 0 },
                    bottomPadding = 0.dp,
                    onChipClick   = { cat -> editCategory = cat },
                    onAdd         = { showAddSheet = true }
                )
            }
        }
    }

    editCategory?.let { cat ->
        CategoryFormSheet(
            existing    = cat,
            defaultType = cat.type,
            onSave      = { name, type, color, icon, budget, period, arch ->
                onSave(name, type, color, icon, budget, period, arch, cat)
                editCategory = null
            },
            onDelete    = { onDelete(cat); editCategory = null },
            onDismiss   = { editCategory = null }
        )
    }

    if (showAddSheet) {
        CategoryFormSheet(
            existing    = null,
            defaultType = defaultType,
            onSave      = { name, type, color, icon, budget, period, _ ->
                onSave(name, type, color, icon, budget, period, false, null)
                showAddSheet = false
            },
            onDismiss   = { showAddSheet = false }
        )
    }
}

// ── Загальний стан калькулятора ───────────────────────────────────────────────

@Stable
class CalcStateHolder(initial: Double = 0.0) {

    var currentStr by mutableStateOf(
        when {
            initial <= 0.0                         -> "0"
            initial == initial.toLong().toDouble() -> initial.toLong().toString()
            else -> initial.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
        }
    )
    var pendingVal by mutableStateOf(0.0)
    var pendingOp  by mutableStateOf<String?>(null)

    /** Обчислює поточний результат виразу */
    fun result(): Double {
        val c = currentStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        return when (pendingOp) {
            "+"  -> pendingVal + c
            "−"  -> pendingVal - c
            "×"  -> pendingVal * c
            "÷"  -> if (c != 0.0) pendingVal / c else pendingVal
            else -> c
        }
    }

    /** Рядок виразу для дисплею: «154 × 789 ₴» або просто «789 ₴» */
    fun displayExpr(symbol: String = "₴"): String {
        val nf = NumberFormat.getNumberInstance(Locale.getDefault())
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 2
        return if (pendingOp != null)
            "${nf.format(pendingVal)} $pendingOp $currentStr $symbol"
        else
            "$currentStr $symbol"
    }

    /** Обробляє натискання клавіші */
    fun onKey(key: String) {
        when (key) {
            "÷", "×", "−", "+" -> { pendingVal = result(); pendingOp = key; currentStr = "0" }
            "=" -> if (pendingOp != null) {
                val r = result()
                currentStr = when {
                    r == r.toLong().toDouble() -> r.toLong().toString()
                    else -> r.toBigDecimal().stripTrailingZeros().toPlainString().replace(".", ",")
                }
                pendingOp  = null
                pendingVal = 0.0
            }
            "⌫" -> currentStr = if (currentStr.length <= 1) "0" else currentStr.dropLast(1)
            "," -> {
                if ("," !in currentStr && "." !in currentStr)
                    currentStr = if (currentStr == "0") "0," else "$currentStr,"
            }
            else -> {
                val dotIdx = currentStr.indexOfFirst { it == ',' || it == '.' }
                currentStr = when {
                    dotIdx >= 0 && currentStr.length - dotIdx > 2 -> currentStr
                    currentStr == "0"      -> key
                    currentStr.length < 12 -> "$currentStr$key"
                    else                   -> currentStr
                }
            }
        }
    }
}

/** Фабрика, що зберігає стан калькулятора у Composition */
@Composable
fun rememberCalcState(initial: Double = 0.0): CalcStateHolder =
    remember { CalcStateHolder(initial) }

// ── Загальна клавіатура-калькулятор 5×4 ──────────────────────────────────────
/**
 * @param calc           стан калькулятора
 * @param modifier       модифікатор для всього блоку (задайте weight або height)
 * @param currencySymbol символ валюти (нижня ліва клавіша відображення)
 * @param confirmColor   колір кнопки «=»
 * @param onConfirm      виклик при натисканні «=»
 * @param row2ExtraKey   composable для правої клавіші 2-го рядка (null = порожньо)
 */
@Composable
fun SharedCalcKeypad(
    calc:           CalcStateHolder,
    modifier:       Modifier = Modifier,
    currencySymbol: String   = "₴",
    confirmColor:   Color    = Color(0xFF4CAF50),
    onConfirm:      () -> Unit,
    row2ExtraKey:   (@Composable RowScope.() -> Unit)? = null
) {
    val gap   = 3.dp
    val keyBg = MaterialTheme.colorScheme.surfaceVariant
    val opBg  = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier            = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Рядок 1: ÷ 7 8 9 ⌫
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            QKey("÷", Modifier.weight(1f), isOp = true, bg = opBg) { calc.onKey("÷") }
            QKey("7", Modifier.weight(1f), bg = keyBg) { calc.onKey("7") }
            QKey("8", Modifier.weight(1f), bg = keyBg) { calc.onKey("8") }
            QKey("9", Modifier.weight(1f), bg = keyBg) { calc.onKey("9") }
            Box(
                modifier         = Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp)).background(keyBg)
                    .clickable { calc.onKey("⌫") },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.Backspace, null, modifier = Modifier.size(20.dp)) }
        }
        // Рядок 2: × 4 5 6 [extraKey або Spacer]
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            QKey("×", Modifier.weight(1f), isOp = true, bg = opBg) { calc.onKey("×") }
            QKey("4", Modifier.weight(1f), bg = keyBg) { calc.onKey("4") }
            QKey("5", Modifier.weight(1f), bg = keyBg) { calc.onKey("5") }
            QKey("6", Modifier.weight(1f), bg = keyBg) { calc.onKey("6") }
            if (row2ExtraKey != null) row2ExtraKey() else Spacer(Modifier.weight(1f))
        }
        // Рядки 3+4: оператори | 1/₴ | 2/0 | 3/, | = (подвійна висота)
        Row(
            modifier              = Modifier.weight(2f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("−", Modifier.weight(1f).fillMaxWidth(), isOp = true, bg = opBg) { calc.onKey("−") }
                QKey("+", Modifier.weight(1f).fillMaxWidth(), isOp = true, bg = opBg) { calc.onKey("+") }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("1", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("1") }
                QKey(currencySymbol, Modifier.weight(1f).fillMaxWidth(), bg = keyBg) {}
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("2", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("2") }
                QKey("0", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("0") }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                QKey("3", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey("3") }
                QKey(",", Modifier.weight(1f).fillMaxWidth(), bg = keyBg) { calc.onKey(",") }
            }
            // ✓ (підтвердити) або = (обчислити вираз)
            val hasPendingOp = calc.pendingOp != null
            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(confirmColor)
                    .clickable { if (hasPendingOp) calc.onKey("=") else onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                if (hasPendingOp) {
                    Text("=", fontSize = 32.sp, fontWeight = FontWeight.Normal, color = Color.White)
                } else {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

// ── Універсальний аркуш-калькулятор для введення суми ────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountCalculatorSheet(
    initial:        Double = 0.0,
    currencySymbol: String = "₴",
    title:          String = "Сума",
    onResult:       (Double) -> Unit,
    onDismiss:      () -> Unit
) {
    val calc    = rememberCalcState(initial)
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.58f)) {

            // Дисплей виразу
            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                Text(
                    text       = calc.displayExpr(currencySymbol),
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            SharedCalcKeypad(
                calc           = calc,
                modifier       = Modifier.weight(1f).fillMaxWidth(),
                currencySymbol = currencySymbol,
                onConfirm      = { onResult(calc.result()) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
