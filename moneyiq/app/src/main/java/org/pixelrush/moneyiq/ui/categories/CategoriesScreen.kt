package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.formatMoney
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    embeddedMode: Boolean = false,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    @Composable
    fun CategoryBody(padding: PaddingValues) {
        Box(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Расходы") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Доходы") })
                }
                val categoryItems = if (selectedTab == 0) state.expenseCategories else state.incomeCategories
                if (categoryItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет категорий", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn { items(categoryItems) { cat ->
                        CategoryItem(
                            category = cat,
                            spending = state.monthSpending[cat.id] ?: 0.0,
                            onEdit = { editCategory = it },
                            onDelete = { viewModel.delete(it) }
                        )
                    } }
                }
            }
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Default.Add, "Добавить категорию") }
        }
        if (showDialog || editCategory != null) {
            val typeDefault = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
            CategoryDialog(
                existing = editCategory,
                defaultType = typeDefault,
                onSave = { name, type, color, budget ->
                    if (editCategory != null) viewModel.update(editCategory!!.copy(name = name, type = type, colorHex = color, budgetAmount = budget))
                    else viewModel.add(name, type, color, budget)
                    showDialog = false; editCategory = null
                },
                onDismiss = { showDialog = false; editCategory = null }
            )
        }
    }

    if (embeddedMode) {
        CategoryBody(PaddingValues(bottom = 88.dp))
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Категории") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Добавить категорию")
            }
        }
    ) { padding ->
        CategoryBody(padding)
    }
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    spending: Double = 0.0,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
    var showMenu by remember { mutableStateOf(false) }
    val hasBudget = category.budgetAmount > 0
    val progress = if (hasBudget) min(1f, (spending / category.budgetAmount).toFloat()) else 0f
    val overBudget = hasBudget && spending > category.budgetAmount

    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Category, null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        headlineContent = { Text(category.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                if (hasBudget) {
                    val budgetColor = if (overBudget) MaterialTheme.colorScheme.error else color
                    Text(
                        "${formatMoney(spending)} / ${formatMoney(category.budgetAmount)} в мес",
                        style = MaterialTheme.typography.bodySmall,
                        color = budgetColor.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = if (overBudget) MaterialTheme.colorScheme.error else color,
                        trackColor = color.copy(alpha = 0.12f)
                    )
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Редактировать") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { onEdit(category); showMenu = false }
                    )
                    if (!category.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { onDelete(category); showMenu = false }
                        )
                    }
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDialog(
    existing: CategoryEntity?,
    defaultType: TransactionType,
    onSave: (String, TransactionType, String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: defaultType) }
    var budget by remember {
        mutableStateOf(
            existing?.budgetAmount?.let { if (it > 0) it.toString() else "" } ?: ""
        )
    }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: "#FF5722") }

    val colorPalette = listOf(
        "#FF5722", "#4CAF50", "#2196F3", "#9C27B0",
        "#FF9800", "#E91E63", "#009688", "#607D8B",
        "#FFC107", "#795548"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing != null) "Редактировать категорию" else "Новая категория")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Тип транзакции
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        TransactionType.EXPENSE to "Расход",
                        TransactionType.INCOME to "Доход"
                    ).forEachIndexed { i, (t, label) ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            label = { Text(label) }
                        )
                    }
                }

                // Лимит бюджета
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Лимит в месяц (0 = без лимита)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Выбор цвета
                Text("Цвет", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorPalette.take(5).forEach { hex ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorPalette.drop(5).forEach { hex ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name, type, colorHex, b)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
