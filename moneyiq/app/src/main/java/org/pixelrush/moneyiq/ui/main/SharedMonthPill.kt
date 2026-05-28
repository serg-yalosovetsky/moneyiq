package org.pixelrush.moneyiq.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import java.util.*

private val PILL_ACCENT = Color(0xFFD81B60)

// ── Загальна пілюля навігації по місяцю ──────────────────────────────────────

/**
 * Єдина малинова пілюля навігації.
 * При кліку — відкривається [PeriodSelectorSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMonthNavPill(
    year:        Int,
    month:       Int,     // 0-based
    daysInMonth: Int,
    onPrev:      () -> Unit,
    onNext:      () -> Unit,
    onSelectMonth: (year: Int, month: Int) -> Unit = { _, _ -> }
) {
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        PeriodSelectorSheet(
            currentYear   = year,
            currentMonth  = month,
            daysInMonth   = daysInMonth,
            onDismiss     = { showSheet = false },
            onSelectMonth = { y, m ->
                onSelectMonth(y, m)
                showSheet = false
            }
        )
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // « подвійна стрілка вліво
        IconButton(onClick = onPrev) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
            }
        }

        // Пілюля (клікабельна)
        Surface(
            shape    = RoundedCornerShape(50.dp),
            color    = PILL_ACCENT.copy(alpha = 0.12f),
            modifier = Modifier.clickable { showSheet = true }
        ) {
            Row(
                modifier              = Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = CircleShape, color = PILL_ACCENT) {
                    Text(
                        "$daysInMonth",
                        modifier   = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
                Text(
                    "${MONTH_NAMES_UA[month]} $year",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = PILL_ACCENT
                )
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint     = PILL_ACCENT,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // » подвійна стрілка вправо
        IconButton(onClick = onNext) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Period Selector Bottom Sheet ──────────────────────────────────────────────

private data class PeriodOption(
    val id:       String,
    val label:    String,
    val subLabel: String,
    val badge:    String?,          // null → icon
    val icon:     ImageVector?,
    val color:    Color = PILL_ACCENT
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelectorSheet(
    currentYear:   Int,
    currentMonth:  Int,   // 0-based
    daysInMonth:   Int,
    onDismiss:     () -> Unit,
    onSelectMonth: (year: Int, month: Int) -> Unit
) {
    val today     = Calendar.getInstance()
    val todayYear = today.get(Calendar.YEAR)
    val todayMon  = today.get(Calendar.MONTH)
    val todayDay  = today.get(Calendar.DAY_OF_MONTH)
    val yearDays  = if (todayYear % 4 == 0 && (todayYear % 100 != 0 || todayYear % 400 == 0)) 366 else 365

    val weekStart = (today.clone() as Calendar).apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }
    val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_WEEK, 6) }

    fun monthNameOf(cal: Calendar) = MONTH_NAMES_UA_FULL[cal.get(Calendar.MONTH)]

    val wkLabel = "${weekStart.get(Calendar.DAY_OF_MONTH)} ${monthNameOf(weekStart)}" +
                  " — ${weekEnd.get(Calendar.DAY_OF_MONTH)} ${monthNameOf(weekEnd)}"

    val options = listOf(
        PeriodOption("all",   "Весь час",      "від початку",                       "∞",   null,                        Color(0xFF607D8B)),
        PeriodOption("day",   "Виберіть день", "оберіть дату",                      null,  Icons.Default.CalendarMonth, Color(0xFF9E9E9E)),
        PeriodOption("week",  "Тиждень",       wkLabel,                             "7",   null,                        Color(0xFF26A69A)),
        PeriodOption("today", "Сьогодні",      "$todayDay ${MONTH_NAMES_UA_FULL[todayMon]}", "1", null,                Color(0xFF42A5F5)),
        PeriodOption("year",  "Рік",           "$todayYear",                        "$yearDays", null,                  Color(0xFFEF6C00)),
        PeriodOption("month", "Місяць",        "${MONTH_NAMES_UA_FULL[currentMonth]} $currentYear", "$daysInMonth", null, PILL_ACCENT),
    )

    // ── Sub-screen states ─────────────────────────────────────────────────────
    var showDayPicker   by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    // ── Day picker dialog (Material3) ─────────────────────────────────────────
    if (showDayPicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { ms ->
                        val cal = Calendar.getInstance().apply { timeInMillis = ms }
                        onSelectMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                        showDayPicker = false
                        onDismiss()
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDayPicker = false }) { Text("Скасувати") }
            }
        ) { DatePicker(state = dpState) }
        return
    }

    // ── Date range picker (fullscreen) ────────────────────────────────────────
    if (showRangePicker) {
        DateRangePickerFullScreen(
            onDismiss = { showRangePicker = false },
            onConfirm = { startMs, endMs ->
                val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                onSelectMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                showRangePicker = false
                onDismiss()
            }
        )
        return
    }

    // ── Main bottom sheet ─────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Заголовок
            Text(
                "Період",
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            // ── «Вибрати діапазон» ────────────────────────────────────────
            Surface(
                onClick    = { showRangePicker = true },
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape      = RoundedCornerShape(16.dp),
                color      = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⋯ бейдж
                    Box(
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PILL_ACCENT.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "···",
                            color      = PILL_ACCENT,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Вибрати діапазон",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${MONTH_NAMES_UA_FULL[currentMonth]} $currentYear",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Сітка 2×3 ────────────────────────────────────────────────
            options.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { opt ->
                        PeriodOptionCard(
                            option     = opt,
                            isSelected = opt.id == "month",   // завжди "Місяць" обраний
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                when (opt.id) {
                                    "day"    -> { showDayPicker = true }
                                    "month"  -> onSelectMonth(currentYear, currentMonth)
                                    "today"  -> onSelectMonth(todayYear, todayMon)
                                    "week"   -> onSelectMonth(todayYear, todayMon)
                                    "year"   -> onSelectMonth(todayYear, 0)
                                    "all"    -> onSelectMonth(todayYear, todayMon)
                                }
                            }
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Картка варіанту ───────────────────────────────────────────────────────────

@Composable
private fun PeriodOptionCard(
    option:     PeriodOption,
    isSelected: Boolean,
    modifier:   Modifier = Modifier,
    onClick:    () -> Unit
) {
    val bg = if (isSelected) option.color.copy(alpha = 0.10f)
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)

    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, option.color)
        else null
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верх: бейдж або іконка
            if (option.badge != null) {
                // Закруглений прямокутник (як в оригіналі)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) option.color else option.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        option.badge,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (isSelected) Color.White else option.color
                    )
                }
            } else if (option.icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(option.color.copy(alpha = if (isSelected) 1f else 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        option.icon, null,
                        tint     = if (isSelected) Color.White else option.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Низ: назва + підпис
            Column {
                Text(
                    option.label,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSelected) option.color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    option.subLabel,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }
    }
}

// ── Повноекранний вибір діапазону дат ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerFullScreen(
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long) -> Unit
) {
    val rangeState = rememberDateRangePickerState()
    val canSave    = rangeState.selectedStartDateMillis != null

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar   = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрити")
                        }
                    },
                    title  = {},
                    actions = {
                        TextButton(
                            onClick  = {
                                val s = rangeState.selectedStartDateMillis ?: return@TextButton
                                val e = rangeState.selectedEndDateMillis ?: s
                                onConfirm(s, e)
                            },
                            enabled = canSave
                        ) {
                            Text(
                                "Зберегти",
                                fontWeight = FontWeight.SemiBold,
                                color      = if (canSave) PILL_ACCENT
                                             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            DateRangePicker(
                state    = rangeState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                title    = {
                    Text(
                        "Вибрати діапазон",
                        modifier   = Modifier.padding(start = 64.dp, top = 16.dp),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                headline = null,
                showModeToggle = false
            )
        }
    }
}
