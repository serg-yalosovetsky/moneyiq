package org.pixelrush.moneyiq.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.pixelrush.moneyiq.data.repository.*
// ── Сторінка Теми ─────────────────────────────────────────────────────────────

@Composable
internal fun ThemePageContent(
    settings: AppSettings,
    vm:       SettingsViewModel,
    onBack:   () -> Unit
) {
    val themeLabel = when (settings.themeMode) {
        ThemeMode.LIGHT  -> "Світла"
        ThemeMode.DARK   -> "Темна"
        ThemeMode.SYSTEM -> "Системна"
    }
    var showThemeModeDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SettingsTopBar(title = "Тема", onBack = onBack)

        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                SettingsRow(
                    icon      = Icons.Default.Brightness6,
                    title     = "Тема",
                    subtitle  = themeLabel,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    showCrown = true,
                    onClick   = { showThemeModeDialog = true }
                )
            }
            item {
                SettingsToggleRow(
                    icon    = Icons.Default.DarkMode,
                    title   = "Темна тема",
                    checked = settings.themeMode == ThemeMode.DARK,
                    onToggle = { dark ->
                        vm.setThemeMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                    }
                )
            }
            item { SettingsDivider() }

            item {
                Text(
                    "Колір",
                    modifier  = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp),
                    style     = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color     = MaterialTheme.colorScheme.primary
                )
            }
            item {
                ColorPalette(
                    selected = Color(settings.accentColorArgb),
                    onSelect = { color -> vm.setAccentColor(color.toArgb()) }
                )
            }
        }
    }

    if (showThemeModeDialog) {
        RadioListDialog(
            title    = "Тема",
            icon     = Icons.Default.Brightness6,
            options  = listOf("Системна", "Світла", "Темна"),
            selected = when (settings.themeMode) { ThemeMode.SYSTEM -> 0; ThemeMode.LIGHT -> 1; ThemeMode.DARK -> 2 },
            onSelect = { idx ->
                vm.setThemeMode(when (idx) { 1 -> ThemeMode.LIGHT; 2 -> ThemeMode.DARK; else -> ThemeMode.SYSTEM })
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }
}

// ── Сітка кольорів ────────────────────────────────────────────────────────────

@Composable
internal fun ColorPalette(selected: Color, onSelect: (Color) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ACCENT_COLORS.chunked(5).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                row.forEach { color ->
                    val isSelected = color.toArgb() == selected.toArgb()
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { onSelect(color) }
                    )
                }
            }
        }
    }
}

// ── Сторінка вибору валюти ────────────────────────────────────────────────────

