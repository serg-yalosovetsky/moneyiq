package org.pixelrush.moneyiq.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.repository.*
import org.pixelrush.moneyiq.ui.settings.data.*

// ── Enum внутрішньої навігації ─────────────────────────────────────────────────

private enum class SettingsPage { MAIN, THEME, CURRENCY }

// ── Головний контейнер ─────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onData:         () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var page by remember { mutableStateOf(SettingsPage.MAIN) }

    val onBack: () -> Unit = {
        if (page == SettingsPage.MAIN) onNavigateBack() else page = SettingsPage.MAIN
    }

    BackHandler(onBack = onBack)

    when (page) {
        SettingsPage.MAIN     -> MainSettingsContent(
            settings   = settings,
            vm         = viewModel,
            onTheme    = { page = SettingsPage.THEME },
            onCurrency = { page = SettingsPage.CURRENCY },
            onData     = onData,
            onBack     = onBack
        )
        SettingsPage.THEME    -> ThemePageContent(
            settings = settings,
            vm       = viewModel,
            onBack   = onBack
        )
        SettingsPage.CURRENCY -> CurrencyPageContent(
            selected = settings.defaultCurrency,
            onSelect = { code -> viewModel.setDefaultCurrency(code); page = SettingsPage.MAIN },
            onClose  = { page = SettingsPage.MAIN }
        )
    }
}

// ── Головна сторінка налаштувань ───────────────────────────────────────────────

