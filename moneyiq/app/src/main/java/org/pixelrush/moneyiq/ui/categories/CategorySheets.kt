package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.formatMoney

// ── Кольорова палітра ─────────────────────────────────────────────────────────

internal val CATEGORY_FORM_COLORS = listOf(
    "#FF5722", "#F44336", "#E91E63", "#9C27B0",
    "#673AB7", "#3F51B5", "#2196F3", "#03A9F4",
    "#009688", "#4CAF50", "#FFEB3B", "#FF9800"
)

// ── Іконки категорій ──────────────────────────────────────────────────────────

internal val CATEGORY_ICONS_LIST: List<Pair<String, ImageVector>> = listOf(
    "category"      to Icons.Outlined.Category,
    "shopping"      to Icons.Outlined.ShoppingCart,
    "restaurant"    to Icons.Outlined.Restaurant,
    "car"           to Icons.Outlined.DirectionsCar,
    "home"          to Icons.Outlined.Home,
    "work"          to Icons.Outlined.Work,
    "school"        to Icons.Outlined.School,
    "health"        to Icons.Outlined.LocalHospital,
    "flight"        to Icons.Outlined.Flight,
    "music"         to Icons.Outlined.MusicNote,
    "money"         to Icons.Outlined.AttachMoney,
    "coffee"        to Icons.Outlined.LocalCafe,
    "pets"          to Icons.Outlined.Pets,
    "gift"          to Icons.Outlined.CardGiftcard,
    "phone"         to Icons.Outlined.PhoneAndroid,
    "sports"        to Icons.Outlined.FitnessCenter,
)

internal fun categoryIconFor(iconName: String): ImageVector =
    CATEGORY_ICONS_LIST.firstOrNull { it.first == iconName }?.second ?: Icons.Outlined.Category

