package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import org.pixelrush.moneyiq.ui.components.calculator.*
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.util.suggestCategoryStyle

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
    val isLightBg  = catColor.luminance() > 0.5f
    val onCatColor = if (isLightBg) Color(0xFF1C1B1F) else Color.White
    val percent  = if (totalInPeriod > 0.0) (spending / totalInPeriod * 100).toInt() else 0
    val progress = if (totalInPeriod > 0.0) (spending / totalInPeriod).coerceIn(0.0, 1.0).toFloat() else 0f
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Повноекранний Dialog = скрим + кастомний шит знизу (без кліпінгу ModalBottomSheet)
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                // Кольорова шапка (іконка всередині — без виплавання за межі)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(catColor)
                        .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 20.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            category.name,
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color      = onCatColor,
                            modifier   = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(onCatColor.copy(alpha = 0.15f))
                                .border(2.dp, onCatColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                categoryIconFor(category.icon), null,
                                tint     = onCatColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            txCountLabel(txCount),
                            color = onCatColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${formatMoney(spending)} ₴",
                            color      = onCatColor,
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
                            color      = onCatColor,
                            trackColor = onCatColor.copy(alpha = 0.28f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "$percent%",
                            color      = onCatColor,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            pillLabel,
                            color = onCatColor.copy(0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${formatMoney(totalInPeriod)} ₴",
                            color      = onCatColor.copy(0.8f),
                            fontWeight = FontWeight.Medium,
                            style      = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ── Кнопки дій (білий фон) ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            start = 16.dp,
                            top = 20.dp,
                            end = 16.dp,
                            bottom = 20.dp + navigationBottom
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CatActionButton(Icons.Default.Edit,     "Редагувати", catColor, onEdit,       Modifier.weight(1f))
                    CatActionButton(Icons.Outlined.Speed,   "Бюджет",     catColor, onBudget,     Modifier.weight(1f))
                    CatActionButton(Icons.Outlined.Receipt, "Операції",   catColor, onOperations, Modifier.weight(1f))
                }
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
    val isCatLight    = catColor.luminance() > 0.5f
    val onCatColor    = if (isCatLight) Color(0xFF1C1B1F) else Color.White
    val displayColor  = if (isCatLight) Color(0xFF37474F) else catColor
    val accountColor  = Color(0xFF3949AB)  // indigo — колір панелі рахунку
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
            // ── 1. Панелі: для витрат [рахунок|категорія], для доходів [категорія|рахунок]
            Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {

                // Панель категорії (catColor bg)
                @Composable
                fun CatPanel(labelText: String, textAlign: Alignment.Horizontal, modifier: Modifier) {
                    Box(modifier = modifier.fillMaxHeight().background(catColor)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 8.dp, start = 8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(onCatColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(categoryIconFor(category.icon), null, tint = onCatColor, modifier = Modifier.size(18.dp))
                        }
                        Column(
                            modifier = Modifier
                                .align(if (textAlign == Alignment.End) Alignment.BottomEnd else Alignment.BottomStart)
                                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                            horizontalAlignment = textAlign
                        ) {
                            Text(labelText, style = MaterialTheme.typography.labelSmall, color = onCatColor.copy(alpha = 0.7f))
                            Text(category.name, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = onCatColor,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Панель рахунку (accountColor bg, тапається)
                @Composable
                fun AccPanel(labelText: String, textAlign: Alignment.Horizontal, modifier: Modifier) {
                    Box(modifier = modifier.fillMaxHeight().background(accountColor)
                        .clickable { if (accounts.size > 1) showAccSheet = true }
                    ) {
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
                        Column(
                            modifier = Modifier
                                .align(if (textAlign == Alignment.End) Alignment.BottomEnd else Alignment.BottomStart)
                                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                            horizontalAlignment = textAlign
                        ) {
                            Text(labelText, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(selectedAccount?.name ?: "—", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (isIncome) {
                    CatPanel("З категорії", Alignment.Start, Modifier.weight(1f))
                    AccPanel("На рахунок",  Alignment.End,   Modifier.weight(1f))
                } else {
                    AccPanel("З рахунку",   Alignment.Start, Modifier.weight(1f))
                    CatPanel("До категорії", Alignment.End,  Modifier.weight(1f))
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
                    color = displayColor
                )
                Text(
                    text       = displayText,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = displayColor,
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
