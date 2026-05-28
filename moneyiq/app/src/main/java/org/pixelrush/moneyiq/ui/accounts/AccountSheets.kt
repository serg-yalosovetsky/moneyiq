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
import org.pixelrush.moneyiq.ui.components.calculator.AmountCalculatorSheet

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
