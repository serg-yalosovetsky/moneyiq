package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe

// ── Розміри чипів ─────────────────────────────────────────────────────────────

private val CHIP_WIDTH          = 68.dp
private val CHIP_CIRCLE_SIZE    = 44.dp
private val DONUT_SECTION_HEIGHT = 248.dp

// ── Головний екран ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit    = {},
    embeddedMode:   Boolean       = false,
    padding:        PaddingValues = PaddingValues(),
    viewModel:      CategoriesViewModel = hiltViewModel()
) {
    val state        by viewModel.state.collectAsState()
    var selectedTab  by remember { mutableIntStateOf(0) }   // 0 = Витрати, 1 = Доходи

    // Яка категорія відкрита в QuickSheet; яку додаємо
    var quickCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet  by remember { mutableStateOf(false) }

    val allCategoriesForTab = (if (selectedTab == 0) state.expenseCategories else state.incomeCategories)
        .filter { !it.archived }
    // childCounts: скільки підкатегорій має кожна коренева категорія
    val childCounts = allCategoriesForTab
        .filter { it.parentId != null }
        .groupBy { it.parentId!! }
        .mapValues { it.value.size }
    // Якщо згорнуто — показуємо лише кореневі (parentId == null)
    val categories = if (!state.showSubcategories) {
        allCategoriesForTab.filter { it.parentId == null }
    } else {
        allCategoriesForTab
    }
    val spending   = if (selectedTab == 0) state.monthSpending else state.monthIncome

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
            .horizontalSwipe(
                onSwipeLeft  = viewModel::nextMonth,
                onSwipeRight = viewModel::prevMonth
            )
    ) {
        // Навігатор місяців
        SharedMonthNavPill(
            appMonth       = state.appMonth,
            daysInPeriod   = state.daysInMonth,
            onPrev         = viewModel::prevMonth,
            onNext         = viewModel::nextMonth,
            onSelectPeriod = viewModel::setPeriod
        )

        // Сітка категорій (без TabRow — donut є перемикачем)
        CategoriesGridContent(
            categories            = categories,
            spending              = spending,
            totalExpense          = state.totalExpense,
            totalIncome           = state.totalIncome,
            selectedTab           = selectedTab,
            onToggleTab           = { selectedTab = if (selectedTab == 0) 1 else 0 },
            bottomPadding         = padding.calculateBottomPadding(),
            onChipClick           = { cat -> quickCategory = cat },
            onAdd                 = { showAddSheet = true },
            showSubcategories     = state.showSubcategories,
            onToggleSubcategories = viewModel::toggleSubcategories,
            childCounts           = childCounts
        )
    }

    // ── Quick expense / income sheet ─────────────────────────────────────────
    quickCategory?.let { cat ->
        QuickExpenseSheet(
            category  = cat,
            accounts  = state.accounts,
            onSave    = { accountId, amount, note, date ->
                viewModel.recordTransaction(accountId, cat, amount, note, date)
                quickCategory = null
            },
            onDismiss = { quickCategory = null }
        )
    }

    // ── Форма нової категорії ────────────────────────────────────────────────
    if (showAddSheet) {
        val defaultType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
        CategoryFormSheet(
            existing    = null,
            defaultType = defaultType,
            onSave      = { name, type, color, icon, budget, period, _ ->
                viewModel.add(name, type, color, icon, budget, period)
                showAddSheet = false
            },
            onDismiss   = { showAddSheet = false }
        )
    }
}

// ── Сітка: доnat-чарт + чипи ─────────────────────────────────────────────────

