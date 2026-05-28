package org.pixelrush.moneyiq.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Notes
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.ui.categories.AmountCalculatorSheet

// ── Currency data ─────────────────────────────────────────────────────────────

data class CurrencyInfo(val code: String, val name: String, val symbol: String)

val MAIN_CURRENCIES = listOf(
    CurrencyInfo("EUR", "Євро",                   "€"),
    CurrencyInfo("AUD", "Австралійський долар",    "$"),
    CurrencyInfo("GBP", "Британський фунт",        "£"),
    CurrencyInfo("USD", "Долар США",               "$"),
    CurrencyInfo("CAD", "Канадський долар",         "$"),
    CurrencyInfo("CNY", "Китайський юань",          "¥"),
    CurrencyInfo("RUB", "Російський рубль",         "₽"),
    CurrencyInfo("UAH", "Українська гривня",        "₴"),
    CurrencyInfo("CHF", "Швейцарський франк",       "Fr"),
    CurrencyInfo("JPY", "Японська єна",             "¥"),
)

val OTHER_CURRENCIES = listOf(
    CurrencyInfo("PLN", "Польський злотий",         "zł"),
    CurrencyInfo("CZK", "Чеська крона",             "Kč"),
    CurrencyInfo("HUF", "Угорський форинт",         "Ft"),
    CurrencyInfo("RON", "Румунський лей",            "lei"),
    CurrencyInfo("BGN", "Болгарський лев",           "лв"),
    CurrencyInfo("DKK", "Датська крона",             "kr"),
    CurrencyInfo("NOK", "Норвезька крона",           "kr"),
    CurrencyInfo("SEK", "Шведська крона",            "kr"),
    CurrencyInfo("TRY", "Турецька ліра",             "₺"),
    CurrencyInfo("KZT", "Казахський тенге",          "₸"),
    CurrencyInfo("GEL", "Грузинський ларі",          "₾"),
    CurrencyInfo("AMD", "Вірменський драм",          "֏"),
    CurrencyInfo("AZN", "Азербайджанський манат",    "₼"),
    CurrencyInfo("BYN", "Білоруський рубль",         "Br"),
    CurrencyInfo("MDL", "Молдовський лей",           "L"),
    CurrencyInfo("ILS", "Ізраїльський шекель",       "₪"),
    CurrencyInfo("AED", "Дирхам ОАЕ",               "د.إ"),
    CurrencyInfo("INR", "Індійська рупія",           "₹"),
    CurrencyInfo("KRW", "Корейська вона",            "₩"),
    CurrencyInfo("SGD", "Сінгапурський долар",       "$"),
    CurrencyInfo("HKD", "Гонконгський долар",        "HK$"),
    CurrencyInfo("MXN", "Мексиканське песо",         "$"),
    CurrencyInfo("BRL", "Бразильський реал",         "R$"),
    CurrencyInfo("ZAR", "Південноафриканський ренд", "R"),
)

val CRYPTO_CURRENCIES = listOf(
    CurrencyInfo("BTC",  "Bitcoin",    "₿"),
    CurrencyInfo("ETH",  "Ethereum",   "Ξ"),
    CurrencyInfo("USDT", "Tether",     "₮"),
    CurrencyInfo("BNB",  "Binance Coin","BNB"),
    CurrencyInfo("SOL",  "Solana",     "◎"),
    CurrencyInfo("USDC", "USD Coin",   "USDC"),
    CurrencyInfo("XRP",  "Ripple",     "XRP"),
    CurrencyInfo("ADA",  "Cardano",    "₳"),
    CurrencyInfo("DOGE", "Dogecoin",   "Ð"),
    CurrencyInfo("TON",  "Toncoin",    "TON"),
)

private val ALL_CURRENCIES = MAIN_CURRENCIES + OTHER_CURRENCIES + CRYPTO_CURRENCIES

fun currencyDisplayName(code: String): String = ALL_CURRENCIES.find { it.code == code }?.name ?: code
fun currencySymbol(code: String): String = ALL_CURRENCIES.find { it.code == code }?.symbol ?: code

// ── Account type helpers (Ukrainian) ─────────────────────────────────────────

fun accountTypeNameUA(type: AccountType): String = when (type) {
    AccountType.CASH       -> "Готівка"
    AccountType.CARD       -> "Карта"
    AccountType.SAVING     -> "Заощадження"
    AccountType.INVESTMENT -> "Інвестиції"
    AccountType.DEBT       -> "Борговий"
    AccountType.OTHER      -> "Інше"
}

