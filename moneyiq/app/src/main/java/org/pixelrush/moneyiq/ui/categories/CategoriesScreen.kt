package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.formatMoney
import kotlin.math.min

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit = {},
    embeddedMode: Boolean = false,
    padding: PaddingValues = PaddingValues(),
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab   by remember { mutableIntStateOf(0) }
    var showDialog    by remember { mutableStateOf(false) }
    var editCategory  by remember { mutableStateOf<CategoryEntity?>(null) }

    val typeDefault = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
    ) {
        // ── Шапка ──────────────────────────────────────────────────────────
        CategoriesTopBar(
            expenseCount  = state.expenseCategories.size,
            incomeCount   = state.incomeCategories.size,
            onAddClick    = { showDialog = true }
        )

        // ── Вкладки: Расходы | Доходы ────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.surface,
            contentColor     = MaterialTheme.colorScheme.primary,
            divider = {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text = {
                    Text(
                        "Расходы",
                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text = {
                    Text(
                        "Доходы",
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }

        // ── Список категорий ─────────────────────────────────────────────
        val categories = if (selectedTab == 0) state.expenseCategories else state.incomeCategories
        CategoriesListTab(
            categories    = categories,
            monthSpending = state.monthSpending,
            bottomPadding = padding.calculateBottomPadding(),
            onAdd         = { showDialog = true },
            onEdit        = { editCategory = it },
            onDelete      = { viewModel.delete(it) }
        )
    }

    // ── Диалог ───────────────────────────────────────────────────────────────
    if (showDialog || editCategory != null) {
        CategoryEditDialog(
            existing    = editCategory,
            defaultType = typeDefault,
            onSave = { name, type, color, budget ->
                if (editCategory != null) {
                    viewModel.update(
                        editCategory!!.copy(
                            name = name, type = type,
                            colorHex = color, budgetAmount = budget
                        )
                    )
                } else {
                    viewModel.add(name, type, color, budget)
                }
                showDialog   = false
                editCategory = null
            },
            onDismiss = { showDialog = false; editCategory = null }
        )
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun CategoriesTopBar(
    expenseCount: Int,
    incomeCount: Int,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка-заглушка слева (симметрия со шапкой счетов)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Центр
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Категории",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                "$expenseCount расх · $incomeCount дох",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(12.dp))

        // Кнопка «+»
        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Добавить категорию",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Список категорий ──────────────────────────────────────────────────────────

@Composable
private fun CategoriesListTab(
    categories:    List<CategoryEntity>,
    monthSpending: Map<Long, Double>,
    bottomPadding: Dp,
    onAdd:   () -> Unit,
    onEdit:  (CategoryEntity) -> Unit,
    onDelete:(CategoryEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = bottomPadding + 16.dp
        )
    ) {
        items(categories) { cat ->
            CategoryListItem(
                category = cat,
                spending = monthSpending[cat.id] ?: 0.0,
                onEdit   = { onEdit(cat) },
                onDelete = { onDelete(cat) }
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            Spacer(Modifier.height(4.dp))
            AddCategoryItem(onClick = onAdd)
        }

        if (categories.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет категорий",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Элемент категории ─────────────────────────────────────────────────────────

@Composable
private fun CategoryListItem(
    category: CategoryEntity,
    spending: Double,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val hasBudget  = category.budgetAmount > 0
    val progress   = if (hasBudget) min(1f, (spending / category.budgetAmount).toFloat()) else 0f
    val overBudget = hasBudget && spending > category.budgetAmount
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Квадратная иконка
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        // Название + прогресс бюджета
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hasBudget) {
                Spacer(Modifier.height(3.dp))
                val budgetColor = if (overBudget) MaterialTheme.colorScheme.error else accentColor
                Text(
                    "${formatMoney(spending)} / ${formatMoney(category.budgetAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = budgetColor.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (overBudget) MaterialTheme.colorScheme.error else accentColor,
                    trackColor = accentColor.copy(alpha = 0.14f)
                )
            } else if (spending > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    formatMoney(spending),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }
        }

        // Меню «⋮»
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { onEdit(); showMenu = false }
                )
                if (!category.isDefault) {
                    DropdownMenuItem(
                        text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

// ── Кнопка «Добавить категорию» ───────────────────────────────────────────────

@Composable
private fun AddCategoryItem(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .categoryDashedBorder(color = dashColor, cornerRadius = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add, null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            "Добавить категорию",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ── Диалог создания/редактирования ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditDialog(
    existing:    CategoryEntity?,
    defaultType: TransactionType,
    onSave:  (String, TransactionType, String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name     by remember { mutableStateOf(existing?.name ?: "") }
    var type     by remember { mutableStateOf(existing?.type ?: defaultType) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: "#FF5722") }
    var budget   by remember {
        mutableStateOf(existing?.budgetAmount?.let {
            if (it > 0) it.toString() else ""
        } ?: "")
    }

    val colorPalette = listOf(
        "#FF5722", "#F44336", "#E91E63", "#9C27B0",
        "#673AB7", "#3F51B5", "#2196F3", "#03A9F4",
        "#009688", "#4CAF50", "#FFEB3B", "#FF9800"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing != null) "Редактировать категорию" else "Новая категория")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Название
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Category, null) }
                )

                // Расход / Доход
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        TransactionType.EXPENSE to "Расход",
                        TransactionType.INCOME  to "Доход"
                    ).forEachIndexed { i, (t, label) ->
                        SegmentedButton(
                            selected = type == t,
                            onClick  = { type = t },
                            shape    = SegmentedButtonDefaults.itemShape(i, 2),
                            label    = { Text(label) }
                        )
                    }
                }

                // Лимит бюджета
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Лимит в месяц (0 = без лимита)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) }
                )

                // Выбор цвета
                Text(
                    "Цвет",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorPalette) { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (_: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name, type, colorHex, b)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ── Пунктирная рамка (аналог AccountsScreen) ─────────────────────────────────

private fun Modifier.categoryDashedBorder(
    color: Color,
    cornerRadius: Dp,
    dashWidth: Dp = 8.dp,
    dashGap: Dp = 5.dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = this.drawBehind {
    val cr = cornerRadius.toPx()
    val sw = strokeWidth.toPx()
    val dw = dashWidth.toPx()
    val dg = dashGap.toPx()
    drawRoundRect(
        color = color,
        size  = Size(size.width, size.height),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(
            width = sw,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dw, dg), 0f)
        )
    )
}
