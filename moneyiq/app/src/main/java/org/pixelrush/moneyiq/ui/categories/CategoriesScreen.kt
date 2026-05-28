package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
import org.pixelrush.moneyiq.util.suggestCategoryStyle

// ── Розміри чипів ─────────────────────────────────────────────────────────────

private val CHIP_WIDTH           = 82.dp
private val CHIP_HEIGHT          = 124.dp
private val CHIP_CIRCLE_SIZE     = 48.dp
private val CHIP_WIDTH_COMPACT   = 70.dp
private val CHIP_HEIGHT_COMPACT  = 108.dp
private val CHIP_CIRCLE_COMPACT  = 40.dp
private val DONUT_SECTION_HEIGHT = 360.dp
private val SIDE_COLUMN_WIDTH    = 90.dp

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
    var selectedTab  by remember { mutableIntStateOf(0) }

    var quickCategory      by remember { mutableStateOf<CategoryEntity?>(null) }
    var actionCategory     by remember { mutableStateOf<CategoryEntity?>(null) }
    var editCategory       by remember { mutableStateOf<CategoryEntity?>(null) }
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showAddSheet       by remember { mutableStateOf(false) }

    // Collapse double-click expansion when toggling subcategory view
    LaunchedEffect(state.showSubcategories) {
        if (state.showSubcategories) expandedCategoryId = null
    }

    val allCategoriesForTab = (if (selectedTab == 0) state.expenseCategories else state.incomeCategories)
        .filter { !it.archived }
    val spending   = if (selectedTab == 0) state.monthSpending else state.monthIncome
    val childCounts = allCategoriesForTab
        .filter { it.parentId != null && (spending[it.id] ?: 0.0) > 0.0 }
        .groupBy { it.parentId!! }
        .mapValues { it.value.size }
    val categories = if (!state.showSubcategories) {
        allCategoriesForTab.filter { it.parentId == null }
    } else {
        allCategoriesForTab
    }

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
        SharedMonthNavPill(
            appMonth       = state.appMonth,
            daysInPeriod   = state.daysInMonth,
            onPrev         = viewModel::prevMonth,
            onNext         = viewModel::nextMonth,
            onSelectPeriod = viewModel::setPeriod
        )

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
        val catSpending = effectiveSpending[cat.id] ?: 0.0
        val catTotal    = if (cat.type == TransactionType.EXPENSE) state.totalExpense else state.totalIncome
        val catTxCount  = allCategoriesForTab
            .filter { it.id == cat.id || it.parentId == cat.id }
            .sumOf { state.monthTxCounts[it.id] ?: 0 }
        CategoryActionSheet(
            category      = cat,
            spending      = catSpending,
            txCount       = catTxCount,
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

    // ── Редагування категорії ────────────────────────────────────────────────
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

// ── Сітка: donut-чарт + чипи ─────────────────────────────────────────────────

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

    val parentColors: Map<Long, String> = if (showSubcategories) {
        val parentMap = categories.filter { it.parentId == null }.associateBy { it.id }
        categories.filter { it.parentId != null }.mapNotNull { child ->
            parentMap[child.parentId]?.let { child.id to it.colorHex }
        }.toMap()
    } else emptyMap()

    val active  = sorted.filter { (spending[it.id] ?: 0.0) > 0.0 }
    val display = if (active.isNotEmpty()) active else sorted.filter { it.parentId == null }

    // When subcategories are expanded, everything goes below the donut — no overlap possible
    val topRow   = if (!showSubcategories) display.take(4) else emptyList()
    val midLeft  = if (!showSubcategories) display.drop(4).take(2) else emptyList()
    val midRight = if (!showSubcategories) display.drop(6).take(2) else emptyList()
    val extCats  = if (!showSubcategories) display.drop(8) else display

    // Double-click expansion strip (only in collapsed mode)
    val expandedCat = if (expandedCategoryId != null && !showSubcategories)
        display.find { it.id == expandedCategoryId } else null
    val expandedChildren = expandedCat?.let {
        allCategoriesForTab.filter { c -> c.parentId == it.id && !c.archived }
    } ?: emptyList()

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

        // ── Top row: 4 чипи (тільки в згорнутому режимі) ────────────────
        item {
            if (!showSubcategories) {
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
                        Box(
                            Modifier.width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cat != null) {
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
                                    showChildBadge = true,
                                    groupColorHex  = parentColors[cat.id],
                                    isCompact      = isCompact,
                                    isExpanded     = cat.id == expandedCategoryId
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Expansion strip після topRow ─────────────────────────────────
        if (expandedCat != null && topRow.any { it.id == expandedCat.id } && expandedChildren.isNotEmpty()) {
            item(key = "strip_top_${expandedCategoryId}") {
                ExpandedCategoryStrip(
                    parent           = expandedCat,
                    children         = expandedChildren,
                    spending         = spending,
                    onClickParent    = { onChipClick(expandedCat) },
                    onClickChild     = { onChipClick(it) },
                    onLongClickChild = { onChipLongClick(it) }
                )
            }
        }

        // ── Mid row: donut повна ширина АБО [2 зліва][donut][2 справа] ──
        item {
            if (showSubcategories) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DONUT_SECTION_HEIGHT)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    DonutChart(
                        categories   = categories,
                        spending     = spending,
                        totalExpense = totalExpense,
                        totalIncome  = totalIncome,
                        selectedTab  = selectedTab,
                        onToggle     = onToggleTab,
                        modifier     = Modifier.fillMaxSize().padding(8.dp)
                    )
                }
            } else {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(DONUT_SECTION_HEIGHT)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                        .graphicsLayer { clip = false },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier              = Modifier
                            .width(SIDE_COLUMN_WIDTH).fillMaxHeight()
                            .graphicsLayer { clip = false },
                        verticalArrangement   = Arrangement.spacedBy(16.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        midLeft.forEach { cat ->
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
                                showChildBadge = true,
                                groupColorHex  = parentColors[cat.id],
                                isCompact      = isCompact,
                                isExpanded     = cat.id == expandedCategoryId
                            )
                        }
                    }

                    DonutChart(
                        categories   = categories,
                        spending     = spending,
                        totalExpense = totalExpense,
                        totalIncome  = totalIncome,
                        selectedTab  = selectedTab,
                        onToggle     = onToggleTab,
                        modifier     = Modifier.weight(1f).fillMaxHeight().padding(8.dp)
                    )

                    Column(
                        modifier              = Modifier
                            .width(SIDE_COLUMN_WIDTH).fillMaxHeight()
                            .graphicsLayer { clip = false },
                        verticalArrangement   = Arrangement.spacedBy(16.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        midRight.forEach { cat ->
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
                                showChildBadge = true,
                                groupColorHex  = parentColors[cat.id],
                                isCompact      = isCompact,
                                isExpanded     = cat.id == expandedCategoryId
                            )
                        }
                    }
                }
            }
        }

        // ── Expansion strip після mid-row ────────────────────────────────
        if (expandedCat != null && (midLeft + midRight).any { it.id == expandedCat.id } && expandedChildren.isNotEmpty()) {
            item(key = "strip_mid_${expandedCategoryId}") {
                ExpandedCategoryStrip(
                    parent           = expandedCat,
                    children         = expandedChildren,
                    spending         = spending,
                    onClickParent    = { onChipClick(expandedCat) },
                    onClickChild     = { onChipClick(it) },
                    onLongClickChild = { onChipLongClick(it) }
                )
            }
        }

        // ── Ext рядки: по 4, з expansion strip після рядка що містить expanded ──
        extCats.chunked(4).forEach { rowCats ->
            item(key = rowCats.firstOrNull()?.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) CHIP_HEIGHT_COMPACT else CHIP_HEIGHT)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top
                ) {
                    repeat(4) { i ->
                        val cat = rowCats.getOrNull(i)
                        Box(
                            Modifier.width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cat != null) {
                                CategoryChip(
                                    category       = cat,
                                    spending       = spending[cat.id] ?: 0.0,
                                    onClick        = { onChipClick(cat) },
                                    childCount     = childCounts[cat.id] ?: 0,
                                    onLongPress    = { onChipLongClick(cat) },
                                    onDoubleClick  = {
                                        if ((childCounts[cat.id] ?: 0) > 0 && !showSubcategories)
                                            onChipDoubleClick(cat.id)
                                        else onChipClick(cat)
                                    },
                                    showChildBadge = !showSubcategories,
                                    groupColorHex  = parentColors[cat.id],
                                    isCompact      = isCompact,
                                    isExpanded     = cat.id == expandedCategoryId
                                )
                            }
                        }
                    }
                }
            }
            if (expandedCat != null && rowCats.any { it.id == expandedCat.id } && expandedChildren.isNotEmpty()) {
                item(key = "strip_ext_${expandedCategoryId}") {
                    ExpandedCategoryStrip(
                        parent           = expandedCat,
                        children         = expandedChildren,
                        spending         = spending,
                        onClickParent    = { onChipClick(expandedCat) },
                        onClickChild     = { onChipClick(it) },
                        onLongClickChild = { onChipLongClick(it) }
                    )
                }
            }
        }

        // ── Кнопка «+» ──────────────────────────────────────────────────
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
    }
}