// ── Quick Expense / Income Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickExpenseSheet(
    category:  CategoryEntity,
    accounts:  List<AccountEntity>,
    onSave:    (accountId: Long, amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val catColor = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val isIncome = category.type == TransactionType.INCOME

    var amountStr       by remember { mutableStateOf("0") }
    var note            by remember { mutableStateOf("") }
    var selectedAccount by remember {
        mutableStateOf(accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())
    }

    fun onKey(key: String) {
        amountStr = when (key) {
            "⌫" -> if (amountStr.length <= 1) "0" else amountStr.dropLast(1)
            "," -> {
                when {
                    "," in amountStr || "." in amountStr -> amountStr
                    amountStr == "0"                     -> "0,"
                    else                                  -> "$amountStr,"
                }
            }
            else -> {
                val dotIdx = amountStr.indexOfFirst { it == ',' || it == '.' }
                if (dotIdx >= 0 && amountStr.length - dotIdx > 2) amountStr
                else if (amountStr == "0") key
                else if (amountStr.length < 12) "$amountStr$key"
                else amountStr
            }
        }
    }

    val parsedAmount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Заголовок: іконка + назва + закрити
            Row(
                modifier          = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.size(42.dp).clip(CircleShape).background(catColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIconFor(category.icon), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isIncome) "Дохід" else "Витрата",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Закрити") }
            }

            // Відображення суми
            Box(
                modifier         = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = quickAmountDisplay(amountStr) + " ₴",
                    fontSize  = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isIncome) Color(0xFF26A69A) else MaterialTheme.colorScheme.error,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
            }

            // Вибір рахунку (горизонтальний скрол)
            if (accounts.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(vertical = 8.dp)
                ) {
                    items(accounts) { acc ->
                        val selected = selectedAccount?.id == acc.id
                        FilterChip(
                            selected    = selected,
                            onClick     = { selectedAccount = acc },
                            label       = { Text(acc.name, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            } else {
                Text(
                    "Спочатку додайте рахунок",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Нотатка
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                placeholder   = { Text("Нотатка...") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(10.dp))

            // Клавіатура (3 колонки, телефонний стиль)
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(",", "0", "⌫")
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier            = Modifier.fillMaxWidth()
            ) {
                keyRows.forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onKey(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "⌫") {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, null, modifier = Modifier.size(22.dp))
                                } else {
                                    Text(
                                        key,
                                        style      = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Кнопка «Зберегти»
            Button(
                onClick = {
                    val amt   = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val accId = selectedAccount?.id ?: return@Button
                    if (amt > 0.0) onSave(accId, amt, note.trim())
                },
                enabled  = parsedAmount > 0.0 && selectedAccount != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = catColor),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text("Зберегти", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun quickAmountDisplay(s: String): String {
    if (s.endsWith(",") || s.endsWith(".")) return s
    val d = s.replace(",", ".").toDoubleOrNull() ?: return s
    return formatMoney(d)
}

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
    var budget   by remember { mutableStateOf(existing?.budgetAmount?.takeIf { it > 0.0 }?.let {
        val s = it.toBigDecimal().stripTrailingZeros().toPlainString()
        s
    } ?: "") }
    var period   by remember { mutableStateOf(existing?.budgetPeriod ?: "MONTHLY") }
    var archived by remember { mutableStateOf(existing?.archived ?: false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val catColor = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val hasBudget = budget.isNotBlank() && (budget.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (existing != null) "Редагувати категорію" else "Нова категорія")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (name.isNotBlank()) onSave(name, type, colorHex, iconKey, b, period, archived)
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text("Зберегти", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Превʼю іконки
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(catColor)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIconFor(iconKey), null, tint = Color.White, modifier = Modifier.size(38.dp))
                }

                // Назва
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Назва") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Outlined.Category, null) }
                )

                // Тип: Витрати / Доходи
                Text("Тип", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        TransactionType.EXPENSE to "Витрати",
                        TransactionType.INCOME  to "Доходи"
                    ).forEachIndexed { i, (t, label) ->
                        SegmentedButton(
                            selected = type == t,
                            onClick  = { type = t },
                            shape    = SegmentedButtonDefaults.itemShape(i, 2),
                            label    = { Text(label) }
                        )
                    }
                }

                // Колір
                Text("Колір", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORY_FORM_COLORS) { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        }
                    }
                }

                // Іконка
                Text("Іконка", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORY_ICONS_LIST) { (key, icon) ->
                        val selected = iconKey == key
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (selected) catColor else MaterialTheme.colorScheme.surfaceVariant)
                                .then(
                                    if (selected) Modifier.border(2.dp, catColor, CircleShape)
                                    else Modifier
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, null,
                                tint     = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Бюджет
                OutlinedTextField(
                    value         = budget,
                    onValueChange = { budget = it },
                    label         = { Text("Бюджет (0 = без ліміту)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Outlined.AttachMoney, null) }
                )

                // Період бюджету (тільки якщо бюджет > 0)
                if (hasBudget) {
                    Text("Період бюджету", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("MONTHLY" to "Місяць", "WEEKLY" to "Тиждень").forEachIndexed { i, (p, label) ->
                            SegmentedButton(
                                selected = period == p,
                                onClick  = { period = p },
                                shape    = SegmentedButtonDefaults.itemShape(i, 2),
                                label    = { Text(label) }
                            )
                        }
                    }
                }

                // Архів + видалення (тільки для редагування)
                if (existing != null) {
                    HorizontalDivider()

                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("В архів", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Категорія не відображатиметься в сітці",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Switch(checked = archived, onCheckedChange = { archived = it })
                    }

                    HorizontalDivider()

                    if (onDelete != null) {
                        OutlinedButton(
                            onClick  = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Видалити категорію")
                        }
                    }
                }
            }
        }
    }

    // Підтвердження видалення
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
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Скасувати") }
            }
        )
    }
}

// ── Edit Categories Screen (повноекранний діалог) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoriesScreen(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    onSave:   (name: String, type: TransactionType, color: String, icon: String, budget: Double, period: String, archived: Boolean, existing: CategoryEntity?) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab  by remember { mutableIntStateOf(0) }
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val categories  = if (selectedTab == 0) expenseCategories else incomeCategories
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
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, "Нова категорія")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Вкладки
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text(
                            "Витрати",
                            modifier   = Modifier.padding(vertical = 14.dp),
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text(
                            "Доходи",
                            modifier   = Modifier.padding(vertical = 14.dp),
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Список
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (categories.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Немає категорій. Натисніть + щоб додати.",
                                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    items(categories) { cat ->
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { Color(0xFFFF5722) }
                        ListItem(
                            modifier = Modifier.clickable { editCategory = cat },
                            leadingContent = {
                                Box(
                                    modifier         = Modifier.size(42.dp).clip(CircleShape).background(catColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(categoryIconFor(cat.icon), null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            },
                            headlineContent = {
                                Text(cat.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = if (cat.budgetAmount > 0) {
                                { Text("Бюджет: ${formatMoney(cat.budgetAmount)} ₴", style = MaterialTheme.typography.bodySmall) }
                            } else null,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (cat.archived) {
                                        Text(
                                            "Архів",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Icon(Icons.Default.ChevronRight, null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }

    // Редагувати існуючу категорію
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

    // Додати нову категорію
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
