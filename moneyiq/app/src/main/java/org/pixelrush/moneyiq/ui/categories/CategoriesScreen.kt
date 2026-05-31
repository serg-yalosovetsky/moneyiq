package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.ui.budget.BudgetCatRow
import org.pixelrush.moneyiq.ui.budget.BudgetInputSheet
import org.pixelrush.moneyiq.ui.main.SharedMonthNavPill
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.ui.main.horizontalSwipe
import org.pixelrush.moneyiq.util.suggestCategoryStyle

// ── Розміри чипів ─────────────────────────────────────────────────────────────

internal val CHIP_WIDTH           = 116.dp
internal val CHIP_HEIGHT          = 136.dp
internal val CHIP_CIRCLE_SIZE     = 60.dp
internal val CHIP_WIDTH_COMPACT   = 82.dp
internal val CHIP_HEIGHT_COMPACT  = 112.dp
internal val CHIP_CIRCLE_COMPACT  = 40.dp
internal val CATEGORY_VERTICAL_GAP = 8.dp
internal val DONUT_SECTION_HEIGHT = (CHIP_HEIGHT * 2 + CATEGORY_VERTICAL_GAP)
internal val SUBCATEGORY_PANEL_WIDTH = 150.dp
internal val SUBCATEGORY_PANEL_HEIGHT = 76.dp

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
    var budgetCategory     by remember { mutableStateOf<CategoryEntity?>(null) }
    var editCategory       by remember { mutableStateOf<CategoryEntity?>(null) }
    var addSubcategoryTo   by remember { mutableStateOf<CategoryEntity?>(null) }
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
        .filter { it.parentId != null && ((spending[it.id] ?: 0.0) > 0.0 || it.budgetAmount > 0.0) }
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
    val monthLabel = "${MONTH_NAMES_UA_FULL[state.selectedMonth.month]} ${state.selectedMonth.year}"

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
            onBudget      = { actionCategory = null; budgetCategory = cat },
            onOperations  = { actionCategory = null; onViewCategoryTx(cat) },
            onDismiss     = { actionCategory = null }
        )
    }

    budgetCategory?.let { cat ->
        val catAmount = effectiveSpending[cat.id] ?: 0.0
        BudgetInputSheet(
            catRow      = BudgetCatRow(cat, catAmount),
            monthLabel  = monthLabel,
            accentColor = if (cat.type == TransactionType.INCOME) Color(0xFF26A69A) else Color(0xFFD81B60),
            amountLabel = if (cat.type == TransactionType.INCOME) "отримано" else "витрачено",
            onDismiss   = { budgetCategory = null },
            onConfirm   = { newBudget ->
                viewModel.update(cat.copy(budgetAmount = newBudget))
                budgetCategory = null
            }
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
        val catChildren = allCategoriesForTab.filter { it.parentId == cat.id }
        CategoryFormSheet(
            existing         = cat,
            children         = catChildren,
            onAddSubcategory = if (cat.parentId == null) ({ addSubcategoryTo = cat }) else null,
            defaultType      = cat.type,
            onSave           = { name, type, color, icon, budget, period, archived, currency ->
                viewModel.update(cat.copy(
                    name         = name,
                    type         = type,
                    colorHex     = color,
                    icon         = icon,
                    budgetAmount = budget,
                    budgetPeriod = period,
                    archived     = archived,
                    currencyCode = currency
                ))
                editCategory = null
            },
            onDelete  = { viewModel.delete(cat); editCategory = null },
            onDismiss = { editCategory = null }
        )
    }

    // ── Форма нової підкатегорії ─────────────────────────────────────────────
    addSubcategoryTo?.let { parent ->
        CategoryFormSheet(
            existing    = null,
            forParentId = parent.id,
            defaultType = parent.type,
            onSave      = { name, type, color, icon, budget, period, _, currency ->
                viewModel.add(name, type, color, icon, budget, period, currency, parentId = parent.id)
                addSubcategoryTo = null
            },
            onDismiss   = { addSubcategoryTo = null }
        )
    }

    // ── Форма нової категорії ────────────────────────────────────────────────
    if (showAddSheet) {
        val defaultType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
        CategoryFormSheet(
            existing    = null,
            defaultType = defaultType,
            onSave      = { name, type, color, icon, budget, period, _, currency ->
                viewModel.add(name, type, color, icon, budget, period, currency)
                showAddSheet = false
            },
            onDismiss   = { showAddSheet = false }
        )
    }
}

