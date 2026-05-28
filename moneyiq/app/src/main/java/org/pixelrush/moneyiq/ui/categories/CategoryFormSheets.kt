package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.components.calculator.*
import org.pixelrush.moneyiq.util.suggestCategoryStyle
// ── Category Form Sheet (додавання / редагування категорії) ───────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    existing:    CategoryEntity?,
    defaultType: TransactionType = TransactionType.EXPENSE,
    onSave:      (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean) -> Unit,
    onDelete:    (() -> Unit)? = null,
    onDismiss:   () -> Unit
) {
    var name     by remember { mutableStateOf(existing?.name     ?: "") }
    var type     by remember { mutableStateOf(existing?.type     ?: defaultType) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: CATEGORY_FORM_COLORS.first()) }
    var iconKey  by remember { mutableStateOf(existing?.icon     ?: "category") }
    var budget   by remember {
        mutableStateOf(
            existing?.budgetAmount?.takeIf { it > 0.0 }
                ?.let { it.toBigDecimal().stripTrailingZeros().toPlainString() } ?: ""
        )
    }
    var period   by remember { mutableStateOf(existing?.budgetPeriod ?: "MONTHLY") }
    var archived by remember { mutableStateOf(existing?.archived ?: false) }

    var showNameDialog    by remember { mutableStateOf(false) }
    var tempName          by remember { mutableStateOf("") }
    var showIconPicker    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBudgetCalc    by remember { mutableStateOf(false) }

    // Для нової категорії — одразу відкриваємо введення назви
    LaunchedEffect(Unit) {
        if (existing == null) { tempName = ""; showNameDialog = true }
    }

    // Авто-підказка стилю при введенні назви нової категорії
    LaunchedEffect(name) {
        if (existing == null && name.length >= 3 && iconKey == "category") {
            val (sugIcon, sugColor) = suggestCategoryStyle(name, type)
            iconKey  = sugIcon
            colorHex = sugColor
        }
    }

    val catColor by remember { derivedStateOf {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    } }

    fun doSave() {
        if (name.isNotBlank()) {
            val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
            onSave(name, type, colorHex, iconKey, b, period, archived)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (existing != null) "Категорія" else "Нова категорія") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick  = ::doSave,
                            enabled  = name.isNotBlank()
                        ) {
                            Text("Зберегти", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Шапка: назва + іконка ────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(end = 76.dp)) {
                            Text(
                                "Назва",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text  = name.ifBlank { "Торкніться для введення" },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (name.isBlank())
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { tempName = name; showNameDialog = true }
                            )
                        }
                        // Велика кольорова іконка праворуч
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(catColor)
                                .clickable { showIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                categoryIconFor(iconKey), null,
                                tint     = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // ── Налаштування ─────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Налаштування",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }

                // Тип (витрати / доходи)
                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.Category, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent = { Text("Тип") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(TransactionType.EXPENSE to "Витрати", TransactionType.INCOME to "Доходи")
                                    .forEach { (t, label) ->
                                        FilterChip(
                                            selected = type == t,
                                            onClick  = { type = t },
                                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }

                // Валюта
                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.AttachMoney, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Валюта категорії") },
                        supportingContent = {
                            Text(
                                "Українська гривня – ₴",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier.clickable {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }

                // Бюджет
                item {
                    val budgetNum = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.Speed, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Бюджет") },
                        supportingContent = {
                            Text(
                                if (budgetNum > 0.0) "$budget ₴ / ${if (period == "MONTHLY") "місяць" else "тиждень"}"
                                else "Без ліміту",
                                color = if (budgetNum > 0.0) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        },
                        modifier = Modifier.clickable { showBudgetCalc = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }

                // ── Підкатегорії ─────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Підкатегорії",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Default.Add, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent = {
                            Text("Додати підкатегорію",
                                color = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable {}
                    )
                    HorizontalDivider()
                }

                // ── Архів ────────────────────────────────────────────────────
                item {
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.Archive, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Архівна категорія") },
                        trailingContent  = {
                            Switch(checked = archived, onCheckedChange = { archived = it })
                        },
                        modifier = Modifier.clickable { archived = !archived }
                    )
                    HorizontalDivider()
                }

                // ── Видалити ─────────────────────────────────────────────────
                if (onDelete != null) {
                    item {
                        ListItem(
                            leadingContent  = {
                                Icon(Icons.Default.Delete, null,
                                    tint     = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp))
                            },
                            headlineContent = {
                                Text("Видалити категорію",
                                    color = MaterialTheme.colorScheme.error)
                            },
                            modifier = Modifier.clickable { showDeleteConfirm = true }
                        )
                    }
                }
            }
        }
    }

    // ── Діалог назви ─────────────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (name.isNotBlank()) showNameDialog = false },
            title   = { Text(if (existing != null) "Назва категорії" else "Нова категорія") },
            text    = {
                OutlinedTextField(
                    value         = tempName,
                    onValueChange = { tempName = it },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Назва") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = { name = tempName; showNameDialog = false },
                    enabled  = tempName.isNotBlank()
                ) { Text("OK") }
            },
            dismissButton = {
                if (name.isNotBlank()) {
                    TextButton(onClick = { showNameDialog = false }) { Text("Скасувати") }
                }
            }
        )
    }

    // ── Пікер кольору та іконки ───────────────────────────────────────────────
    if (showIconPicker) {
        ColorIconPickerSheet(
            currentColor = colorHex,
            currentIcon  = iconKey,
            onSave       = { newColor, newIcon ->
                colorHex = newColor
                iconKey  = newIcon
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }

    // ── Видалення ─────────────────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("Видалити категорію?") },
            text    = { Text("Транзакції залишаться, але без категорії.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete?.invoke() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Скасувати") } }
        )
    }

    // ── Бюджет-калькулятор ────────────────────────────────────────────────────
    if (showBudgetCalc) {
        AmountCalculatorSheet(
            initial   = budget.replace(",", ".").toDoubleOrNull() ?: 0.0,
            title     = "Бюджет",
            onResult  = { v ->
                budget = if (v <= 0.0) "" else v.toBigDecimal().stripTrailingZeros().toPlainString()
                showBudgetCalc = false
            },
            onDismiss = { showBudgetCalc = false }
        )
    }
}

