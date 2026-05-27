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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import java.util.*

private val PILL_ACCENT = Color(0xFFD81B60)

// ── Общая пилюля с Period Selector ────────────────────────────────────────────

/**
 * Единая малиновая пилюля навигации по месяцу.
 * При клике на пилюлю — открывается [PeriodSelectorSheet].
 * Стрелки влево/вправо переключают предыдущий/следующий месяц.
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
            currentYear  = year,
            currentMonth = month,
            daysInMonth  = daysInMonth,
            onDismiss    = { showSheet = false },
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
        // « двойная стрелка влево
        IconButton(onClick = onPrev) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
            }
        }

        // Пилюля (кликабельная)
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
                // Бейдж с числом дней
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

        // » двойная стрелка вправо
        IconButton(onClick = onNext) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Period Selector Bottom Sheet ──────────────────────────────────────────────

/** Описание одной опции периода */
private data class PeriodOption(
    val id:       String,
    val label:    String,
    val subLabel: String,
    val badge:    String?,          // null → показываем иконку
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

    // Текущие даты
    val monthName = MONTH_NAMES_UA_FULL[currentMonth]
    val todayStr  = "$todayDay ${MONTH_NAMES_UA_FULL[todayMon]}"
    val weekStart = today.clone() as Calendar
    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
    val weekEnd   = weekStart.clone() as Calendar
    (weekEnd as Calendar).add(Calendar.DAY_OF_WEEK, 6)
    val wkLabel   = "${weekStart.get(Calendar.DAY_OF_MONTH)} ${MONTH_NAMES_UA_FULL[weekStart.get(Calendar.MONTH)]} — " +
                    "${weekEnd.get(Calendar.DAY_OF_MONTH)} ${MONTH_NAMES_UA_FULL[weekEnd.get(Calendar.MONTH)]}"

    val options = listOf(
        PeriodOption("all",   "Весь час",       "від початку",     "∞",  null,                       Color(0xFF607D8B)),
        PeriodOption("day",   "Виберіть день",  "оберіть дату",    null, Icons.Default.CalendarMonth, Color(0xFF9E9E9E)),
        PeriodOption("week",  "Тиждень",        wkLabel,           "7",  null,                       Color(0xFF26A69A)),
        PeriodOption("today", "Сьогодні",       todayStr,          "1",  null,                       Color(0xFF42A5F5)),
        PeriodOption("year",  "Рік",            "$todayYear",     "$yearDays", null,                Color(0xFFEF6C00)),
        PeriodOption("month", "Місяць",         "$monthName $currentYear", "$daysInMonth", null,    PILL_ACCENT),
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = MaterialTheme.colorScheme.surface,
        dragHandle        = {
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
                .padding(bottom = 24.dp)
        ) {
            // Заголовок «Період»
            Text(
                "Період",
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            // Карточка «Вибрати діапазон»
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: custom range picker */ onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Три точки
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
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Сетка 2×3 (6 опций)
            val rows = options.chunked(2)
            rows.forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { opt ->
                        PeriodOptionCard(
                            option    = opt,
                            isSelected = opt.id == "month" &&
                                         currentYear  == todayYear &&
                                         currentMonth == todayMon,
                            modifier  = Modifier.weight(1f),
                            onClick   = {
                                when (opt.id) {
                                    "month"  -> onSelectMonth(currentYear, currentMonth)
                                    "today"  -> onSelectMonth(todayYear, todayMon)
                                    "week"   -> onSelectMonth(todayYear, todayMon)
                                    "year"   -> onSelectMonth(todayYear, 0)
                                    "all"    -> onSelectMonth(todayYear, todayMon)
                                    "day"    -> onDismiss()
                                }
                            }
                        )
                    }
                    // Если нечётная пара, добавить пустую ячейку
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PeriodOptionCard(
    option:     PeriodOption,
    isSelected: Boolean,
    modifier:   Modifier = Modifier,
    onClick:    () -> Unit
) {
    val bg = if (isSelected) option.color.copy(alpha = 0.15f)
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    Card(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, option.color)
        else null
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верх: бейдж или иконка
            if (option.badge != null) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) option.color else option.color.copy(alpha = 0.18f)
                ) {
                    Text(
                        option.badge,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = if (isSelected) Color.White else option.color
                    )
                }
            } else if (option.icon != null) {
                Icon(
                    option.icon, null,
                    tint     = option.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Низ: название + подпись
            Column {
                Text(
                    option.label,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSelected) option.color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    option.subLabel,
                    style      = MaterialTheme.typography.labelSmall,
                    fontStyle  = FontStyle.Italic,
                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines   = 1
                )
            }
        }
    }
}