@Composable
internal fun CategoriesGridContent(
    categories:            List<CategoryEntity>,
    spending:              Map<Long, Double>,
    totalExpense:          Double,
    totalIncome:           Double,
    selectedTab:           Int,
    onToggleTab:           () -> Unit,
    bottomPadding:         Dp,
    onChipClick:           (CategoryEntity) -> Unit,
    onAdd:                 () -> Unit,
    showSubcategories:     Boolean          = false,
    onToggleSubcategories: () -> Unit       = {},
    childCounts:           Map<Long, Int>   = emptyMap()
) {
    // Розклад по позиціях:
    //   Рядок top  (0..4): перші 5 категорій
    //   Рядок mid         : 2 зліва | donut | 2 справа
    //   Рядки ext (9+)    : по 5 у рядку
    //   «+»               : окремий рядок в самому низу

    val sorted   = categories.sortedByDescending { spending[it.id] ?: 0.0 }
    val active   = sorted.filter { (spending[it.id] ?: 0.0) > 0.0 }
    val inactive = sorted.filter { (spending[it.id] ?: 0.0) == 0.0 }
    // Якщо немає жодних активних — показуємо всі (нова установка / новий місяць)
    val display  = if (active.isNotEmpty()) active else sorted
    var showInactive by remember { mutableStateOf(false) }

    val topRow   = display.take(5)
    val midLeft  = display.drop(5).take(2)
    val midRight = display.drop(7).take(2)
    val extCats  = display.drop(9)

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 16.dp)
    ) {
        // ── Порожній стан ────────────────────────────────────────────────
        if (categories.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Category, null,
                            modifier = Modifier.size(56.dp),
                            tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Немає категорій",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Натисніть + щоб додати",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ── Верхній рядок: 5 чипів ───────────────────────────────────────
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(5) { i ->
                    val cat = topRow.getOrNull(i)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (cat != null) {
                            CategoryChip(
                                category       = cat,
                                spending       = spending[cat.id] ?: 0.0,
                                onClick        = { onChipClick(cat) },
                                childCount     = childCounts[cat.id] ?: 0,
                                onLongPress    = onToggleSubcategories,
                                showChildBadge = !showSubcategories
                            )
                        }
                    }
                }
            }
        }

        // ── Середній рядок: [2 зліва] | [donut] | [2 справа] ────────────
        item {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(DONUT_SECTION_HEIGHT)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ліва колонка — притиснута до лівого краю екрану
                Column(
                    modifier              = Modifier.width(88.dp).fillMaxHeight(),
                    verticalArrangement   = Arrangement.SpaceEvenly,
                    horizontalAlignment   = Alignment.Start
                ) {
                    midLeft.forEach { cat ->
                        CategoryChip(
                            category       = cat,
                            spending       = spending[cat.id] ?: 0.0,
                            onClick        = { onChipClick(cat) },
                            childCount     = childCounts[cat.id] ?: 0,
                            onLongPress    = onToggleSubcategories,
                            showChildBadge = !showSubcategories
                        )
                    }
                }

                // Donut-чарт (центр — кнопка перемикання)
                DonutChart(
                    categories   = categories,
                    spending     = spending,
                    totalExpense = totalExpense,
                    totalIncome  = totalIncome,
                    selectedTab  = selectedTab,
                    onToggle     = onToggleTab,
                    modifier     = Modifier.weight(1f).fillMaxHeight().padding(8.dp)
                )

                // Права колонка — притиснута до правого краю екрану
                Column(
                    modifier              = Modifier.width(88.dp).fillMaxHeight(),
                    verticalArrangement   = Arrangement.SpaceEvenly,
                    horizontalAlignment   = Alignment.End
                ) {
                    midRight.forEach { cat ->
                        CategoryChip(
                            category       = cat,
                            spending       = spending[cat.id] ?: 0.0,
                            onClick        = { onChipClick(cat) },
                            childCount     = childCounts[cat.id] ?: 0,
                            onLongPress    = onToggleSubcategories,
                            showChildBadge = !showSubcategories
                        )
                    }
                }
            }
        }

        // ── Розширені рядки: по 5 ────────────────────────────────────────
        if (extCats.isNotEmpty()) {
            items(extCats.chunked(5)) { rowCats ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowCats.forEach { cat ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CategoryChip(
                                category       = cat,
                                spending       = spending[cat.id] ?: 0.0,
                                onClick        = { onChipClick(cat) },
                                childCount     = childCounts[cat.id] ?: 0,
                                onLongPress    = onToggleSubcategories,
                                showChildBadge = !showSubcategories
                            )
                        }
                    }
                    repeat(5 - rowCats.size) { Box(Modifier.weight(1f)) {} }
                }
            }
        }

        // ── Кнопка «+» — в самому низу списку категорій ──────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                AddCategoryChip(onClick = onAdd)
            }
        }

        // ── Кнопка розгортання/згортання підкатегорій ────────────────────
        if (childCounts.isNotEmpty()) {
            item {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication        = null
                        ) { onToggleSubcategories() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (showSubcategories) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (showSubcategories) "Згорнути підкатегорії" else "Розгорнути підкатегорії",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // ── Приховані категорії (0 витрат) ───────────────────────────────
        if (active.isNotEmpty() && inactive.isNotEmpty()) {
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .clickable { showInactive = !showInactive }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "${inactive.size} категор. (0 ₴)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (showInactive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            if (showInactive) {
                items(inactive.chunked(5)) { rowCats ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowCats.forEach { cat ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                CategoryChip(
                                    category       = cat,
                                    spending       = 0.0,
                                    onClick        = { onChipClick(cat) },
                                    childCount     = childCounts[cat.id] ?: 0,
                                    onLongPress    = onToggleSubcategories,
                                    showChildBadge = !showSubcategories
                                )
                            }
                        }
                        repeat(5 - rowCats.size) { Box(Modifier.weight(1f)) {} }
                    }
                }
            }
        }
    }
}