// ── Пікер кольору та іконки (BottomSheet) ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorIconPickerSheet(
    currentColor: String,
    currentIcon:  String,
    onSave:       (color: String, icon: String) -> Unit,
    onDismiss:    () -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    var selectedIcon  by remember { mutableStateOf(currentIcon) }
    val color by remember { derivedStateOf {
        try { Color(android.graphics.Color.parseColor(selectedColor)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Превʼю
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIconFor(selectedIcon), null, tint = Color.White, modifier = Modifier.size(38.dp))
            }

            // Кольори
            Text("Колір", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_FORM_COLORS.chunked(6).forEach { rowColors ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowColors.forEach { hex ->
                            val c     = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                            val isSel = hex == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp).clip(CircleShape).background(c)
                                    .then(if (isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Іконки
            Text("Іконка", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_ICONS_LIST.chunked(4).forEach { rowIcons ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowIcons.forEach { (key, icon) ->
                            val isSel = key == selectedIcon
                            Box(
                                modifier = Modifier
                                    .size(44.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) color else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null,
                                    tint     = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        repeat(4 - rowIcons.size) { Spacer(Modifier.size(44.dp)) }
                    }
                }
            }

            // Кнопка збереження
            Button(
                onClick  = { onSave(selectedColor, selectedIcon) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Застосувати")
            }
        }
    }
}

// ── Edit Categories Screen (повноекранний діалог) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoriesScreen(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    monthSpending:     Map<Long, Double>    = emptyMap(),
    monthIncome:       Map<Long, Double>    = emptyMap(),
    totalExpense:      Double               = 0.0,
    totalIncome:       Double               = 0.0,
    onSave:   (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean, existing: CategoryEntity?) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab  by remember { mutableIntStateOf(0) }
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val categories  = if (selectedTab == 0) expenseCategories.filter { !it.archived }
                      else incomeCategories.filter { !it.archived }
    val spending    = if (selectedTab == 0) monthSpending else monthIncome
    val defaultType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Редагувати категорії") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        icon     = { Icon(Icons.Outlined.ArrowCircleDown, null, modifier = Modifier.size(20.dp)) },
                        text     = { Text("Витрати", fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        icon     = { Icon(Icons.Outlined.ArrowCircleUp, null, modifier = Modifier.size(20.dp)) },
                        text     = { Text("Доходи", fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }

                CategoriesGridContent(
                    categories    = categories,
                    spending      = spending,
                    totalExpense  = totalExpense,
                    totalIncome   = totalIncome,
                    selectedTab   = selectedTab,
                    onToggleTab   = { selectedTab = if (selectedTab == 0) 1 else 0 },
                    bottomPadding = 0.dp,
                    onChipClick   = { cat -> editCategory = cat },
                    onAdd         = { showAddSheet = true }
                )
            }
        }
    }

    editCategory?.let { cat ->
        CategoryFormSheet(
            existing    = cat,
            defaultType = cat.type,
            onSave      = { name, type, color, icon, budget, period, arch ->
                onSave(name, type, color, icon, budget, period, arch, cat)
                editCategory = null
            },
            onDelete    = { onDelete(cat); editCategory = null },
            onDismiss   = { editCategory = null }
        )
    }

    if (showAddSheet) {
        CategoryFormSheet(
            existing    = null,
            defaultType = defaultType,
            onSave      = { name, type, color, icon, budget, period, _ ->
                onSave(name, type, color, icon, budget, period, false, null)
                showAddSheet = false
            },
            onDismiss   = { showAddSheet = false }
        )
    }
}
