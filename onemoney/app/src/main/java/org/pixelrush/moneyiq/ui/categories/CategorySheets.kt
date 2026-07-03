package org.syalosovetskyi.onemoney.ui.categories

import org.syalosovetskyi.onemoney.util.parseColorHex
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.syalosovetskyi.onemoney.data.db.entities.AccountEntity
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType
import org.syalosovetskyi.onemoney.ui.components.currency.CurrencyBottomSheet
import org.syalosovetskyi.onemoney.ui.components.calculator.*
import org.syalosovetskyi.onemoney.util.formatMoney
import org.syalosovetskyi.onemoney.ui.settings.data.CURRENCIES_ALL
import org.syalosovetskyi.onemoney.ui.theme.Spacing
import org.syalosovetskyi.onemoney.ui.theme.OneMoneyTheme
import org.syalosovetskyi.onemoney.ui.theme.OnLightColor
import org.syalosovetskyi.onemoney.ui.theme.FallbackCategoryColor

private val FallbackIconColor:     Color = Color(0xFF757575)
private val CategoryDisplayColor:  Color = Color(0xFF37474F)

// ── Category Action Sheet ─────────────────────────────────────────────────────

private fun txCountLabel(n: Int): String = when {
    n % 100 in 11..19 -> "$n операцій"
    n % 10 == 1        -> "$n операція"
    n % 10 in 2..4     -> "$n операції"
    else               -> "$n операцій"
}