val ACCOUNT_FORM_COLORS = listOf(
    "#D81B60", "#4361EE", "#3A86FF", "#8338EC",
    "#FB5607", "#FFBE0B", "#06D6A0", "#118AB2",
    "#4CAF50", "#009688", "#607D8B", "#A07040"
)

// ── NewAccountTypeSheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAccountTypeSheet(
    onSelect:  (AccountType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor  = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Новий рахунок",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            AccountTypeOption(
                color    = Color(0xFFB07040),
                icon     = Icons.Outlined.Wallet,
                title    = "Звичайний",
                subtitle = "Готівка, карта, ...",
                onClick  = { onSelect(AccountType.CASH) }
            )
            AccountTypeOption(
                color    = Color(0xFF2E7D60),
                icon     = Icons.Outlined.MoneyOff,
                title    = "Борговий",
                subtitle = "Кредит, іпотека, ...",
                onClick  = { onSelect(AccountType.DEBT) }
            )
            AccountTypeOption(
                color    = Color(0xFF3D3F8F),
                icon     = Icons.Outlined.Savings,
                title    = "Накопичувальний",
                subtitle = "Заощадження, мета, ...",
                onClick  = { onSelect(AccountType.SAVING) }
            )
        }
    }
}

@Composable
private fun AccountTypeOption(
    color:    Color,
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    onClick:  () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier            = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        },
        headlineContent   = {
            Text(title, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    )
}

// ── AccountFormSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    initialType: AccountType = AccountType.CASH,
    existing:    org.pixelrush.moneyiq.data.db.entities.AccountEntity? = null,
    onSave:      (name: String, type: AccountType, balance: Double, color: String, currency: String, description: String, includeInTotal: Boolean, icon: String) -> Unit,
    onDismiss:   () -> Unit
) {
    var name           by remember { mutableStateOf(existing?.name ?: "") }
    var type           by remember { mutableStateOf(existing?.type ?: initialType) }
    var balanceStr     by remember {
        mutableStateOf(
            existing?.balance?.let { if (it == 0.0) "" else it.toBigDecimal().stripTrailingZeros().toPlainString() } ?: ""
        )
    }
    var colorHex       by remember { mutableStateOf(existing?.colorHex ?: "#D81B60") }
    var iconKey        by remember { mutableStateOf(existing?.icon ?: "account_balance_wallet") }
    var currency       by remember { mutableStateOf(existing?.currency ?: "UAH") }
    var description    by remember { mutableStateOf(existing?.description ?: "") }
    var includeInTotal by remember { mutableStateOf(existing?.includeInTotal ?: true) }

    var showIconColorPicker by remember { mutableStateOf(false) }
    var showTypePicker      by remember { mutableStateOf(false) }
    var showCurrencyPicker  by remember { mutableStateOf(false) }
    var showDescEditor      by remember { mutableStateOf(false) }
    var showBalanceInput    by remember { mutableStateOf(false) }

    val accentColor = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (_: Exception) { Color(0xFFD81B60) }
    }
    val currencyLabel = "${currencyDisplayName(currency)} – ${currencySymbol(currency)}"
    val sym           = currencySymbol(currency)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {

                // ── Top bar ─────────────────────────────────────────────────
                Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Закрити")
                        }
                        Text(
                            if (existing != null) "Редагувати рахунок" else "Новий рахунок",
                            modifier   = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = {
                                val b = balanceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (name.isNotBlank()) {
                                    onSave(name, type, b, colorHex, currency, description, includeInTotal, iconKey)
                                }
                            },
                            shape           = RoundedCornerShape(50),
                            contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            enabled         = name.isNotBlank()
                        ) {
                            Text("Готово", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                LazyColumn(Modifier.fillMaxSize()) {

                    // ── Name + icon ──────────────────────────────────────────
                    item {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value         = name,
                                    onValueChange = { name = it },
                                    modifier      = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    textStyle     = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    decorationBox = { inner ->
                                        Box {
                                            if (name.isEmpty()) {
                                                Text(
                                                    "Назва",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                                // Colored icon box (opens icon+color picker)
                                Box(
                                    modifier         = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accentColor)
                                        .clickable { showIconColorPicker = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        accountIconFromKey(iconKey), null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ── Section: Рахунок ─────────────────────────────────────
                    item { FormSectionHeader("Рахунок") }

                    item {
                        FormNavRow(
                            icon    = accountTypeIcon(type),
                            label   = "Тип",
                            value   = accountTypeNameUA(type),
                            onClick = { showTypePicker = true }
                        )
                    }
                    item {
                        FormNavRow(
                            icon    = Icons.Outlined.AttachMoney,
                            label   = "Валюта рахунку",
                            value   = currencyLabel,
                            onClick = { showCurrencyPicker = true }
                        )
                    }
                    item {
                        FormNavRow(
                            icon    = Icons.AutoMirrored.Outlined.Notes,
                            label   = "Опис",
                            value   = description,
                            onClick = { showDescEditor = true }
                        )
                    }

                    // ── Section: Баланс ──────────────────────────────────────
                    item { FormSectionHeader("Баланс") }

                    item {
                        FormValueRow(
                            label   = "Баланс рахунку",
                            value   = if (balanceStr.isEmpty()) "0 $sym" else "$balanceStr $sym",
                            onClick = { showBalanceInput = true }
                        )
                    }
                    item {
                        FormValueRow(
                            label   = "Кредитний ліміт",
                            value   = "0 $sym",
                            onClick = { /* TODO */ }
                        )
                    }
                    item {
                        ListItem(
                            headlineContent   = { Text("Враховувати в загальному балансі") },
                            trailingContent   = {
                                Switch(
                                    checked         = includeInTotal,
                                    onCheckedChange = { includeInTotal = it }
                                )
                            }
                        )
                    }

                    item { Spacer(Modifier.height(48.dp)) }
                }
            }
        }
    }

    // ── Nested sheets ────────────────────────────────────────────────────────
    if (showIconColorPicker) {
        IconColorPickerScreen(
            initialIconKey  = iconKey,
            initialColorHex = colorHex,
            onResult = { newIcon, newColor ->
                iconKey  = newIcon
                colorHex = newColor
                showIconColorPicker = false
            },
            onDismiss = { showIconColorPicker = false }
        )
    }

    if (showTypePicker) {
        TypePickerSheet(
            selected  = type,
            onSelect  = { type = it; showTypePicker = false },
            onDismiss = { showTypePicker = false }
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            selected  = currency,
            onSelect  = { currency = it; showCurrencyPicker = false },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    if (showDescEditor) {
        DescEditorDialog(
            initial   = description,
            onSave    = { description = it; showDescEditor = false },
            onDismiss = { showDescEditor = false }
        )
    }

    if (showBalanceInput) {
        AmountCalculatorSheet(
            initial        = balanceStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            currencySymbol = sym,
            title          = "Баланс рахунку",
            onResult       = { v ->
                balanceStr = if (v == 0.0) "" else v.toBigDecimal().stripTrailingZeros().toPlainString()
                showBalanceInput = false
            },
            onDismiss      = { showBalanceInput = false }
        )
    }
}

// ── CurrencyPickerSheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    selected:  String,
    onSelect:  (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {

                // Top bar
                Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Закрити")
                        }
                        Text(
                            "Валюта рахунку",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                var tab by remember { mutableIntStateOf(0) }
                val tabLists = listOf(MAIN_CURRENCIES, OTHER_CURRENCIES, CRYPTO_CURRENCIES)
                val tabLabels = listOf("Основні валюти", "Інші валюти", "Криптовалюти")
                val tabIcons  = listOf<ImageVector>(
                    Icons.Outlined.MonetizationOn,
                    Icons.Outlined.CurrencyExchange,
                    Icons.Outlined.Memory            // placeholder for crypto
                )

                TabRow(
                    selectedTabIndex = tab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary
                ) {
                    tabLabels.forEachIndexed { i, label ->
                        Tab(
                            selected = tab == i,
                            onClick  = { tab = i },
                            icon     = { Icon(tabIcons[i], null, modifier = Modifier.size(20.dp)) },
                            text     = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                LazyColumn(Modifier.fillMaxSize()) {
                    items(tabLists[tab]) { cur ->
                        val isSelected = cur.code == selected
                        ListItem(
                            modifier          = Modifier.clickable { onSelect(cur.code) },
                            leadingContent    = {
                                RadioButton(
                                    selected  = isSelected,
                                    onClick   = { onSelect(cur.code) }
                                )
                            },
                            headlineContent   = {
                                Text(
                                    cur.name,
                                    color      = if (isSelected) MaterialTheme.colorScheme.primary
                                                 else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            trailingContent   = {
                                Text(
                                    cur.symbol,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        )
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 56.dp),
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ── TypePickerSheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypePickerSheet(
    selected:  AccountType,
    onSelect:  (AccountType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                "Тип рахунку",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
            AccountType.entries.forEach { t ->
                ListItem(
                    modifier          = Modifier.clickable { onSelect(t) },
                    leadingContent    = {
                        Icon(accountTypeIcon(t), null,
                            tint = if (t == selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    },
                    headlineContent   = {
                        Text(
                            accountTypeNameUA(t),
                            color      = if (t == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (t == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingContent   = {
                        if (t == selected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

// ── ColorPickerSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    colors:    List<String>,
    selected:  String,
    onSelect:  (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
            Text(
                "Колір рахунку",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
            // Grid: 4 per row
            colors.chunked(4).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (_: Exception) { Color.Gray }
                        Box(
                            modifier         = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { onSelect(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected.equals(hex, ignoreCase = true)) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    // Fill empty cells
                    repeat(4 - row.size) {
                        Spacer(Modifier.size(52.dp))
                    }
                }
            }
        }
    }
}

// ── DescEditorDialog ──────────────────────────────────────────────────────────

@Composable
fun DescEditorDialog(
    initial:   String,
    onSave:    (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Опис") },
        text    = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Введіть опис...") },
                maxLines      = 4
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ── BalanceInputDialog ────────────────────────────────────────────────────────

@Composable
fun BalanceInputDialog(
    initial:   String,
    symbol:    String,
    onSave:    (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Баланс рахунку") },
        text    = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("0") },
                singleLine    = true,
                suffix        = { Text(" $symbol") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) { Text("Готово") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ── Shared form helper composables ────────────────────────────────────────────

@Composable
fun FormSectionHeader(title: String) {
    Text(
        title,
        modifier   = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        style      = MaterialTheme.typography.labelLarge,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun FormNavRow(
    icon:    ImageVector?,
    label:   String,
    value:   String = "",
    onClick: () -> Unit
) {
    ListItem(
        modifier          = Modifier.clickable(onClick = onClick),
        leadingContent    = if (icon != null) {{
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }} else null,
        headlineContent   = { Text(label) },
        trailingContent   = if (value.isNotBlank()) {{
            Text(
                value,
                color      = MaterialTheme.colorScheme.primary,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
        }} else null
    )
    HorizontalDivider(
        modifier  = Modifier.padding(start = if (icon != null) 56.dp else 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun FormValueRow(
    label:   String,
    value:   String,
    onClick: () -> Unit
) {
    ListItem(
        modifier        = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    )
    HorizontalDivider(
        modifier  = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

// ── AccountActionSheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActionSheet(
    account:         org.pixelrush.moneyiq.data.db.entities.AccountEntity,
    onDismiss:       () -> Unit,
    onEdit:          () -> Unit,
    onAdjustBalance: () -> Unit,
    onTransactions:  () -> Unit,
    onIncome:        () -> Unit,
    onExpense:       () -> Unit,
    onTransfer:      () -> Unit,
    onSetDefault:    () -> Unit
) {
    val accentColor = remember(account.colorHex) {
        try { Color(android.graphics.Color.parseColor(account.colorHex)) }
        catch (_: Exception) { Color(0xFF4361EE) }
    }
    val sym = currencySymbol(account.currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // ── Картка рахунку ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor)
                    .padding(20.dp)
            ) {
                // Іконка + назва зліва
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            accountIconFromKey(account.icon), null,
                            tint     = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        account.name,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }

                // Зірочка справа
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onSetDefault(); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Основний рахунок",
                        tint     = if (account.isDefault) Color(0xFFFFD700) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Баланс по центру знизу
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Баланс рахунку",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        "${org.pixelrush.moneyiq.ui.main.formatMoney(account.balance)} $sym",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Рядки кнопок ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccountActionButton(Icons.Default.Edit,                 "Редагувати", Color(0xFFFFAB00)) { onEdit(); onDismiss() }
                AccountActionButton(Icons.Default.SwapVert,             "Баланс",     Color(0xFF9E9E9E)) { onAdjustBalance(); onDismiss() }
                AccountActionButton(Icons.AutoMirrored.Filled.ReceiptLong, "Операції", Color(0xFF5C6BC0)) { onTransactions(); onDismiss() }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccountActionButton(Icons.Default.ArrowUpward,              "Поповнення", Color(0xFF00897B)) { onIncome(); onDismiss() }
                AccountActionButton(Icons.Default.ArrowDownward,            "Списати",    Color(0xFFE91E63)) { onExpense(); onDismiss() }
                AccountActionButton(Icons.AutoMirrored.Filled.ArrowForward, "Переказ",   Color(0xFF9E9E9E)) { onTransfer(); onDismiss() }
            }
        }
    }
}

@Composable
private fun AccountActionButton(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    color:   Color,
    onClick: () -> Unit
) {
    Column(
        modifier            = Modifier
            .widthIn(min = 80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