@Composable
private fun MainSettingsContent(
    settings:   AppSettings,
    vm:         SettingsViewModel,
    onTheme:    () -> Unit,
    onCurrency: () -> Unit,
    onData:     () -> Unit,
    onBack:     () -> Unit
) {
    var showHomeDialog       by remember { mutableStateOf(false) }
    var showFormatDialog     by remember { mutableStateOf(false) }
    var showWeekDialog       by remember { mutableStateOf(false) }
    var showMonthDialog      by remember { mutableStateOf(false) }
    var showNotifTimeDialog  by remember { mutableStateOf(false) }
    var showLangDialog       by remember { mutableStateOf(false) }

    val currencyInfo = CURRENCIES_ALL.find { it.code == settings.defaultCurrency }
    val currencyLabel = currencyInfo?.let { "${it.name} — ${it.symbol}" } ?: settings.defaultCurrency
    val formatLabel = CURRENCY_FORMAT_EXAMPLES.getOrNull(settings.currencyFormatIndex) ?: ""
    val weekLabel   = DAYS_OF_WEEK.find { it.first == settings.firstDayOfWeek }?.second ?: "Понеділок"
    val themeLabel  = when (settings.themeMode) {
        ThemeMode.LIGHT  -> "Світла"
        ThemeMode.DARK   -> "Темна"
        ThemeMode.SYSTEM -> "Системна"
    }
    val langLabel = LANGUAGES.find { it.first == settings.language }?.second ?: "За замовчуванням"

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SettingsTopBar(title = "Налаштування", onBack = onBack)

        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            // ── Група 1: Мова / Тема ─────────────────────────────────────────
            item {
                SettingsRow(
                    icon        = Icons.Default.Language,
                    title       = "Мова",
                    subtitle    = langLabel,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    onClick     = { showLangDialog = true }
                )
            }
            item {
                SettingsRow(
                    icon        = Icons.Default.Palette,
                    title       = "Тема",
                    subtitle    = themeLabel,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    showCrown   = true,
                    onClick     = onTheme
                )
            }
            item {
                SettingsRow(
                    icon    = Icons.Default.Storage,
                    title   = "Дані",
                    onClick = onData
                )
            }
            item { SettingsDivider() }

            // ── Група 2: Екран / Перемикачі ──────────────────────────────────
            item {
                SettingsRow(
                    icon      = Icons.Default.Home,
                    title     = "Головний екран",
                    subtitle  = settings.homeScreen.label,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    onClick   = { showHomeDialog = true }
                )
            }
            item {
                SettingsToggleRow(
                    icon    = Icons.Default.PieChart,
                    title   = "Бюджет",
                    checked = settings.budgetVisible,
                    onToggle = { vm.setBudgetVisible(it) }
                )
            }
            item {
                SettingsToggleRow(
                    icon      = Icons.Default.Fingerprint,
                    title     = "Захист при вході",
                    subtitle  = if (settings.loginProtectionEnabled) "Через 30 секунд" else null,
                    showCrown = true,
                    checked   = settings.loginProtectionEnabled,
                    onToggle  = { vm.setLoginProtection(it) }
                )
            }
            item {
                SettingsToggleRow(
                    icon      = Icons.Default.Notifications,
                    title     = "Сповіщення",
                    subtitle  = if (settings.notificationsEnabled)
                        "%02d:%02d".format(settings.notificationHour, settings.notificationMinute)
                        else null,
                    subtitleClickable = settings.notificationsEnabled,
                    checked   = settings.notificationsEnabled,
                    onToggle  = { vm.setNotifications(it, settings.notificationHour, settings.notificationMinute) },
                    onSubtitleClick = { showNotifTimeDialog = true }
                )
            }
            item { SettingsDivider() }

            // ── Група 3: Валюта / Формат / Дні ───────────────────────────────
            item {
                SettingsRow(
                    icon      = Icons.Default.AttachMoney,
                    title     = "Валюта за замовчуванням",
                    subtitle  = currencyLabel,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    onClick   = onCurrency
                )
            }
            item {
                SettingsRow(
                    icon      = Icons.Default.FormatListNumbered,
                    title     = "Формат валюти",
                    subtitle  = formatLabel,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    onClick   = { showFormatDialog = true }
                )
            }
            item {
                SettingsRow(
                    icon      = Icons.Default.CalendarToday,
                    title     = "Перший день тижня",
                    subtitle  = weekLabel,
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    onClick   = { showWeekDialog = true }
                )
            }
            item {
                SettingsRow(
                    icon      = Icons.Default.Event,
                    title     = "Перший день місяця",
                    subtitle  = settings.firstDayOfMonth.toString(),
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    showCrown = true,
                    onClick   = { showMonthDialog = true }
                )
            }
        }
    }

    // ── Діалоги ───────────────────────────────────────────────────────────────

    if (showLangDialog) {
        RadioListDialog(
            title   = "Мова",
            icon    = Icons.Default.Language,
            options = LANGUAGES.map { it.second },
            selected = LANGUAGES.indexOfFirst { it.first == settings.language }.coerceAtLeast(0),
            onSelect = { idx -> vm.setLanguage(LANGUAGES[idx].first); showLangDialog = false },
            onDismiss = { showLangDialog = false }
        )
    }

    if (showHomeDialog) {
        RadioListDialog(
            title    = "Головний екран",
            icon     = Icons.Default.Home,
            options  = HomeScreenTab.entries.map { it.label },
            selected = settings.homeScreen.index,
            onSelect = { idx -> vm.setHomeScreen(HomeScreenTab.fromIndex(idx)); showHomeDialog = false },
            onDismiss = { showHomeDialog = false }
        )
    }

    if (showFormatDialog) {
        RadioListDialog(
            title    = "Формат валюти",
            icon     = Icons.Default.FormatListNumbered,
            options  = CURRENCY_FORMAT_EXAMPLES.map { it.replace("UAH", currencyInfo?.symbol ?: "₴") },
            selected = settings.currencyFormatIndex,
            onSelect = { idx -> vm.setCurrencyFormat(idx); showFormatDialog = false },
            onDismiss = { showFormatDialog = false }
        )
    }

    if (showWeekDialog) {
        RadioListDialog(
            title    = "Перший день тижня",
            icon     = Icons.Default.CalendarToday,
            options  = DAYS_OF_WEEK.map { it.second },
            selected = DAYS_OF_WEEK.indexOfFirst { it.first == settings.firstDayOfWeek }.coerceAtLeast(0),
            onSelect = { idx -> vm.setFirstDayOfWeek(DAYS_OF_WEEK[idx].first); showWeekDialog = false },
            onDismiss = { showWeekDialog = false }
        )
    }

    if (showMonthDialog) {
        RadioListDialog(
            title    = "Перший день місяця",
            icon     = Icons.Default.Event,
            options  = (1..31).map { it.toString() },
            selected = (settings.firstDayOfMonth - 1).coerceIn(0, 30),
            onSelect = { idx -> vm.setFirstDayOfMonth(idx + 1); showMonthDialog = false },
            onDismiss = { showMonthDialog = false }
        )
    }

    if (showNotifTimeDialog) {
        TimePickerDialog(
            hour      = settings.notificationHour,
            minute    = settings.notificationMinute,
            onConfirm = { h, m -> vm.setNotifications(true, h, m); showNotifTimeDialog = false },
            onDismiss = { showNotifTimeDialog = false }
        )
    }
}