@Composable
fun CategoryActionSheet(
    category:      CategoryEntity,
    spending:      Double,
    txCount:       Int,
    totalInPeriod: Double,
    pillLabel:     String,
    onEdit:        () -> Unit,
    onBudget:      () -> Unit,
    onOperations:  () -> Unit,
    onDismiss:     () -> Unit
) {
    val catColor = remember(category.colorHex) {
        parseColorHex(category.colorHex, FallbackCategoryColor)
    }
    val isLightBg  = catColor.luminance() > 0.5f
    val onCatColor = if (isLightBg) OnLightColor else Color.White
    val percent  = if (totalInPeriod > 0.0) (spending / totalInPeriod * 100).toInt() else 0
    val progress = if (totalInPeriod > 0.0) (spending / totalInPeriod).coerceIn(0.0, 1.0).toFloat() else 0f
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Повноекранний Dialog = скрим + кастомний шит знизу (без кліпінгу ModalBottomSheet)
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            // ── Скрим ────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onDismiss
                    )
            )

            // ── Панель знизу ─────────────────────────────────────────────
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth()) {
                // Кольорова шапка
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(catColor)
                        .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 20.dp)
                ) {
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        category.name,
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = onCatColor
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            txCountLabel(txCount),
                            color = onCatColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${formatMoney(spending)} ₴",
                            color      = onCatColor,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val clamped    = progress.coerceIn(0f, 1f)
                    val showInside = clamped >= 0.4f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(OneMoneyTheme.dimens.smallRadius))
                    ) {
                        Spacer(Modifier.fillMaxSize().background(onCatColor.copy(alpha = 0.28f)))
                        if (clamped > 0f) {
                            Spacer(Modifier.fillMaxHeight().fillMaxWidth(clamped).background(onCatColor))
                        }
                        Box(
                            Modifier.fillMaxWidth(clamped.coerceAtLeast(0.15f)).fillMaxHeight(),
                            contentAlignment = if (showInside) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Text(
                                "$percent%",
                                color      = if (showInside) catColor else onCatColor,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 11.sp,
                                lineHeight = 11.sp,
                                modifier   = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            pillLabel,
                            color = onCatColor.copy(0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${formatMoney(totalInPeriod)} ₴",
                            color      = onCatColor.copy(0.8f),
                            fontWeight = FontWeight.Medium,
                            style      = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ── Кнопки дій (білий фон) ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            start = 16.dp,
                            top = 20.dp,
                            end = 16.dp,
                            bottom = 32.dp + navigationBottom
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CatActionButton(Icons.Default.Edit,     "Редагувати", catColor, onEdit,       Modifier.weight(1f))
                    CatActionButton(Icons.Outlined.Speed,   "Бюджет",     catColor, onBudget,     Modifier.weight(1f))
                    CatActionButton(Icons.Outlined.Receipt, "Операції",   catColor, onOperations, Modifier.weight(1f))
                }
                } // Column wrapper

                // ── Іконка категорії (виступає за верхній край картки) ────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 20.dp)
                        .offset(y = (-36).dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, catColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        categoryIconFor(category.icon), null,
                        tint     = catColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CatActionButton(
    icon:     ImageVector,
    label:    String,
    color:    Color,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style     = MaterialTheme.typography.labelMedium,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

// ── Quick Expense / Income Sheet ──────────────────────────────────────────────
// Лейаут: 2 кольорові панелі (рахунок / категорія) + сума + нотатка +
//         5×4 калькулятор (оператори зліва, ✓ праворуч на 2 рядки) + дата.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickExpenseSheet(
    category:   CategoryEntity,
    categories: List<CategoryEntity> = emptyList(),
    accounts:   List<AccountEntity>,
    onSave:     (accountId: Long, amount: Double, note: String, date: Long, repeatMode: String, reminderMode: String, categoryId: Long) -> Unit,
    onDismiss:  () -> Unit
) {
    var selectedCategory by remember(category) { mutableStateOf(category) }

    val catColor = remember(selectedCategory.colorHex) {
        parseColorHex(selectedCategory.colorHex, FallbackCategoryColor)
    }
    val isCatLight   = catColor.luminance() > 0.5f
    val onCatColor   = if (isCatLight) OnLightColor else Color.White
    val displayColor = if (isCatLight) CategoryDisplayColor else catColor
    val isIncome     = selectedCategory.type == TransactionType.INCOME

    // ── Стан калькулятора ──────────────────────────────────────────────────
    val calc = rememberCalcState()

    // ── Інший стан ────────────────────────────────────────────────────────
    var note            by remember { mutableStateOf("") }
    var selectedAccount by remember {
        mutableStateOf(accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull())
    }
    var selectedDate    by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCurrency  by remember { mutableStateOf(selectedAccount?.currency ?: "UAH") }
    LaunchedEffect(selectedAccount?.currency) { selectedCurrency = selectedAccount?.currency ?: "UAH" }
    val currencySymbol = CURRENCIES_ALL.find { it.code == selectedCurrency }?.symbol ?: selectedCurrency

    var showDateSheet      by remember { mutableStateOf(false) }
    var showRepeat         by remember { mutableStateOf(false) }
    var showReminder       by remember { mutableStateOf(false) }
    var showFullDate       by remember { mutableStateOf(false) }
    var showAccSheet       by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showCatPicker      by remember { mutableStateOf(false) }
    var repeatMode     by remember { mutableStateOf("NEVER") }
    var reminderMode   by remember { mutableStateOf("NEVER") }
    var amountError    by remember { mutableStateOf(false) }

    LaunchedEffect(amountError) {
        if (amountError) { delay(600); amountError = false }
    }

    val amountDisplayColor by animateColorAsState(
        targetValue    = if (amountError) MaterialTheme.colorScheme.error else displayColor,
        animationSpec  = tween(150),
        label          = "amountColor"
    )

    // ── Логіка ────────────────────────────────────────────────────────────
    fun onConfirm() {
        val amt   = calc.result()
        val accId = selectedAccount?.id ?: return
        if (amt > 0.0) {
            onSave(accId, amt, note.trim(), selectedDate, repeatMode, reminderMode, selectedCategory.id)
        } else {
            amountError = true
        }
    }

    // ── Висота аркуша ≈ 2/3 екрана ────────────────────────────────────────
    val screenH  = LocalConfiguration.current.screenHeightDp.dp
    val sheetH   = screenH * 0.67f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetH)
        ) {
            // ── 1. Шапка: обидві іконки виступають над панелями
            Box(modifier = Modifier.fillMaxWidth()) {
                val iconD  = 80.dp
                val badgeD = 48.dp
                val stripH = 40.dp

                Column(Modifier.fillMaxWidth()) {
                    // Колірна смуга — продовжує фони панелей вгору без білого зазору
                    Row(Modifier.fillMaxWidth().height(stripH)) {
                        Spacer(Modifier.weight(1f).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface))
                        Spacer(Modifier.weight(1f).fillMaxHeight()
                            .background(catColor))
                    }
                    // Панелі
                    Row(Modifier.fillMaxWidth().height(80.dp)) {
                        // Ліва: рахунок
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { if (accounts.size > 1) showAccSheet = true }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    if (isIncome) "На рахунок" else "З рахунку",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                )
                                Text(
                                    selectedAccount?.name ?: "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // Права: категорія
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(catColor)
                                .clickable(enabled = categories.isNotEmpty()) { showCatPicker = true }
                                .padding(start = 12.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    if (isIncome) "З категорії" else "До категорії",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onCatColor.copy(0.7f)
                                )
                                Text(
                                    selectedCategory.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onCatColor,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Іконка рахунку — над лівою панеллю (badge)
                Box(
                    modifier = Modifier
                        .size(badgeD)
                        .align(Alignment.TopCenter)
                        .offset(x = -(badgeD / 2 + 8.dp))
                        .clip(RoundedCornerShape(OneMoneyTheme.dimens.cardRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CreditCard, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Іконка категорії — над правою панеллю (коло)
                Box(
                    modifier = Modifier
                        .size(iconD)
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp)
                        .clip(CircleShape)
                        .background(catColor)
                        .clickable(enabled = categories.isNotEmpty()) { showCatPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        categoryIconFor(selectedCategory.icon), null,
                        tint = onCatColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // ── 2. Відображення виразу / суми ─────────────────────────────
            val displayText = calc.displayExpr(currencySymbol)

            Column(
                modifier            = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isIncome) "Дохід" else "Витрата",
                    style = MaterialTheme.typography.labelMedium,
                    color = amountDisplayColor
                )
                Text(
                    text       = displayText,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color      = amountDisplayColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }

            // ── 3. Нотатка ────────────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                placeholder   = {
                    Text(
                        "Нотатки...",
                        style     = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier  = Modifier.fillMaxWidth()
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                modifier   = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                singleLine = true,
                shape      = RoundedCornerShape(OneMoneyTheme.dimens.cardRadius)
            )

            Spacer(Modifier.height(6.dp))

            // ── 4. Клавіатура-калькулятор ─────────────────────────────────
            SharedCalcKeypad(
                calc            = calc,
                modifier        = Modifier.weight(1f).fillMaxWidth(),
                currencySymbol  = currencySymbol,
                confirmColor    = catColor,
                onCurrencyClick = { showCurrencyPicker = true },
                onConfirm       = { onConfirm() },
                row2ExtraKey    = {
                    val keyBg = MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(OneMoneyTheme.dimens.keyRadius)).background(keyBg)
                            .clickable { showDateSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp))
                    }
                }
            )

            // ── 5. Дата внизу ─────────────────────────────────────────────
            Text(
                text      = txDateLabel(selectedDate),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    // ── Вибір валюти ─────────────────────────────────────────────────────────
    if (showCurrencyPicker) {
        CurrencyBottomSheet(
            selected  = selectedCurrency,
            title     = "Валюта транзакції",
            onSelect  = { selectedCurrency = it; showCurrencyPicker = false },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    // ── Вибір категорії ───────────────────────────────────────────────────────
    if (showCatPicker && categories.isNotEmpty()) {
        QuickCategoryPickerSheet(
            categories         = categories,
            selectedCategoryId = selectedCategory.id,
            onSelect           = { selectedCategory = it; showCatPicker = false },
            onDismiss          = { showCatPicker = false }
        )
    }

    // ── Вибір дати (аркуш) ────────────────────────────────────────────────────
    if (showDateSheet) {
        CalcDateSheet(
            currentDate  = selectedDate,
            repeatMode   = repeatMode,
            reminderMode = reminderMode,
            onDateSelected  = { selectedDate = it; showDateSheet = false },
            onRepeatClick   = { showDateSheet = false; showRepeat = true },
            onReminderClick = { showDateSheet = false; showReminder = true },
            onPickDate      = { showDateSheet = false; showFullDate = true },
            onDismiss       = { showDateSheet = false }
        )
    }

    // ── Повний DatePicker ─────────────────────────────────────────────────────
    if (showFullDate) {
        FullDatePickerDialog(
            initial        = selectedDate,
            onDateSelected = { selectedDate = it; showFullDate = false },
            onDismiss      = { showFullDate = false }
        )
    }

    // ── Діалог повторення ─────────────────────────────────────────────────────
    if (showRepeat) {
        RepeatDialog(
            current   = repeatMode,
            onSelect  = { repeatMode = it; showRepeat = false },
            onDismiss = { showRepeat = false }
        )
    }

    // ── Діалог нагадування ────────────────────────────────────────────────────
    if (showReminder) {
        ReminderDialog(
            current   = reminderMode,
            onSelect  = { reminderMode = it; showReminder = false },
            onDismiss = { showReminder = false }
        )
    }

    // ── Вибір рахунку (аркуш) ────────────────────────────────────────────────
    if (showAccSheet) {
        AccountPickerSheet(
            accounts       = accounts,
            selectedId     = selectedAccount?.id,
            label          = "З рахунку",
            onSelect       = { acc -> selectedAccount = acc; showAccSheet = false },
            onDismiss      = { showAccSheet = false }
        )
    }
}

/** Аркуш вибору категорії для швидкого запису (секції Витрати/Доходи).
 *  Раніше ~73 рядки inline у QuickExpenseSheet з дублюванням exp/inc гілок. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCategoryPickerSheet(
    categories:         List<CategoryEntity>,
    selectedCategoryId: Long,
    onSelect:           (CategoryEntity) -> Unit,
    onDismiss:          () -> Unit,
) {
    val expCats = remember(categories) {
        categories.filter { it.parentId == null && !it.archived && it.type == TransactionType.EXPENSE }
    }
    val incCats = remember(categories) {
        categories.filter { it.parentId == null && !it.archived && it.type == TransactionType.INCOME }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Text(
            "Категорія",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            quickCatSection("Витрати", expCats, selectedCategoryId, onSelect)
            quickCatSection("Доходи",  incCats, selectedCategoryId, onSelect)
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/** Секція пікера категорій (заголовок + рядки). Прибирає дубль exp/inc гілок. */
private fun LazyListScope.quickCatSection(
    title:              String,
    cats:               List<CategoryEntity>,
    selectedCategoryId: Long,
    onSelect:           (CategoryEntity) -> Unit,
) {
    if (cats.isEmpty()) return
    item {
        Text(title, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
    items(cats) { cat ->
        val color = parseColorHex(cat.colorHex, FallbackIconColor)
        ListItem(
            modifier        = Modifier.clickable { onSelect(cat) },
            leadingContent  = {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(color),
                    contentAlignment = Alignment.Center
                ) { Icon(categoryIconFor(cat.icon), null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            },
            headlineContent = { Text(cat.name) },
            trailingContent = {
                if (cat.id == selectedCategoryId)
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        )
    }
}