@Composable
internal fun CurrencyPageContent(
    selected: String,
    onSelect: (String) -> Unit,
    onClose:  () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Основні валюти", "Інші валюти", "Криптовалюти")
    val currencies = when (tabIndex) {
        0 -> CURRENCIES_MAIN
        1 -> CURRENCIES_OTHER
        else -> CURRENCIES_CRYPTO
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрити")
            }
            Text(
                "Валюта за замовчуванням",
                style     = MaterialTheme.typography.titleLarge,
                modifier  = Modifier.padding(start = 4.dp)
            )
        }

        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick  = { tabIndex = i },
                    text     = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        LazyColumn {
            items(currencies) { currency ->
                val isSelected = currency.code == selected
                ListItem(
                    modifier      = Modifier.clickable { onSelect(currency.code) },
                    leadingContent = {
                        RadioButton(
                            selected = isSelected,
                            onClick  = { onSelect(currency.code) }
                        )
                    },
                    headlineContent = {
                        Text(
                            currency.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingContent = {
                        Text(
                            currency.symbol,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

// ── Спільні компоненти ────────────────────────────────────────────────────────

@Composable
internal fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }
        Text(
            title,
            style    = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
internal fun SettingsRow(
    icon:          ImageVector,
    title:         String,
    subtitle:      String?       = null,
    subtitleColor: Color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    showCrown:     Boolean       = false,
    onClick:       () -> Unit
) {
    ListItem(
        modifier       = Modifier.clickable(onClick = onClick),
        leadingContent = { SettingsIcon(icon, showCrown) },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let {
            { Text(it, style = MaterialTheme.typography.bodySmall, color = subtitleColor) }
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    )
    ItemDivider()
}

@Composable
internal fun SettingsToggleRow(
    icon:               ImageVector,
    title:              String,
    subtitle:           String?   = null,
    subtitleClickable:  Boolean   = false,
    showCrown:          Boolean   = false,
    checked:            Boolean,
    onToggle:           (Boolean) -> Unit,
    onSubtitleClick:    () -> Unit = {}
) {
    ListItem(
        modifier       = Modifier.clickable { onToggle(!checked) },
        leadingContent = { SettingsIcon(icon, showCrown) },
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let {
            {
                Text(
                    it,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = if (subtitleClickable) Modifier.clickable(onClick = onSubtitleClick) else Modifier
                )
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    )
    ItemDivider()
}

@Composable
internal fun SettingsIcon(icon: ImageVector, showCrown: Boolean = false) {
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showCrown) {
            Text("👑", fontSize = 10.sp, modifier = Modifier.offset(x = 2.dp, y = (-4).dp))
        }
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(vertical = 4.dp),
        thickness = 1.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
internal fun ItemDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant
    )
}

// ── Діалог з radio-buttons ────────────────────────────────────────────────────

@Composable
internal fun RadioListDialog(
    title:    String,
    icon:     ImageVector,
    options:  List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(icon, null) },
        title = { Text(title) },
        text  = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(options.size) { idx ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(idx) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = idx == selected,
                            onClick  = { onSelect(idx) }
                        )
                        Text(
                            options[idx],
                            modifier   = Modifier.padding(start = 8.dp),
                            fontWeight = if (idx == selected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (idx == selected) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

// ── Діалог вибору часу сповіщення ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(
    hour:      Int,
    minute:    Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Час сповіщення") },
        text    = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ── Дані: мови ────────────────────────────────────────────────────────────────

val LANGUAGES: List<Pair<String, String>> = listOf(
    "default" to "За замовчуванням",
    "uk"      to "Українська",
    "ru"      to "Русский",
    "en"      to "English",
    "de"      to "Deutsch",
    "fr"      to "Français",
    "pl"      to "Polski"
)

// ── Дані: дні тижня ───────────────────────────────────────────────────────────

val DAYS_OF_WEEK: List<Pair<Int, String>> = listOf(
    1 to "Неділя",
    2 to "Понеділок",
    3 to "Вівторок",
    4 to "Середа",
    5 to "Четвер",
    6 to "П'ятниця",
    7 to "Субота"
)

// ── Дані: формати валюти ──────────────────────────────────────────────────────

val CURRENCY_FORMAT_EXAMPLES: List<String> = listOf(
    "−1.234.567,90 UAH",
    "−1 234 567.90 UAH",
    "−1 234 567,90 UAH",
    "−UAH 1,234,567.90",
    "−UAH 12,34,567.90",
    "−UAH 1.234.567,90",
    "−UAH 1 234 567.90",
    "−UAH 1 234 567,90",
    "UAH −1,234,567.90",
    "UAH −12,34,567.90",
    "UAH −1.234.567,90",
    "UAH −1 234 567.90",
    "UAH −1 234 567,90"
)

fun formatMoneyWithSettings(amount: Double, symbol: String, formatIndex: Int): String {
    val neg    = amount < 0
    val abs    = kotlin.math.abs(amount)
    val intPart = abs.toLong()
    val decPart = kotlin.math.round((abs - intPart) * 100).toInt().coerceIn(0, 99)
    val decStr  = "%02d".format(decPart)

    data class Fmt(val grp: String, val dec: Char, val indian: Boolean, val pos: Int)

    val (grp, dec, indian, pos) = when (formatIndex) {
        0  -> Fmt(".", ',', false, 0)
        1  -> Fmt(" ", '.', false, 0)
        2  -> Fmt(" ", ',', false, 0)
        3  -> Fmt(",", '.', false, 1)
        4  -> Fmt(",", '.', true,  1)
        5  -> Fmt(".", ',', false, 1)
        6  -> Fmt(" ", '.', false, 1)
        7  -> Fmt(" ", ',', false, 1)
        8  -> Fmt(",", '.', false, 2)
        9  -> Fmt(",", '.', true,  2)
        10 -> Fmt(".", ',', false, 2)
        11 -> Fmt(" ", '.', false, 2)
        12 -> Fmt(" ", ',', false, 2)
        else -> Fmt(" ", ',', false, 0)
    }

    val numStr = buildString {
        val s = intPart.toString()
        if (indian && s.length > 3) {
            val last3 = s.takeLast(3)
            val rest  = s.dropLast(3)
            rest.forEachIndexed { i, c ->
                if (i > 0 && (rest.length - i) % 2 == 0) append(grp)
                append(c)
            }
            append(grp); append(last3)
        } else {
            s.forEachIndexed { i, c ->
                if (i > 0 && (s.length - i) % 3 == 0) append(grp)
                append(c)
            }
        }
        append(dec); append(decStr)
    }

    val minus = "−"
    return when (pos) {
        0 -> if (neg) "$minus$numStr $symbol" else "$numStr $symbol"
        1 -> if (neg) "$minus$symbol $numStr"  else "$symbol $numStr"
        2 -> if (neg) "$symbol $minus$numStr"  else "$symbol $numStr"
        else -> "$numStr $symbol"
    }
}

// ── Дані: валюти ─────────────────────────────────────────────────────────────

data class CurrencyDef(val code: String, val name: String, val symbol: String)

val CURRENCIES_MAIN: List<CurrencyDef> = listOf(
    CurrencyDef("EUR", "Євро",                  "€"),
    CurrencyDef("AUD", "Австралійський долар",  "$"),
    CurrencyDef("GBP", "Британський фунт",      "£"),
    CurrencyDef("USD", "Долар США",             "$"),
    CurrencyDef("CAD", "Канадський долар",       "$"),
    CurrencyDef("CNY", "Китайський юань",        "¥"),
    CurrencyDef("RUB", "Російський рубль",       "₽"),
    CurrencyDef("UAH", "Українська гривня",      "₴"),
    CurrencyDef("CHF", "Швейцарський франк",     "Fr"),
    CurrencyDef("JPY", "Японська єна",           "¥"),
    CurrencyDef("SEK", "Шведська крона",         "kr"),
    CurrencyDef("NOK", "Норвезька крона",        "kr"),
    CurrencyDef("DKK", "Данська крона",          "kr"),
    CurrencyDef("PLN", "Польський злотий",       "zł"),
    CurrencyDef("CZK", "Чеська крона",           "Kč"),
    CurrencyDef("HUF", "Угорський форинт",       "Ft"),
    CurrencyDef("RON", "Румунський лей",         "lei"),
    CurrencyDef("BGN", "Болгарський лев",        "лв"),
    CurrencyDef("HRK", "Хорватська куна",        "kn"),
    CurrencyDef("BRL", "Бразильський реал",      "R$"),
    CurrencyDef("MXN", "Мексиканське песо",      "$"),
    CurrencyDef("INR", "Індійська рупія",        "₹"),
    CurrencyDef("KRW", "Південнокорейська вона", "₩"),
    CurrencyDef("SGD", "Сінгапурський долар",    "$"),
    CurrencyDef("HKD", "Гонконзький долар",      "$"),
    CurrencyDef("TRY", "Турецька ліра",          "₺"),
)

val CURRENCIES_OTHER: List<CurrencyDef> = listOf(
    CurrencyDef("AED", "Дирхам ОАЕ",              "د.إ"),
    CurrencyDef("AFN", "Афганський афгані",        "؋"),
    CurrencyDef("ALL", "Албанський лек",           "L"),
    CurrencyDef("AMD", "Вірменський драм",         "֏"),
    CurrencyDef("ANG", "Нідерландський антильський гульден", "ƒ"),
    CurrencyDef("AOA", "Ангольська кванза",        "Kz"),
    CurrencyDef("ARS", "Аргентинське песо",        "$"),
    CurrencyDef("AWG", "Арубський флорин",         "ƒ"),
    CurrencyDef("AZN", "Азербайджанський манат",   "₼"),
    CurrencyDef("BAM", "Боснійська марка",         "KM"),
    CurrencyDef("BBD", "Барбадоський долар",       "$"),
    CurrencyDef("BDT", "Бангладеська така",        "৳"),
    CurrencyDef("BHD", "Бахрейнський динар",       ".د.ب"),
    CurrencyDef("BIF", "Бурундійський франк",      "Fr"),
    CurrencyDef("BMD", "Бермудський долар",        "$"),
    CurrencyDef("BND", "Брунейський долар",        "$"),
    CurrencyDef("BOB", "Болівійський болівіано",   "Bs"),
    CurrencyDef("BWP", "Ботсванська пула",         "P"),
    CurrencyDef("BYN", "Білоруський рубль",        "Br"),
    CurrencyDef("BZD", "Белізький долар",          "$"),
    CurrencyDef("CLP", "Чилійське песо",           "$"),
    CurrencyDef("COP", "Колумбійське песо",        "$"),
    CurrencyDef("CRC", "Коста-риканський колон",   "₡"),
    CurrencyDef("CUP", "Кубинське песо",           "$"),
    CurrencyDef("CVE", "Кабо-вердський ескудо",    "$"),
    CurrencyDef("DJF", "Джибутійський франк",      "Fr"),
    CurrencyDef("DOP", "Домініканське песо",       "$"),
    CurrencyDef("DZD", "Алжирський динар",         "د.ج"),
    CurrencyDef("EGP", "Єгипетський фунт",        "£"),
    CurrencyDef("ERN", "Еритрейська накфа",        "Nfk"),
    CurrencyDef("ETB", "Ефіопський бир",           "Br"),
    CurrencyDef("FJD", "Фіджійський долар",        "$"),
    CurrencyDef("GEL", "Грузинський ларі",         "₾"),
    CurrencyDef("GHS", "Ганський седі",            "₵"),
    CurrencyDef("GMD", "Гамбійський даласі",       "D"),
    CurrencyDef("GTQ", "Гватемальський кетсаль",   "Q"),
    CurrencyDef("HNL", "Гондураська лемпіра",      "L"),
    CurrencyDef("IDR", "Індонезійська рупія",      "Rp"),
    CurrencyDef("ILS", "Ізраїльський шекель",      "₪"),
    CurrencyDef("IQD", "Іракський динар",          "ع.د"),
    CurrencyDef("IRR", "Іранський ріал",           "﷼"),
    CurrencyDef("ISK", "Ісландська крона",         "kr"),
    CurrencyDef("JMD", "Ямайський долар",          "$"),
    CurrencyDef("JOD", "Йорданський динар",        "JD"),
    CurrencyDef("KES", "Кенійський шилінг",        "KSh"),
    CurrencyDef("KGS", "Киргизький сом",           "с"),
    CurrencyDef("KHR", "Камбоджійський рієль",     "៛"),
    CurrencyDef("KWD", "Кувейтський динар",        "KD"),
    CurrencyDef("KZT", "Казахський тенге",         "₸"),
    CurrencyDef("LAK", "Лаоський кіп",             "₭"),
    CurrencyDef("LBP", "Ліванський фунт",          "ل.ل"),
    CurrencyDef("LKR", "Шрі-ланкійська рупія",     "₨"),
    CurrencyDef("MAD", "Марокканський дирхам",     "MAD"),
    CurrencyDef("MDL", "Молдовський лей",          "L"),
    CurrencyDef("MKD", "Македонський денар",       "ден"),
    CurrencyDef("MMK", "М'янманський кьят",        "K"),
    CurrencyDef("MNT", "Монгольський тугрик",      "₮"),
    CurrencyDef("MUR", "Маврикійська рупія",       "₨"),
    CurrencyDef("MVR", "Мальдівська руфія",        "RM"),
    CurrencyDef("MYR", "Малайзійський рингіт",     "RM"),
    CurrencyDef("NAD", "Намібійський долар",       "$"),
    CurrencyDef("NGN", "Нігерійська найра",        "₦"),
    CurrencyDef("NIO", "Нікарагуанська кордоба",   "C$"),
    CurrencyDef("NPR", "Непальська рупія",         "₨"),
    CurrencyDef("NZD", "Новозеландський долар",    "$"),
    CurrencyDef("OMR", "Оманський ріал",           "ر.ع."),
    CurrencyDef("PAB", "Панамське бальбоа",        "B/."),
    CurrencyDef("PEN", "Перуанський соль",         "S/."),
    CurrencyDef("PHP", "Філіппінське песо",        "₱"),
    CurrencyDef("PKR", "Пакистанська рупія",       "₨"),
    CurrencyDef("PYG", "Парагвайський гуарані",    "₲"),
    CurrencyDef("QAR", "Катарський ріал",          "ر.ق"),
    CurrencyDef("SAR", "Саудівський ріял",         "ر.س"),
    CurrencyDef("SBD", "Долар Соломонових Островів","$"),
    CurrencyDef("SCR", "Сейшельська рупія",        "₨"),
    CurrencyDef("SDG", "Суданський фунт",          "£"),
    CurrencyDef("SLL", "Сьєрра-леонський леоне",   "Le"),
    CurrencyDef("SOS", "Сомалійський шилінг",      "Sh"),
    CurrencyDef("SRD", "Суринамський долар",       "$"),
    CurrencyDef("SYP", "Сирійський фунт",          "£"),
    CurrencyDef("SZL", "Свазілендська ліланґені",  "L"),
    CurrencyDef("THB", "Таїландський бат",         "฿"),
    CurrencyDef("TND", "Туніський динар",          "د.ت"),
    CurrencyDef("TOP", "Тонганська паанга",        "T$"),
    CurrencyDef("TWD", "Тайванський долар",        "$"),
    CurrencyDef("TZS", "Танзанійський шилінг",     "Sh"),
    CurrencyDef("UAH", "Українська гривня",        "₴"),
    CurrencyDef("UGX", "Угандійський шилінг",      "Sh"),
    CurrencyDef("UYU", "Уругвайське песо",         "$"),
    CurrencyDef("UZS", "Узбецький сум",            "сўм"),
    CurrencyDef("VND", "В'єтнамський донг",        "₫"),
    CurrencyDef("XAF", "Центральноафриканський франк CFA", "Fr"),
    CurrencyDef("XOF", "Західноафриканський франк CFA", "Fr"),
    CurrencyDef("YER", "Єменський ріал",           "﷼"),
    CurrencyDef("ZAR", "Південноафриканський ренд", "R"),
    CurrencyDef("ZMW", "Замбійська квача",         "ZK"),
)

val CURRENCIES_CRYPTO: List<CurrencyDef> = listOf(
    CurrencyDef("BTC",  "Біткоїн",          "₿"),
    CurrencyDef("ETH",  "Ethereum",         "Ξ"),
    CurrencyDef("USDT", "Tether",           "₮"),
    CurrencyDef("BNB",  "BNB",              "BNB"),
    CurrencyDef("SOL",  "Solana",           "◎"),
    CurrencyDef("XRP",  "XRP",              "✕"),
    CurrencyDef("USDC", "USD Coin",         "◎"),
    CurrencyDef("ADA",  "Cardano",          "₳"),
    CurrencyDef("DOGE", "Dogecoin",         "Ð"),
    CurrencyDef("TRX",  "TRON",             "TRX"),
    CurrencyDef("LTC",  "Litecoin",         "Ł"),
    CurrencyDef("DOT",  "Polkadot",         "●"),
    CurrencyDef("MATIC","Polygon",          "MATIC"),
    CurrencyDef("SHIB", "Shiba Inu",        "SHIB"),
    CurrencyDef("DAI",  "Dai",              "◈"),
    CurrencyDef("AVAX", "Avalanche",        "AVAX"),
    CurrencyDef("LINK", "Chainlink",        "⬡"),
    CurrencyDef("UNI",  "Uniswap",         "🦄"),
    CurrencyDef("XMR",  "Monero",           "ɱ"),
    CurrencyDef("XLM",  "Stellar",          "✶"),
)

val CURRENCIES_ALL: List<CurrencyDef> = (CURRENCIES_MAIN + CURRENCIES_OTHER + CURRENCIES_CRYPTO)
    .distinctBy { it.code }
