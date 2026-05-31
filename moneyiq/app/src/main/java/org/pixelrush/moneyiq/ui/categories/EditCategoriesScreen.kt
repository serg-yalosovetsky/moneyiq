package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoriesScreen(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    monthSpending:     Map<Long, Double>,
    monthIncome:       Map<Long, Double>,
    totalExpense:      Double,
    totalIncome:       Double,
    onSave:            (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean, currency: String, existing: CategoryEntity?) -> Unit,
    onAddSubcategory:  (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, currency: String, parentId: Long) -> Unit,
    onDelete:          (CategoryEntity) -> Unit,
    onDismiss:         () -> Unit
) {
    var selectedTab       by remember { mutableIntStateOf(0) }
    var showSubcategories by remember { mutableStateOf(false) }
    var editCategory      by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet      by remember { mutableStateOf(false) }
    var addSubcategoryFor by remember { mutableStateOf<CategoryEntity?>(null) }

    val allCategoriesForTab = (if (selectedTab == 0) expenseCategories else incomeCategories)
        .filter { !it.archived }
    val rawSpending = if (selectedTab == 0) monthSpending else monthIncome

    val effectiveSpending: Map<Long, Double> = run {
        val result = rawSpending.toMutableMap()
        allCategoriesForTab.filter { it.parentId != null }.forEach { child ->
            child.parentId?.let { pid ->
                result[pid] = (result[pid] ?: 0.0) + (rawSpending[child.id] ?: 0.0)
            }
        }
        result
    }

    val categories = if (!showSubcategories)
        allCategoriesForTab.filter { it.parentId == null }
    else
        allCategoriesForTab

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    "Редагувати категорії",
                    style    = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
                TextButton(onClick = { showSubcategories = !showSubcategories }) {
                    Text("Субкатегорії")
                }
            }

            // ── Tab row ───────────────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Outlined.ArrowCircleDown, contentDescription = null) },
                    text     = { Text("Витрати") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null) },
                    text     = { Text("Доходи") }
                )
            }

            // ── Grid (same layout as categories, badges hidden) ───────────────
            CategoriesGridContent(
                categories            = categories,
                allCategoriesForTab   = allCategoriesForTab,
                spending              = effectiveSpending,
                totalExpense          = totalExpense,
                totalIncome           = totalIncome,
                selectedTab           = selectedTab,
                onToggleTab           = { selectedTab = if (selectedTab == 0) 1 else 0 },
                bottomPadding         = 0.dp,
                onChipClick           = { cat -> editCategory = cat },
                onChipLongClick       = { cat -> editCategory = cat },
                onAdd                 = { showAddSheet = true },
                showSubcategories     = showSubcategories,
                onToggleSubcategories = { showSubcategories = !showSubcategories },
                childCounts           = emptyMap()  // no +N badges in edit mode
            )
        }
    }

    // ── Edit existing category ────────────────────────────────────────────────
    editCategory?.let { cat ->
        val children = allCategoriesForTab.filter { it.parentId == cat.id && !it.archived }
        CategoryFormSheet(
            existing             = cat,
            children             = children,
            defaultType          = cat.type,
            onAddSubcategory     = if (cat.parentId == null) ({
                editCategory = null
                addSubcategoryFor = cat
            }) else null,
            onDetachSubcategory  = if (cat.parentId == null) ({ child ->
                onSave(child.name, child.type, child.colorHex, child.icon,
                       child.budgetAmount, child.budgetPeriod, child.archived,
                       child.currencyCode, child.copy(parentId = null))
            }) else null,
            onSave    = { name, type, color, icon, budget, period, archived, currency ->
                onSave(name, type, color, icon, budget, period, archived, currency, cat)
                editCategory = null
            },
            onDelete  = { onDelete(cat); editCategory = null },
            onDismiss = { editCategory = null }
        )
    }

    // ── Add new top-level category ────────────────────────────────────────────
    if (showAddSheet) {
        val defaultType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
        CategoryFormSheet(
            existing    = null,
            defaultType = defaultType,
            onSave      = { name, type, color, icon, budget, period, _, currency ->
                onSave(name, type, color, icon, budget, period, false, currency, null)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // ── Add subcategory under a parent ────────────────────────────────────────
    addSubcategoryFor?.let { parent ->
        CategoryFormSheet(
            existing    = null,
            forParentId = parent.id,
            defaultType = parent.type,
            onSave      = { name, type, color, icon, budget, period, _, currency ->
                onAddSubcategory(name, type, color, icon, budget, period, currency, parent.id)
                addSubcategoryFor = null
            },
            onDismiss = { addSubcategoryFor = null }
        )
    }
}