// ── Чип категорії ─────────────────────────────────────────────────────────────

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
    isCompact:      Boolean = false,
    isExpanded:     Boolean = false
) {
    val chipW      = if (isCompact) CHIP_WIDTH_COMPACT   else CHIP_WIDTH
    val chipH      = if (isCompact) CHIP_HEIGHT_COMPACT  else CHIP_HEIGHT
    val circleSize = if (isCompact) CHIP_CIRCLE_COMPACT  else CHIP_CIRCLE_SIZE
    val iconSize   = if (isCompact) 22.dp  else 26.dp
    val titleSize  = if (isCompact) 10.sp  else 11.sp
    val moneySize  = if (isCompact)  8.sp  else  9.sp
    val spendSize  = if (isCompact)  9.sp  else 10.sp

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
            .let { m ->
                when {
                    isExpanded  -> m.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f))
                    groupBg != null -> m.clip(RoundedCornerShape(12.dp)).background(groupBg)
                    else -> m
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress, onDoubleClick = onDoubleClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Назва
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 22.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                category.name,
                style      = MaterialTheme.typography.labelSmall.copy(
                    fontSize   = titleSize,
                    lineHeight = if (isCompact) 12.sp else 13.sp
                ),
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                softWrap   = true,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.fillMaxWidth()
            )
        }
        // 2. Бюджет або spacer
        if (category.budgetAmount > 0.0) {
            Text(
                formatMoney(category.budgetAmount) + " ₴",
                style      = MaterialTheme.typography.labelSmall.copy(
                    fontSize   = moneySize,
                    lineHeight = if (isCompact) 10.sp else 11.sp
                ),
                fontWeight = FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(Modifier.height(if (isCompact) 10.dp else 11.dp))
        }
        Spacer(Modifier.height(if (isCompact) 2.dp else 3.dp))
        // 3. Іконка — outer Box рисує кільце expansion поза кліпом внутрішнього кола
        val iconKey = remember(category.icon, category.name) {
            if (category.icon == "category")
                suggestCategoryStyle(category.name, category.type).first
            else
                category.icon
        }
        val hasSpending = spending > 0.0
        Box(
            modifier = Modifier
                .size(circleSize)
                .then(
                    if (isExpanded) Modifier.drawBehind {
                        drawCircle(
                            color  = color.copy(alpha = 0.45f),
                            radius = size.minDimension / 2f + 4.dp.toPx(),
                            style  = Stroke(width = 2.5.dp.toPx())
                        )
                    } else Modifier
                )
        ) {
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
                    Text(
                        "+$childCount",
                        style    = MaterialTheme.typography.labelSmall.copy(fontSize = if (isCompact) 7.sp else 8.sp),
                        color    = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(if (isCompact) 2.dp else 3.dp))
        // 4. Витрачено
        Text(
            formatMoney(spending) + " ₴",
            style      = MaterialTheme.typography.labelSmall.copy(
                fontSize   = spendSize,
                lineHeight = if (isCompact) 11.sp else 12.sp
            ),
            fontWeight = FontWeight.SemiBold,
            color      = if (spending > 0.0) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

// ── Полоса підкатегорій ───────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedCategoryStrip(
    parent:           CategoryEntity,
    children:         List<CategoryEntity>,
    spending:         Map<Long, Double>,
    onClickParent:    () -> Unit,
    onClickChild:     (CategoryEntity) -> Unit,
    onLongClickChild: (CategoryEntity) -> Unit = {}
) {
    val parentColor = remember(parent.colorHex) {
        try { Color(android.graphics.Color.parseColor(parent.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val sortedKids = children
        .filter { (spending[it.id] ?: 0.0) > 0.0 }
        .sortedByDescending { spending[it.id] ?: 0.0 }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = parentColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            sortedKids.take(4).forEach { child ->
                val childColor = remember(child.colorHex) {
                    try { Color(android.graphics.Color.parseColor(child.colorHex)) }
                    catch (_: Exception) { Color(0xFFFF5722) }
                }
                val childIconKey = if (child.icon == "category")
                    suggestCategoryStyle(child.name, child.type).first else child.icon
                val childSpend = spending[child.id] ?: 0.0

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick     = { onClickChild(child) },
                            onLongClick = { onLongClickChild(child) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (childSpend > 0) childColor else childColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIconFor(childIconKey), null,
                            tint     = if (childSpend > 0) Color.White else childColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        child.name,
                        style     = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                    Text(
                        formatMoney(childSpend) + " ₴",
                        style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.SemiBold,
                        color      = if (childSpend > 0) childColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        maxLines   = 1,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth()
                    )
                }
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
        Spacer(Modifier.height(28.dp))
        Text("", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp))
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
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines  = 1,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Donut-чарт ───────────────────────────────────────────────────────────────

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

    val tabType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
    val activeSpending = categories
        .filter { it.type == tabType && !it.archived }
        .mapNotNull { cat -> (spending[cat.id] ?: 0.0).takeIf { it > 0.0 }?.let { cat to it } }
        .sortedByDescending { it.second }

    val tabTotal = activeSpending.sumOf { it.second }

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
