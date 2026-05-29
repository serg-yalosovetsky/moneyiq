package org.pixelrush.moneyiq.ui.settings

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.pixelrush.moneyiq.BuildConfig
import org.pixelrush.moneyiq.R
import org.pixelrush.moneyiq.data.repository.*
import org.pixelrush.moneyiq.ui.settings.data.*

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

// ── Сторінка «Про додаток» ────────────────────────────────────────────────────

@Composable
internal fun AboutPageContent(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SettingsTopBar(title = "Про додаток", onBack = onBack)

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Surface(
                shape  = CircleShape,
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter            = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier           = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "MoneyIQ",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Версія ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(24.dp))

            Text(
                "Персональний фінансовий менеджер.\nВідстежуйте витрати, доходи та бюджет.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Text(
            "© 2025 PixelRush",
            modifier  = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
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
