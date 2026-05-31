package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.components.calculator.*
import org.pixelrush.moneyiq.ui.components.dialogs.ConfirmationDialog
import org.pixelrush.moneyiq.ui.settings.CurrencyPageContent
import org.pixelrush.moneyiq.ui.settings.data.CURRENCIES_CRYPTO
import org.pixelrush.moneyiq.ui.settings.data.CURRENCIES_MAIN
import org.pixelrush.moneyiq.ui.settings.data.CURRENCIES_OTHER
import org.pixelrush.moneyiq.util.suggestCategoryStyle
// ── Category Form Sheet (додавання / редагування категорії) ───────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    existing:         CategoryEntity?,
    forParentId:      Long?                 = null,
    children:         List<CategoryEntity>  = emptyList(),
    onAddSubcategory: (() -> Unit)?         = null,
    defaultType:      TransactionType       = TransactionType.EXPENSE,
    onSave:           (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean, currencyCode: String) -> Unit,
    onDelete:         (() -> Unit)?         = null,
    onDismiss:        () -> Unit
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
    var period        by remember { mutableStateOf(existing?.budgetPeriod ?: "MONTHLY") }
    var archived      by remember { mutableStateOf(existing?.archived ?: false) }
    var currencyCode  by remember { mutableStateOf(existing?.currencyCode ?: "UAH") }

    var showIconPicker     by remember { mutableStateOf(false) }
    var showDeleteConfirm  by remember { mutableStateOf(false) }
    var showBudgetCalc     by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    val nameFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isNameFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isNameFocused) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Для нової категорії — авто-фокус на полі назви
    LaunchedEffect(Unit) {
        if (existing == null) nameFocusRequester.requestFocus()
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
            onSave(name, type, colorHex, iconKey, b, period, archived, currencyCode)
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
                    title = {
                        Text(when {
                            existing == null && forParentId != null -> "Нова субкатегорія"
                            existing == null                         -> "Нова категорія"
                            existing.parentId != null                -> "Субкатегорія"
                            else                                     -> "Категорія"
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isNameFocused) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            } else {
                                onDismiss()
                            }
                        }) {
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
                            BasicTextField(
                                value          = name,
                                onValueChange  = { name = it },
                                textStyle      = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush    = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine     = true,
                                keyboardOptions   = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions   = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }),
                                modifier       = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(nameFocusRequester)
                                    .onFocusChanged { isNameFocused = it.isFocused },
                                decorationBox  = { inner ->
                                    Box {
                                        if (name.isBlank()) {
                                            Text(
                                                "Введіть назву",
                                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                            )
                                        }
                                        inner()
                                    }
                                }
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
                    val allCurrencies = CURRENCIES_MAIN + CURRENCIES_OTHER + CURRENCIES_CRYPTO
                    val currencyDef = allCurrencies.firstOrNull { it.code == currencyCode }
                    val currencyLabel = currencyDef?.let { "${it.name} – ${it.symbol}" } ?: currencyCode
                    ListItem(
                        leadingContent  = {
                            Icon(Icons.Outlined.AttachMoney, null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp))
                        },
                        headlineContent  = { Text("Валюта категорії") },
                        supportingContent = {
                            Text(
                                currencyLabel,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier.clickable { showCurrencyPicker = true }
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

                // ── Підкатегорії (тільки для кореневих категорій) ────────────
                if (existing != null && existing.parentId == null) {
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

                    items(children) { child ->
                        val childColor = remember(child.colorHex) {
                            try { Color(android.graphics.Color.parseColor(child.colorHex)) }
                            catch (_: Exception) { Color.Gray }
                        }
                        ListItem(
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(childColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        categoryIconFor(child.icon), null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            headlineContent = { Text(child.name) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    }

                    if (onAddSubcategory != null) {
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
                                modifier = Modifier.clickable { onAddSubcategory() }
                            )
                            HorizontalDivider()
                        }
                    }
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
        ConfirmationDialog(
            title     = "Видалити категорію?",
            message   = "Транзакції залишаться, але без категорії.",
            onConfirm = { showDeleteConfirm = false; onDelete?.invoke() },
            onDismiss = { showDeleteConfirm = false }
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

    // ── Вибір валюти ─────────────────────────────────────────────────────────
    if (showCurrencyPicker) {
        Dialog(
            onDismissRequest = { showCurrencyPicker = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CurrencyPageContent(
                title    = "Валюта категорії",
                selected = currencyCode,
                onSelect = { code ->
                    currencyCode = code
                    showCurrencyPicker = false
                },
                onClose  = { showCurrencyPicker = false }
            )
        }
    }
}

// ── Пікер кольору та іконки ───────────────────────────────────────────────────

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
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити", modifier = Modifier.size(34.dp))
                    }
                    Text(
                        "Значок категорії",
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { onSave(selectedColor, selectedIcon) },
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text("Готово", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIconFor(selectedIcon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Outlined.StarBorder, null, modifier = Modifier.size(30.dp)) },
                        text = { Text("Значок", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(30.dp)) },
                        text = { Text("Колір", fontWeight = FontWeight.SemiBold) }
                    )
                }

                if (selectedTab == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(CATEGORY_ICONS_LIST) { (key, icon) ->
                            val isSel = key == selectedIcon
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (isSel) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(CATEGORY_FORM_COLORS) { hex ->
                            val swatch = remember(hex) {
                                try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (_: Exception) { Color.Gray }
                            }
                            val isSel = hex == selectedColor
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .then(
                                        if (isSel) Modifier.border(
                                            width = 4.dp,
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(7.dp)
                                            .border(3.dp, Color.White, CircleShape)
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

// ── Edit Categories Screen (повноекранний діалог) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoriesScreen(
    expenseCategories:  List<CategoryEntity>,
    incomeCategories:   List<CategoryEntity>,
    monthSpending:      Map<Long, Double>    = emptyMap(),
    monthIncome:        Map<Long, Double>    = emptyMap(),
    totalExpense:       Double               = 0.0,
    totalIncome:        Double               = 0.0,
    onSave:             (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean, currencyCode: String, existing: CategoryEntity?) -> Unit,
    onAddSubcategory:   ((name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, currencyCode: String, parentId: Long) -> Unit)? = null,
    onDelete:           (CategoryEntity) -> Unit,
    onDismiss:          () -> Unit
) {
    var selectedTab       by remember { mutableIntStateOf(0) }
    var editCategory      by remember { mutableStateOf<CategoryEntity?>(null) }
    var addSubcategoryTo  by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet      by remember { mutableStateOf(false) }
    var showSubcategories by remember { mutableStateOf(false) }

    val allCats  = if (selectedTab == 0) expenseCategories.filter { !it.archived }
                   else incomeCategories.filter { !it.archived }
    val categories = if (showSubcategories) allCats.filter { it.parentId != null }
                     else allCats.filter { it.parentId == null }
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
                    },
                    actions = {
                        TextButton(
                            onClick = { showSubcategories = !showSubcategories }
                        ) {
                            Text(
                                if (showSubcategories) "Категорії" else "Субкатегорії",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (showSubcategories)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
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

                val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                CategoriesGridContent(
                    categories          = categories,
                    allCategoriesForTab = allCats,
                    spending            = spending,
                    totalExpense        = totalExpense,
                    totalIncome         = totalIncome,
                    selectedTab         = selectedTab,
                    onToggleTab         = { selectedTab = if (selectedTab == 0) 1 else 0 },
                    bottomPadding       = navBarPadding + 16.dp,
                    onChipClick         = { cat -> editCategory = cat },
                    onAdd               = { showAddSheet = true },
                    showSubcategories   = showSubcategories
                )
            }
        }
    }

    editCategory?.let { cat ->
        val catChildren = (if (cat.type == TransactionType.EXPENSE) expenseCategories else incomeCategories)
            .filter { it.parentId == cat.id }
        CategoryFormSheet(
            existing         = cat,
            children         = catChildren,
            onAddSubcategory = if (cat.parentId == null && onAddSubcategory != null)
                                   ({ addSubcategoryTo = cat }) else null,
            defaultType      = cat.type,
            onSave           = { name, type, color, icon, budget, period, arch, currency ->
                onSave(name, type, color, icon, budget, period, arch, currency, cat)
                editCategory = null
            },
            onDelete    = { onDelete(cat); editCategory = null },
            onDismiss   = { editCategory = null }
        )
    }

    addSubcategoryTo?.let { parent ->
        CategoryFormSheet(
            existing    = null,
            forParentId = parent.id,
            defaultType = parent.type,
            onSave      = { name, type, color, icon, budget, period, _, currency ->
                onAddSubcategory?.invoke(name, type, color, icon, budget, period, currency, parent.id)
                addSubcategoryTo = null
            },
            onDismiss   = { addSubcategoryTo = null }
        )
    }

    if (showAddSheet) {
        CategoryFormSheet(
            existing    = null,
            defaultType = defaultType,
            onSave      = { name, type, color, icon, budget, period, _, currency ->
                onSave(name, type, color, icon, budget, period, false, currency, null)
                showAddSheet = false
            },
            onDismiss   = { showAddSheet = false }
        )
    }
}
