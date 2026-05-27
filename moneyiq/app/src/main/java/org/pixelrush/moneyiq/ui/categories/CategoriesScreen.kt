package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

// ── Розміри чипів ─────────────────────────────────────────────────────────────

private val CHIP_WIDTH          = 76.dp
private val CHIP_CIRCLE_SIZE    = 50.dp
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

    val categories = (if (selectedTab == 0) state.expenseCategories else state.incomeCategories)
        .filter { !it.archived }
    val spending   = if (selectedTab == 0) state.monthSpending else state.monthIncome

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
    ) {
        // Навігатор місяців
        SharedMonthNavPill(
            year          = state.selectedMonth.year,
            month         = state.selectedMonth.month,
            daysInMonth   = state.daysInMonth,
            onPrev        = viewModel::prevMonth,
            onNext        = viewModel::nextMonth,
            onSelectMonth = viewModel::goToMonth
        )

        // Сітка категорій (без TabRow — donut є перемикачем)
        CategoriesGridContent(
            categories    = categories,
            spending      = spending,
            totalExpense  = state.totalExpense,
            totalIncome   = state.totalIncome,
            selectedTab   = selectedTab,
            onToggleTab   = { selectedTab = if (selectedTab == 0) 1 else 0 },
            bottomPadding = padding.calculateBottomPadding(),
            onChipClick   = { cat -> quickCategory = cat },
            onAdd         = { showAddSheet = true }
        )
    }

    // ── Quick expense / income sheet ─────────────────────────────────────────
    quickCategory?.let { cat ->
        QuickExpenseSheet(
            category  = cat,
            accounts  = state.accounts,
            onSave    = { accountId, amount, note ->
                viewModel.recordTransaction(accountId, cat, amount, note)
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
private fun CategoriesGridContent(
    categories:    List<CategoryEntity>,
    spending:      Map<Long, Double>,
    totalExpense:  Double,
    totalIncome:   Double,
    selectedTab:   Int,
    onToggleTab:   () -> Unit,
    bottomPadding: Dp,
    onChipClick:   (CategoryEntity) -> Unit,
    onAdd:         () -> Unit
) {
    // Розклад по позиціях:
    //   Рядок top  (0..3): перші 4 категорії
    //   Рядок mid         : 2 зліва | donut | 2 справа
    //   Рядок bot         : «+» + наступні 3
    //   Рядки ext (11+)   : по 4 у рядку

    val topRow   = categories.take(4)
    val midLeft  = categories.drop(4).take(2)
    val midRight = categories.drop(6).take(2)
    val botFirst = categories.drop(8).take(3)
    val extCats  = categories.drop(11)

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

        // ── Верхній рядок: 4 чипи ────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0 until 4) {
                    val cat = topRow.getOrNull(i)
                    if (cat != null) {
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onChipClick(cat) }
                        )
                    } else {
                        Spacer(Modifier.width(CHIP_WIDTH))
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
                // Ліва колонка
                Column(
                    modifier              = Modifier.width(CHIP_WIDTH + 4.dp).fillMaxHeight(),
                    verticalArrangement   = Arrangement.SpaceEvenly,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    midLeft.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onChipClick(cat) }
                        )
                    }
                }

                // Donut-чарт (центр — кнопка перемикання)
                DonutChart(
                    totalExpense = totalExpense,
                    totalIncome  = totalIncome,
                    selectedTab  = selectedTab,
                    onToggle     = onToggleTab,
                    modifier     = Modifier.weight(1f).fillMaxHeight().padding(8.dp)
                )

                // Права колонка
                Column(
                    modifier              = Modifier.width(CHIP_WIDTH + 4.dp).fillMaxHeight(),
                    verticalArrangement   = Arrangement.SpaceEvenly,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    midRight.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onChipClick(cat) }
                        )
                    }
                }
            }
        }

        // ── Нижній рядок: «+» + 3 категорії ─────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AddCategoryChip(onClick = onAdd)
                for (i in 0 until 3) {
                    val cat = botFirst.getOrNull(i)
                    if (cat != null) {
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onChipClick(cat) }
                        )
                    } else {
                        Spacer(Modifier.width(CHIP_WIDTH))
                    }
                }
            }
        }

        // ── Розширені рядки: по 4 ────────────────────────────────────────
        if (extCats.isNotEmpty()) {
            items(extCats.chunked(4)) { rowCats ->
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCats.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onChipClick(cat) }
                        )
                    }
                    repeat(4 - rowCats.size) { Spacer(Modifier.width(CHIP_WIDTH)) }
                }
            }
        }
    }
}

// ── Чип категорії: назва / бюджет / коло / витрачено ─────────────────────────

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    spending: Double,
    onClick:  () -> Unit
) {
    val color = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }

    Column(
        modifier = Modifier
            .width(CHIP_WIDTH)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Назва
        Text(
            category.name,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
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
        // 3. Кругла іконка
        Box(
            modifier = Modifier
                .size(CHIP_CIRCLE_SIZE)
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
    totalExpense: Double,
    totalIncome:  Double,
    selectedTab:  Int,
    onToggle:     () -> Unit,
    modifier:     Modifier = Modifier
) {
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor  = Color(0xFF26A69A)
    val emptyColor   = MaterialTheme.colorScheme.surfaceVariant

    val total    = totalExpense + totalIncome
    val expAngle = if (total > 0.0) (totalExpense / total * 360.0).toFloat() else 0f
    val incAngle = if (total > 0.0) (totalIncome  / total * 360.0).toFloat() else 0f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw    = size.minDimension * 0.15f
            val inset = sw / 2f
            val arcSz = Size(size.width - sw, size.height - sw)
            val tl    = Offset(inset, inset)

            if (total == 0.0) {
                drawArc(
                    color      = emptyColor,
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter  = false, topLeft = tl, size = arcSz,
                    style      = Stroke(width = sw)
                )
            } else {
                if (expAngle > 0f) {
                    drawArc(
                        color      = expenseColor,
                        startAngle = -90f, sweepAngle = expAngle,
                        useCenter  = false, topLeft = tl, size = arcSz,
                        style      = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
                }
                if (incAngle > 0f) {
                    drawArc(
                        color      = incomeColor,
                        startAngle = -90f + expAngle, sweepAngle = incAngle,
                        useCenter  = false, topLeft = tl, size = arcSz,
                        style      = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
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
