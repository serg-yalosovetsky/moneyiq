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

internal val CHIP_WIDTH           = 82.dp
internal val CHIP_HEIGHT          = 132.dp
internal val CHIP_CIRCLE_SIZE     = 48.dp
internal val CHIP_WIDTH_COMPACT   = 70.dp
internal val CHIP_HEIGHT_COMPACT  = 116.dp
internal val CHIP_CIRCLE_COMPACT  = 40.dp
internal val DONUT_SECTION_HEIGHT = 360.dp
internal val SIDE_COLUMN_WIDTH    = 90.dp

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

    // All categories always shown: spending==0 chips render pale (tinted circle, colored icon)
    val display = sorted

    // Top 4 chips above the donut; all remaining chips in rows below the donut
    val topRow  = if (!showSubcategories) display.take(4) else emptyList()
    val extCats = if (!showSubcategories) display.drop(4) else display

    // Double-click expansion strip (only in collapsed mode)
    val expandedCat = if (expandedCategoryId != null && !showSubcategories)
        display.find { it.id == expandedCategoryId } else null
    val expandedChildren = expandedCat?.let { cat ->
        val parentName = cat.name.trim().lowercase()
        allCategoriesForTab.filter { c ->
            c.parentId == cat.id && !c.archived &&
            c.name.trim().lowercase() != parentName  // skip same-name children
        }
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

        // ── Mid row: donut на повну ширину ──────────────────────────────
        item {
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
        }
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