@Composable
private fun CategoryGridSlot(
    category:          CategoryEntity?,
    spending:          Map<Long, Double>,
    displayBudgets:    Map<Long, Double>,
    childCounts:       Map<Long, Int>,
    parentColors:      Map<Long, String>,
    expandedId:        Long?,
    showChildBadge:    Boolean,
    isCompact:         Boolean,
    inlineStripShown:  Boolean = false,
    onChipClick:       (CategoryEntity) -> Unit,
    onChipLongClick:   (CategoryEntity) -> Unit,
    onChipDoubleClick: (Long?) -> Unit
) {
    Box(
        Modifier.width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH),
        contentAlignment = Alignment.Center
    ) {
        if (category != null) {
            CategoryChip(
                category       = category,
                spending       = spending[category.id] ?: 0.0,
                onClick        = { onChipClick(category) },
                childCount     = childCounts[category.id] ?: 0,
                onLongPress    = { onChipLongClick(category) },
                onDoubleClick  = {
                    if ((childCounts[category.id] ?: 0) > 0) onChipDoubleClick(category.id)
                    else onChipClick(category)
                },
                showChildBadge = showChildBadge,
                groupColorHex  = parentColors[category.id],
                isCompact      = isCompact,
                isExpanded     = category.id == expandedId,
                budgetOverride = displayBudgets[category.id],
                flatBottom     = category.id == expandedId && inlineStripShown
            )
        }
    }
}

@Composable
private fun CategoryGridRow(
    rowCats:           List<CategoryEntity?>,
    spending:          Map<Long, Double>,
    displayBudgets:    Map<Long, Double>,
    childCounts:       Map<Long, Int>,
    parentColors:      Map<Long, String>,
    expandedId:        Long?,
    showChildBadge:    Boolean,
    isCompact:         Boolean,
    inlineStripShown:  Boolean = false,
    onChipClick:       (CategoryEntity) -> Unit,
    onChipLongClick:   (CategoryEntity) -> Unit,
    onChipDoubleClick: (Long?) -> Unit,
    modifier:          Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        repeat(4) { i ->
            CategoryGridSlot(
                category          = rowCats.getOrNull(i),
                spending          = spending,
                displayBudgets    = displayBudgets,
                childCounts       = childCounts,
                parentColors      = parentColors,
                expandedId        = expandedId,
                showChildBadge    = showChildBadge,
                isCompact         = isCompact,
                inlineStripShown  = inlineStripShown,
                onChipClick       = onChipClick,
                onChipLongClick   = onChipLongClick,
                onChipDoubleClick = onChipDoubleClick
            )
        }
    }
}

@Composable
private fun CategoryBottomActionRow(
    leftCategory:      CategoryEntity?,
    rightCategory:     CategoryEntity?,
    spending:          Map<Long, Double>,
    displayBudgets:    Map<Long, Double>,
    childCounts:       Map<Long, Int>,
    parentColors:      Map<Long, String>,
    expandedId:        Long?,
    isCompact:         Boolean,
    onChipClick:       (CategoryEntity) -> Unit,
    onChipLongClick:   (CategoryEntity) -> Unit,
    onChipDoubleClick: (Long?) -> Unit,
    onAdd:             () -> Unit,
    modifier:          Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        CategoryGridSlot(
            category       = leftCategory,
            spending       = spending,
            displayBudgets = displayBudgets,
            childCounts    = childCounts,
            parentColors   = parentColors,
            expandedId     = expandedId,
            showChildBadge = true,
            isCompact      = isCompact,
            onChipClick    = onChipClick,
            onChipLongClick = onChipLongClick,
            onChipDoubleClick = onChipDoubleClick
        )
        Box(
            Modifier.width(if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH),
            contentAlignment = Alignment.Center
        ) {
            AddCategoryChip(onClick = onAdd)
        }
        CategoryGridSlot(
            category       = rightCategory,
            spending       = spending,
            displayBudgets = displayBudgets,
            childCounts    = childCounts,
            parentColors   = parentColors,
            expandedId     = expandedId,
            showChildBadge = true,
            isCompact      = isCompact,
            onChipClick    = onChipClick,
            onChipLongClick = onChipLongClick,
            onChipDoubleClick = onChipDoubleClick
        )
    }
}

@Composable
private fun LocalSubcategoryPanel(
    parent:           CategoryEntity,
    children:         List<CategoryEntity>,
    spending:         Map<Long, Double>,
    onChipClick:      (CategoryEntity) -> Unit,
    onChipLongClick:  (CategoryEntity) -> Unit,
    modifier:         Modifier = Modifier
) {
    SideSubcategoryPanel(
        parent           = parent,
        children         = children,
        spending         = spending,
        onClickChild     = onChipClick,
        onLongClickChild = onChipLongClick,
        modifier         = modifier
            .width(SUBCATEGORY_PANEL_WIDTH)
            .heightIn(max = SUBCATEGORY_PANEL_HEIGHT)
    )
}

