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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
import org.pixelrush.moneyiq.util.suggestCategoryStyle
import kotlin.math.cos
import kotlin.math.sin

// ── Розміри чипів ─────────────────────────────────────────────────────────────

private val CHIP_WIDTH           = 86.dp
private val CHIP_HEIGHT          = 112.dp
private val CHIP_CIRCLE_SIZE     = 48.dp
private val CHIP_WIDTH_COMPACT   = 74.dp
private val CHIP_HEIGHT_COMPACT  = 96.dp
private val CHIP_CIRCLE_COMPACT  = 40.dp
private val DONUT_SECTION_HEIGHT = 360.dp
private val SIDE_COLUMN_WIDTH    = 96.dp

// ── Кутова позиція для орбітального макету ────────────────────────────────────

private fun orbitalAngles(n: Int): List<Float> = when (n) {
    1    -> listOf(270f)
    2    -> listOf(210f, 330f)
    3    -> listOf(30f, 150f, 270f)
    else -> List(n) { i -> -90f + i * (360f / n) }   // рівномірно від верхньої точки
}

// ── Головний екран ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack:   () -> Unit    = {},
    embeddedMode:     Boolean       = false,
    padding:          PaddingValues = PaddingValues(),
    isCompact:        Boolean       = false,
    onViewCategoryTx: (CategoryEntity) -> Unit = {},
    onViewBudget:     () -> Unit    = {},
    viewModel:        CategoriesViewModel = hiltViewModel()
) {
    val state        by viewModel.state.collectAsState()
    var selectedTab  by remember { mutableIntStateOf(0) }   // 0 = Витрати, 1 = Доходи

    // Яка категорія відкрита в QuickSheet; яку додаємо; яка показує ActionSheet; яку редагуємо
    var quickCategory      by remember { mutableStateOf<CategoryEntity?>(null) }
    var actionCategory     by remember { mutableStateOf<CategoryEntity?>(null) }
    var editCategory       by remember { mutableStateOf<CategoryEntity?>(null) }
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showAddSheet       by remember { mutableStateOf(false) }

    val allCategoriesForTab = (if (selectedTab == 0) state.expenseCategories else state.incomeCategories)
        .filter { !it.archived }
    val spending   = if (selectedTab == 0) state.monthSpending else state.monthIncome
    // childCounts: підкатегорії лише з ненульовими витратами (для бейджа і умов розкриття)
    val childCounts = allCategoriesForTab
        .filter { it.parentId != null && (spending[it.id] ?: 0.0) > 0.0 }
        .groupBy { it.parentId!! }
        .mapValues { it.value.size }
    // Якщо згорнуто — показуємо лише кореневі (parentId == null)
    val categories = if (!state.showSubcategories) {
        allCategoriesForTab.filter { it.parentId == null }
    } else {
        allCategoriesForTab
    }

    // Коли підкатегорії згорнуті — підсумовуємо витрати дочірніх у батьківські
    val effectiveSpending: Map<Long, Double> = if (!state.showSubcategories) {
        val result = spending.toMutableMap()
        allCategoriesForTab.filter { it.parentId != null }.forEach { child ->
            child.parentId?.let { pid ->
                result[pid] = (result[pid] ?: 0.0) + (spending[child.id] ?: 0.0)
            }
        }
        result
    } else spending

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
            allCategoriesForTab   = allCategoriesForTab,
            spending              = effectiveSpending,
            totalExpense          = state.totalExpense,
            totalIncome           = state.totalIncome,
            selectedTab           = selectedTab,
            onToggleTab           = { selectedTab = if (selectedTab == 0) 1 else 0 },
            bottomPadding         = padding.calculateBottomPadding(),
            onChipClick           = { cat -> quickCategory = cat },
            onChipLongClick       = { cat -> actionCategory = cat },
            onChipDoubleClick     = { id ->
                expandedCategoryId = if (expandedCategoryId == id) null else id
            },
            expandedCategoryId    = expandedCategoryId,
            onAdd                 = { showAddSheet = true },
            showSubcategories     = state.showSubcategories,
            onToggleSubcategories = viewModel::toggleSubcategories,
            childCounts           = childCounts,
            isCompact             = isCompact
        )
    }

    // ── Category action sheet (long press) ───────────────────────────────────
    actionCategory?.let { cat ->
        val catSpending = (if (cat.type == TransactionType.EXPENSE) state.monthSpending else state.monthIncome)[cat.id] ?: 0.0
        val catTotal    = if (cat.type == TransactionType.EXPENSE) state.totalExpense else state.totalIncome
        CategoryActionSheet(
            category      = cat,
            spending      = catSpending,
            txCount       = state.monthTxCounts[cat.id] ?: 0,
            totalInPeriod = catTotal,
            pillLabel     = state.pillLabel,
            onEdit        = { actionCategory = null; editCategory = cat },
            onBudget      = { actionCategory = null; onViewBudget() },
            onOperations  = { actionCategory = null; onViewCategoryTx(cat) },
            onDismiss     = { actionCategory = null }
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

    // ── Редагування категорії (з ActionSheet) ────────────────────────────────
    editCategory?.let { cat ->
        CategoryFormSheet(
            existing    = cat,
            defaultType = cat.type,
            onSave      = { name, type, color, icon, budget, period, archived ->
                viewModel.update(cat.copy(
                    name         = name,
                    type         = type,
                    colorHex     = color,
                    icon         = icon,
                    budgetAmount = budget,
                    budgetPeriod = period,
                    archived     = archived
                ))
                editCategory = null
            },
            onDelete  = { viewModel.delete(cat); editCategory = null },
            onDismiss = { editCategory = null }
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
    allCategoriesForTab:   List<CategoryEntity>     = emptyList(),
    spending:              Map<Long, Double>,
    totalExpense:          Double,
    totalIncome:           Double,
    selectedTab:           Int,
    onToggleTab:           () -> Unit,
    bottomPadding:         Dp,
    onChipClick:           (CategoryEntity) -> Unit,
    onChipLongClick:       (CategoryEntity) -> Unit = {},
    onChipDoubleClick:     (Long?) -> Unit          = {},
    expandedCategoryId:    Long?                    = null,
    onAdd:                 () -> Unit,
    showSubcategories:     Boolean          = false,
    onToggleSubcategories: () -> Unit       = {},
    childCounts:           Map<Long, Int>   = emptyMap(),
    isCompact:             Boolean          = false
) {
    // Розклад по позиціях:
    //   Рядок top  (0..3): перші 4 категорії
    //   Рядок mid         : 2 зліва | donut | 2 справа
    //   Рядки ext (8+)    : по 4 у рядку
    //   «+»               : окремий рядок в самому низу

    // Коли підкатегорії розгорнуті — групуємо дочірні одразу за батьком
    val sorted: List<CategoryEntity> = if (showSubcategories) {
        val childMap = categories.filter { it.parentId != null }.groupBy { it.parentId!! }
        val roots    = categories.filter { it.parentId == null }
            .sortedByDescending { spending[it.id] ?: 0.0 }
        val orphans  = categories.filter { child ->
            child.parentId != null && categories.none { it.id == child.parentId }
        }.sortedByDescending { spending[it.id] ?: 0.0 }
        roots.flatMap { root ->
            listOf(root) + (childMap[root.id] ?: emptyList())
                .sortedByDescending { spending[it.id] ?: 0.0 }
        } + orphans
    } else {
        categories.sortedByDescending { spending[it.id] ?: 0.0 }
    }

    // Карта child.id → colorHex батька (для підсвічення фону підкатегорій)
    val parentColors: Map<Long, String> = if (showSubcategories) {
        val parentMap = categories.filter { it.parentId == null }.associateBy { it.id }
        categories.filter { it.parentId != null }.mapNotNull { child ->
            parentMap[child.parentId]?.let { child.id to it.colorHex }
        }.toMap()
    } else emptyMap()

    val active   = sorted.filter { (spending[it.id] ?: 0.0) > 0.0 }
    val inactive = sorted.filter { (spending[it.id] ?: 0.0) == 0.0 }
    // Якщо немає жодних активних — показуємо тільки кореневі категорії (без підкатегорій)
    val display  = if (active.isNotEmpty()) active else sorted.filter { it.parentId == null }
    var showInactive by remember { mutableStateOf(false) }

    val topRow   = display.take(4)
    val midLeft  = display.drop(4).take(2)
    val midRight = display.drop(6).take(2)
    val extCats  = display.drop(8)

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

        // ── Верхній рядок: 4 чипи ───────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) CHIP_HEIGHT_COMPACT else CHIP_HEIGHT)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                repeat(4) { i ->
                    val cat = topRow.getOrNull(i)
                    val isExpanded = cat != null && cat.id == expandedCategoryId
                    val chipChildren = if (isExpanded)
                        allCategoriesForTab.filter { it.parentId == cat!!.id && (spending[it.id] ?: 0.0) > 0.0 }
                    else emptyList()
                    Box(
                        Modifier
                            .width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH)
                            .zIndex(if (isExpanded) 10f else 0f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cat != null) {
                            if (isExpanded && chipChildren.isNotEmpty()) {
                                OrbitalCategoryGroup(
                                    parent       = cat,
                                    children     = chipChildren,
                                    spending     = spending,
                                    onClickParent = { onChipClick(cat) },
                                    onClickChild  = { child -> onChipClick(child) },
                                    onDoubleClick = { onChipDoubleClick(cat.id) }
                                )
                            } else {
                                CategoryChip(
                                    category       = cat,
                                    spending       = spending[cat.id] ?: 0.0,
                                    onClick        = { onChipClick(cat) },
                                    childCount     = childCounts[cat.id] ?: 0,
                                    onLongPress    = { onChipLongClick(cat) },
                                    onDoubleClick  = {
                                        if ((childCounts[cat.id] ?: 0) > 0) onChipDoubleClick(cat.id)
                                        else onChipClick(cat)
                                    },
                                    showChildBadge = !showSubcategories,
                                    groupColorHex  = parentColors[cat.id],
                                    isCompact      = isCompact
                                )
                            }
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
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .graphicsLayer { clip = false },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ліва колонка — в коридорі між краєм екрана і донатом
                Column(
                    modifier              = Modifier
                        .width(SIDE_COLUMN_WIDTH).fillMaxHeight()
                        .graphicsLayer { clip = false },
                    verticalArrangement   = Arrangement.SpaceBetween,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    midLeft.forEach { cat ->
                        val isExpanded   = cat.id == expandedCategoryId
                        val chipChildren = if (isExpanded)
                            allCategoriesForTab.filter { it.parentId == cat.id }
                        else emptyList()
                        Box(
                            Modifier.zIndex(if (isExpanded) 10f else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isExpanded && chipChildren.isNotEmpty()) {
                                OrbitalCategoryGroup(
                                    parent        = cat,
                                    children      = chipChildren,
                                    spending      = spending,
                                    onClickParent = { onChipClick(cat) },
                                    onClickChild  = { child -> onChipClick(child) },
                                    onDoubleClick = { onChipDoubleClick(cat.id) }
                                )
                            } else {
                                CategoryChip(
                                    category       = cat,
                                    spending       = spending[cat.id] ?: 0.0,
                                    onClick        = { onChipClick(cat) },
                                    childCount     = childCounts[cat.id] ?: 0,
                                    onLongPress    = { onChipLongClick(cat) },
                                    onDoubleClick  = {
                                        if ((childCounts[cat.id] ?: 0) > 0) onChipDoubleClick(cat.id)
                                        else onChipClick(cat)
                                    },
                                    showChildBadge = !showSubcategories,
                                    groupColorHex  = parentColors[cat.id],
                                    isCompact      = isCompact
                                )
                            }
                        }
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

                // Права колонка — в коридорі між донатом і краєм екрана
                Column(
                    modifier              = Modifier
                        .width(SIDE_COLUMN_WIDTH).fillMaxHeight()
                        .graphicsLayer { clip = false },
                    verticalArrangement   = Arrangement.SpaceBetween,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    midRight.forEach { cat ->
                        val isExpanded   = cat.id == expandedCategoryId
                        val chipChildren = if (isExpanded)
                            allCategoriesForTab.filter { it.parentId == cat.id }
                        else emptyList()
                        Box(
                            Modifier.zIndex(if (isExpanded) 10f else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isExpanded && chipChildren.isNotEmpty()) {
                                OrbitalCategoryGroup(
                                    parent        = cat,
                                    children      = chipChildren,
                                    spending      = spending,
                                    onClickParent = { onChipClick(cat) },
                                    onClickChild  = { child -> onChipClick(child) },
                                    onDoubleClick = { onChipDoubleClick(cat.id) }
                                )
                            } else {
                                CategoryChip(
                                    category       = cat,
                                    spending       = spending[cat.id] ?: 0.0,
                                    onClick        = { onChipClick(cat) },
                                    childCount     = childCounts[cat.id] ?: 0,
                                    onLongPress    = { onChipLongClick(cat) },
                                    onDoubleClick  = {
                                        if ((childCounts[cat.id] ?: 0) > 0) onChipDoubleClick(cat.id)
                                        else onChipClick(cat)
                                    },
                                    showChildBadge = !showSubcategories,
                                    groupColorHex  = parentColors[cat.id],
                                    isCompact      = isCompact
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Розширені рядки: по 4 ────────────────────────────────────────
        if (extCats.isNotEmpty()) {
            items(extCats.chunked(4)) { rowCats ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) CHIP_HEIGHT_COMPACT else CHIP_HEIGHT)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    repeat(4) { i ->
                        val cat = rowCats.getOrNull(i)
                        val isExpanded = cat != null && cat.id == expandedCategoryId
                        val chipChildren = if (isExpanded)
                            allCategoriesForTab.filter { it.parentId == cat!!.id && (spending[it.id] ?: 0.0) > 0.0 }
                        else emptyList()
                        Box(
                            Modifier
                                .width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH)
                                .zIndex(if (isExpanded) 10f else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cat != null) {
                                if (isExpanded && chipChildren.isNotEmpty()) {
                                    OrbitalCategoryGroup(
                                        parent        = cat,
                                        children      = chipChildren,
                                        spending      = spending,
                                        onClickParent = { onChipClick(cat) },
                                        onClickChild  = { child -> onChipClick(child) },
                                        onDoubleClick = { onChipDoubleClick(cat.id) }
                                    )
                                } else {
                                    CategoryChip(
                                        category       = cat,
                                        spending       = spending[cat.id] ?: 0.0,
                                        onClick        = { onChipClick(cat) },
                                        childCount     = childCounts[cat.id] ?: 0,
                                        onLongPress    = { onChipLongClick(cat) },
                                        onDoubleClick  = {
                                            if ((childCounts[cat.id] ?: 0) > 0) onChipDoubleClick(cat.id)
                                            else onChipClick(cat)
                                        },
                                        showChildBadge = !showSubcategories,
                                        groupColorHex  = parentColors[cat.id],
                                        isCompact      = isCompact
                                    )
                                }
                            }
                        }
                    }
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
                items(inactive.chunked(4)) { rowCats ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) CHIP_HEIGHT_COMPACT else CHIP_HEIGHT)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        repeat(4) { i ->
                            val cat = rowCats.getOrNull(i)
                            val isExpanded   = cat != null && cat.id == expandedCategoryId
                            val chipChildren = if (isExpanded)
                                allCategoriesForTab.filter { it.parentId == cat!!.id }
                            else emptyList()
                            Box(
                                Modifier
                                    .width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH)
                                    .zIndex(if (isExpanded) 10f else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cat != null) {
                                    if (isExpanded && chipChildren.isNotEmpty()) {
                                        OrbitalCategoryGroup(
                                            parent        = cat,
                                            children      = chipChildren,
                                            spending      = spending,
                                            onClickParent = { onChipClick(cat) },
                                            onClickChild  = { child -> onChipClick(child) },
                                            onDoubleClick = { onChipDoubleClick(cat.id) }
                                        )
                                    } else {
                                        CategoryChip(
                                            category       = cat,
                                            spending       = 0.0,
                                            onClick        = { onChipClick(cat) },
                                            childCount     = childCounts[cat.id] ?: 0,
                                            onLongPress    = { onChipLongClick(cat) },
                                            onDoubleClick  = {
                                                if ((childCounts[cat.id] ?: 0) > 0) onChipDoubleClick(cat.id)
                                                else onChipClick(cat)
                                            },
                                            showChildBadge = !showSubcategories,
                                            groupColorHex  = parentColors[cat.id],
                                            isCompact      = isCompact
                                        )
                                    }
                                }
                            }
                        }
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
    onDoubleClick:  () -> Unit = {},
    showChildBadge: Boolean = false,
    groupColorHex:  String? = null,
    isCompact:      Boolean = false
) {
    val chipW      = if (isCompact) CHIP_WIDTH_COMPACT   else CHIP_WIDTH
    val chipH      = if (isCompact) CHIP_HEIGHT_COMPACT  else CHIP_HEIGHT
    val circleSize = if (isCompact) CHIP_CIRCLE_COMPACT  else CHIP_CIRCLE_SIZE
    val iconSize   = if (isCompact) 22.dp  else 26.dp
    val titleSize  = if (isCompact) 10.sp  else 11.sp
    val moneySize  = if (isCompact) 9.sp   else 10.sp

    val color = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val groupBg = remember(groupColorHex) {
        groupColorHex?.let {
            try { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.13f) }
            catch (_: Exception) { null }
        }
    }

    Column(
        modifier = Modifier
            .size(width = chipW, height = chipH)
            .let { m -> if (groupBg != null) m.clip(RoundedCornerShape(12.dp)).background(groupBg) else m }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress, onDoubleClick = onDoubleClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Назва
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 20.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                category.name,
                style      = MaterialTheme.typography.bodyMedium.copy(fontSize = titleSize),
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.fillMaxWidth()
            )
        }
        // Бюджет
        Text(
            if (category.budgetAmount > 0.0) formatMoney(category.budgetAmount) + " ₴" else "0 ₴",
            style      = MaterialTheme.typography.bodySmall.copy(fontSize = moneySize),
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(if (isCompact) 2.dp else 3.dp))
        // 3. Кругла іконка
        val iconKey = remember(category.icon, category.name) {
            if (category.icon == "category")
                suggestCategoryStyle(category.name, category.type).first
            else
                category.icon
        }
        val hasSpending = spending > 0.0
        Box(modifier = Modifier.size(circleSize)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (hasSpending) color else color.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIconFor(iconKey), null,
                    tint     = if (hasSpending) Color.White else color,
                    modifier = Modifier.size(iconSize)
                )
            }
            if (showChildBadge && childCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (isCompact) 16.dp else 18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+$childCount", style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isCompact) 7.sp else 8.sp),
                        color = MaterialTheme.colorScheme.onPrimary, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(if (isCompact) 2.dp else 3.dp))
        // 4. Витрачено
        Text(
            formatMoney(spending) + " ₴",
            style      = MaterialTheme.typography.bodySmall.copy(fontSize = moneySize),
            fontWeight = FontWeight.Bold,
            color      = if (spending > 0.0) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

// ── Орбітальна група підкатегорій (подвійний клік) ───────────────────────────

private val ORBITAL_PARENT  = 50.dp
private val ORBITAL_CHILD   = 28.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrbitalCategoryGroup(
    parent:        CategoryEntity,
    children:      List<CategoryEntity>,
    spending:      Map<Long, Double>,
    onClickParent: () -> Unit,
    onClickChild:  (CategoryEntity) -> Unit,
    onDoubleClick: () -> Unit,
    modifier:      Modifier = Modifier
) {
    val parentColor = remember(parent.colorHex) {
        try { Color(android.graphics.Color.parseColor(parent.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val dashColor    = parentColor.copy(alpha = 0.45f)
    val kids         = children  // всі дочірні з витратами > 0 (фільтруються на виклику)
    val angles       = orbitalAngles(kids.size)
    // Масштабуємо радіус: для великої кількості дітей — більший радіус щоб не перекривалися
    val orbitRadius  = when {
        kids.size <= 3 -> 55.dp
        kids.size <= 5 -> 62.dp
        kids.size <= 7 -> 70.dp
        else           -> 78.dp
    }
    val orbitBoxSize = (orbitRadius.value * 2 + ORBITAL_CHILD.value + 36).dp

    Box(
        modifier = modifier
            .requiredSize(orbitBoxSize)
            .combinedClickable(onClick = {}, onDoubleClick = onDoubleClick),
        contentAlignment = Alignment.Center
    ) {
        // Пунктирне кільце
        Canvas(Modifier.matchParentSize()) {
            val r = orbitRadius.toPx()
            drawCircle(
                color  = dashColor,
                radius = r,
                style  = Stroke(
                    width      = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
                )
            )
        }

        // Батьківський чип
        val parentIconKey = if (parent.icon == "category")
            suggestCategoryStyle(parent.name, parent.type).first else parent.icon
        Box(
            modifier = Modifier
                .size(ORBITAL_PARENT)
                .clip(CircleShape)
                .background(parentColor)
                .clickable { onClickParent() },
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIconFor(parentIconKey), null, tint = Color.White, modifier = Modifier.size(26.dp))
        }

        // Підкатегорії за кутами
        kids.forEachIndexed { i, child ->
            val angleDeg = angles[i]
            val rad      = Math.toRadians(angleDeg.toDouble())
            val ox       = (orbitRadius.value * cos(rad)).dp
            val oy       = (orbitRadius.value * sin(rad)).dp
            val childColor = remember(child.colorHex) {
                try { Color(android.graphics.Color.parseColor(child.colorHex)) }
                catch (_: Exception) { Color(0xFFFF5722) }
            }
            val childIconKey = if (child.icon == "category")
                suggestCategoryStyle(child.name, child.type).first else child.icon
            val childSpend = spending[child.id] ?: 0.0

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = ox, y = oy)
                    .width(52.dp)
                    .clickable { onClickChild(child) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    child.name,
                    style     = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(ORBITAL_CHILD)
                        .clip(CircleShape)
                        .background(childColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIconFor(childIconKey), null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    formatMoney(childSpend) + " ₴",
                    style      = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    fontWeight = FontWeight.SemiBold,
                    color      = if (childSpend > 0.0) childColor else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                    maxLines   = 1,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

// ── Чип «Додати» ─────────────────────────────────────────────────────────────

@Composable
private fun AddCategoryChip(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .size(width = CHIP_WIDTH, height = CHIP_HEIGHT)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Порожні рядки для вирівнювання висоти з CategoryChip
        Spacer(Modifier.height(25.dp))
        Text("", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
        Spacer(Modifier.height(5.dp))
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
        Spacer(Modifier.height(5.dp))
        Text(
            "Додати",
            style     = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
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

        // Центр: розходи (червоний, більший) + доходи (зелений, менший)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .clickable(onClick = onToggle)
        ) {
            Text(
                if (selectedTab == 0) "Витрати" else "Доходи",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(totalExpense),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = expenseColor,
                maxLines   = 1
            )
            Text(
                formatMoney(totalIncome),
                style      = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Medium,
                color      = incomeColor,
                maxLines   = 1
            )
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