// ── Чип категорії: назва / бюджет / коло / витрачено ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChip(
    category:       CategoryEntity,
    spending:       Double,
    onClick:        () -> Unit,
    childCount:     Int     = 0,
    onLongPress:    () -> Unit = {},
    showChildBadge: Boolean = false
) {
    val color = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }

    Column(
        modifier = Modifier
            .width(CHIP_WIDTH)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Назва
        Text(
            category.name,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurface,
            modifier  = Modifier.fillMaxWidth()
        )
        // 2. Бюджет
        Text(
            if (category.budgetAmount > 0.0) formatMoney(category.budgetAmount) + " ₴"
            else "0 ₴",
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines  = 1,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(3.dp))
        // 3. Кругла іконка з опціональним бейджем підкатегорій
        Box(modifier = Modifier.size(CHIP_CIRCLE_SIZE)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIconFor(category.icon), null,
                    tint     = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (showChildBadge && childCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+$childCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        // 4. Витрачено цього місяця
        Text(
            formatMoney(spending) + " ₴",
            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.SemiBold,
            color      = if (spending > 0.0) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines   = 1,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

// ── Чип «Додати» ─────────────────────────────────────────────────────────────

@Composable
private fun AddCategoryChip(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .width(CHIP_WIDTH)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Порожні рядки для вирівнювання висоти з CategoryChip
        Text("", style = MaterialTheme.typography.labelSmall)
        Text("", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(CHIP_CIRCLE_SIZE)
                .clip(CircleShape)
                .dashedCircleBorder(color = dashColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add, null,
                tint     = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "Додати",
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines  = 1,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Donut-чарт з кнопкою перемикання в центрі ────────────────────────────────

@Composable
private fun DonutChart(
    categories:   List<CategoryEntity>,
    spending:     Map<Long, Double>,
    totalExpense: Double,
    totalIncome:  Double,
    selectedTab:  Int,
    onToggle:     () -> Unit,
    modifier:     Modifier = Modifier
) {
    val emptyColor   = MaterialTheme.colorScheme.surfaceVariant
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor  = Color(0xFF26A69A)

    // Категорії поточного таба з ненульовими витратами, відсортовані за сумою
    val tabType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
    val activeSpending = categories
        .filter { it.type == tabType && !it.archived }
        .mapNotNull { cat -> (spending[cat.id] ?: 0.0).takeIf { it > 0.0 }?.let { cat to it } }
        .sortedByDescending { it.second }

    val tabTotal = activeSpending.sumOf { it.second }

    // Розбираємо кольори категорій
    val categoryColors = activeSpending.map { (cat, _) ->
        try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val sw     = minDim * 0.09f
            val inset  = sw / 2f
            val arcDim = minDim - sw
            val arcSz  = Size(arcDim, arcDim)
            val tl     = Offset(
                x = (size.width  - minDim) / 2f + inset,
                y = (size.height - minDim) / 2f + inset
            )

            if (tabTotal == 0.0) {
                drawArc(
                    color      = emptyColor,
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter  = false, topLeft = tl, size = arcSz,
                    style      = Stroke(width = sw)
                )
            } else {
                var startAngle = -90f
                activeSpending.forEachIndexed { idx, (_, amount) ->
                    val sweep = (amount / tabTotal * 360.0).toFloat()
                    drawArc(
                        color      = categoryColors[idx],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter  = false, topLeft = tl, size = arcSz,
                        style      = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }

        // Центр — кнопка перемикання між Витрати / Доходи
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .clickable(onClick = onToggle)
        ) {
            val labelColor = if (selectedTab == 0) expenseColor else incomeColor
            val amount     = if (selectedTab == 0) totalExpense else totalIncome

            Text(
                if (selectedTab == 0) "Витрати" else "Доходи",
                style  = MaterialTheme.typography.labelSmall,
                color  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(amount),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = labelColor,
                maxLines   = 1
            )
            // Підказка-стрілка для натискання
            Icon(
                if (selectedTab == 0) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = "Переключити",
                modifier = Modifier.size(14.dp),
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

// ── Пунктирна кругла рамка ────────────────────────────────────────────────────

private fun Modifier.dashedCircleBorder(
    color:       Color,
    dashWidth:   Dp = 8.dp,
    dashGap:     Dp = 5.dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = this.drawBehind {
    val sw = strokeWidth.toPx()
    drawCircle(
        color  = color,
        radius = (size.minDimension - sw) / 2f,
        style  = Stroke(
            width      = sw,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashWidth.toPx(), dashGap.toPx()), 0f
            )
        )
    )
}