@Composable
private fun TopSubcategoryPanelRow(
    expandedIndex:    Int,
    parent:           CategoryEntity,
    children:         List<CategoryEntity>,
    spending:         Map<Long, Double>,
    onChipClick:      (CategoryEntity) -> Unit,
    onChipLongClick:  (CategoryEntity) -> Unit,
    modifier:         Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        repeat(4) { i ->
            Box(
                Modifier.width(if (i == expandedIndex) SUBCATEGORY_PANEL_WIDTH else CHIP_WIDTH),
                contentAlignment = Alignment.TopCenter
            ) {
                if (i == expandedIndex) {
                    LocalSubcategoryPanel(
                        parent          = parent,
                        children        = children,
                        spending        = spending,
                        onChipClick     = onChipClick,
                        onChipLongClick = onChipLongClick
                    )
                }
            }
        }
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
    val chipHeight = if (isCompact) CHIP_HEIGHT_COMPACT else CHIP_HEIGHT
    val chipW      = if (isCompact) CHIP_WIDTH_COMPACT  else CHIP_WIDTH
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

    val displayBudgets: Map<Long, Double> = if (!showSubcategories) {
        val childBudgets = allCategoriesForTab
            .filter { it.parentId != null && !it.archived && it.budgetAmount > 0.0 }
            .groupBy { it.parentId!! }
            .mapValues { (_, children) -> children.sumOf { it.budgetAmount } }
        categories.associate { cat ->
            cat.id to (cat.budgetAmount + (childBudgets[cat.id] ?: 0.0))
        }.filterValues { it > 0.0 }
    } else emptyMap()

    // All categories always shown: spending==0 chips render pale (tinted circle, colored icon)
    val display = sorted

    // Layout: top 4 | [left2 | donut | right2] | + | ext rows of 4
    val topRow   = if (!showSubcategories) display.take(4)         else emptyList()
    val midLeft  = if (!showSubcategories) display.drop(4).take(2) else emptyList()
    val midRight = if (!showSubcategories) display.drop(6).take(2) else emptyList()
    val extCats  = if (!showSubcategories) display.drop(8)         else display

    val expandedCat = if (expandedCategoryId != null && !showSubcategories)
        display.find { it.id == expandedCategoryId } else null
    val expandedChildren = expandedCat?.let { cat ->
        val parentName = cat.name.trim().lowercase()
        allCategoriesForTab.filter { c ->
            c.parentId == cat.id && !c.archived &&
            c.name.trim().lowercase() != parentName
        }
    } ?: emptyList()

    val hasExpandedStrip = expandedCat != null && expandedChildren.isNotEmpty()
    val topStripShown  = hasExpandedStrip && topRow.any  { it.id == expandedCat?.id }
    val midStripShown  = hasExpandedStrip && (midLeft + midRight).any { it.id == expandedCat?.id }

    LazyColumn(
        modifier              = Modifier.fillMaxSize(),
        contentPadding        = PaddingValues(top = 8.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement   = Arrangement.spacedBy(CATEGORY_VERTICAL_GAP)
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

        if (showSubcategories) {
            // Subcategory mode: full-width donut at top, all cats in rows below
            item(key = "donut_sub") {
                DonutChart(
                    categories   = categories,
                    spending     = spending,
                    totalExpense = totalExpense,
                    totalIncome  = totalIncome,
                    selectedTab  = selectedTab,
                    onToggle     = onToggleTab,
                    modifier     = Modifier.fillMaxWidth().height(DONUT_SECTION_HEIGHT).padding(8.dp)
                )
            }
            display.chunked(4).forEach { rowCats ->
                item(key = rowCats.firstOrNull()?.id) {
                    CategoryGridRow(
                        rowCats           = rowCats,
                        spending          = spending,
                        displayBudgets    = displayBudgets,
                        childCounts       = childCounts,
                        parentColors      = parentColors,
                        expandedId        = expandedCategoryId,
                        showChildBadge    = false,
                        isCompact         = isCompact,
                        onChipClick       = onChipClick,
                        onChipLongClick   = onChipLongClick,
                        onChipDoubleClick = onChipDoubleClick,
                        modifier          = Modifier.height(chipHeight)
                    )
                }
            }
        } else {
            // ── Top row: 4 chips ───────────────────────────────────────────
            if (topRow.isNotEmpty()) {
                item(key = "top_row") {
                    Column {
                        CategoryGridRow(
                            rowCats           = topRow,
                            spending          = spending,
                            displayBudgets    = displayBudgets,
                            childCounts       = childCounts,
                            parentColors      = parentColors,
                            expandedId        = expandedCategoryId,
                            showChildBadge    = true,
                            isCompact         = isCompact,
                            inlineStripShown  = topStripShown,
                            onChipClick       = onChipClick,
                            onChipLongClick   = onChipLongClick,
                            onChipDoubleClick = onChipDoubleClick,
                            modifier          = Modifier.height(chipHeight)
                        )
                        if (topStripShown) {
                            ExpandedCategoryStrip(
                                parent           = expandedCat,
                                children         = expandedChildren,
                                spending         = spending,
                                onClickParent    = { onChipClick(expandedCat) },
                                onClickChild     = { onChipClick(it) },
                                onLongClickChild = { onChipLongClick(it) },
                                inline           = true
                            )
                        }
                    }
                }
            }

            // ── Mid: left column | donut | right column ────────────────────
            item(key = "mid_section") {
                val chipW = if (isCompact) CHIP_WIDTH_COMPACT else CHIP_WIDTH
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier            = Modifier.width(chipW),
                            verticalArrangement = Arrangement.spacedBy(CATEGORY_VERTICAL_GAP)
                        ) {
                            midLeft.forEach { cat ->
                                Box(Modifier.height(chipHeight)) {
                                    CategoryGridSlot(
                                        category          = cat,
                                        spending          = spending,
                                        displayBudgets    = displayBudgets,
                                        childCounts       = childCounts,
                                        parentColors      = parentColors,
                                        expandedId        = expandedCategoryId,
                                        showChildBadge    = true,
                                        isCompact         = isCompact,
                                        inlineStripShown  = midStripShown,
                                        onChipClick       = onChipClick,
                                        onChipLongClick   = onChipLongClick,
                                        onChipDoubleClick = onChipDoubleClick
                                    )
                                }
                            }
                        }
                        DonutChart(
                            categories   = categories,
                            spending     = spending,
                            totalExpense = totalExpense,
                            totalIncome  = totalIncome,
                            selectedTab  = selectedTab,
                            onToggle     = onToggleTab,
                            modifier     = Modifier.weight(1f).height(DONUT_SECTION_HEIGHT).padding(4.dp),
                            onAdd        = onAdd
                        )
                        Column(
                            modifier            = Modifier.width(chipW),
                            verticalArrangement = Arrangement.spacedBy(CATEGORY_VERTICAL_GAP)
                        ) {
                            midRight.forEach { cat ->
                                Box(Modifier.height(chipHeight)) {
                                    CategoryGridSlot(
                                        category          = cat,
                                        spending          = spending,
                                        displayBudgets    = displayBudgets,
                                        childCounts       = childCounts,
                                        parentColors      = parentColors,
                                        expandedId        = expandedCategoryId,
                                        showChildBadge    = true,
                                        isCompact         = isCompact,
                                        inlineStripShown  = midStripShown,
                                        onChipClick       = onChipClick,
                                        onChipLongClick   = onChipLongClick,
                                        onChipDoubleClick = onChipDoubleClick
                                    )
                                }
                            }
                        }
                    }
                    if (midStripShown) {
                        ExpandedCategoryStrip(
                            parent           = expandedCat,
                            children         = expandedChildren,
                            spending         = spending,
                            onClickParent    = { onChipClick(expandedCat) },
                            onClickChild     = { onChipClick(it) },
                            onLongClickChild = { onChipLongClick(it) },
                            inline           = true
                        )
                    }
                }
            }

            // ── Ext cats: remaining categories in rows of 4 ────────────────
            extCats.chunked(4).forEach { rowCats ->
                val rowStripShown = hasExpandedStrip && rowCats.any { it.id == expandedCat?.id }
                item(key = rowCats.firstOrNull()?.id) {
                    Column {
                        CategoryGridRow(
                            rowCats           = rowCats,
                            spending          = spending,
                            displayBudgets    = displayBudgets,
                            childCounts       = childCounts,
                            parentColors      = parentColors,
                            expandedId        = expandedCategoryId,
                            showChildBadge    = true,
                            isCompact         = isCompact,
                            inlineStripShown  = rowStripShown,
                            onChipClick       = onChipClick,
                            onChipLongClick   = onChipLongClick,
                            onChipDoubleClick = onChipDoubleClick,
                            modifier          = Modifier.height(chipHeight)
                        )
                        if (rowStripShown) {
                            ExpandedCategoryStrip(
                                parent           = expandedCat,
                                children         = expandedChildren,
                                spending         = spending,
                                onClickParent    = { onChipClick(expandedCat) },
                                onClickChild     = { onChipClick(it) },
                                onLongClickChild = { onChipLongClick(it) },
                                inline           = true
                            )
                        }
                    }
                }
            }
        }

    }
}
