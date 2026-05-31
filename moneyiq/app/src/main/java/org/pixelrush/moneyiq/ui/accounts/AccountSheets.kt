package org.pixelrush.moneyiq.ui.accounts

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.background
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.border
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.clickable
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.layout.*
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.lazy.items
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.CircleShape
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.text.BasicTextField
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.Icons
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.filled.*
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.outlined.*
import androidx.core.graphics.toColorInt
import androidx.compose.material3.*
import androidx.core.graphics.toColorInt
import androidx.compose.runtime.*
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Alignment
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Modifier
import androidx.core.graphics.toColorInt
import androidx.compose.ui.draw.clip
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.toColorInt
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.ui.components.calculator.AmountCalculatorSheet
import org.pixelrush.moneyiq.ui.components.currency.CurrencyPickerSheet
import org.pixelrush.moneyiq.ui.components.form.FormNavRow
import org.pixelrush.moneyiq.ui.components.form.FormSectionHeader
import org.pixelrush.moneyiq.ui.components.form.FormValueRow
import org.pixelrush.moneyiq.ui.settings.data.CURRENCIES_ALL

fun currencyDisplayName(code: String): String = CURRENCIES_ALL.find { it.code == code }?.name ?: code
fun currencySymbol(code: String): String      = CURRENCIES_ALL.find { it.code == code }?.symbol ?: code

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
    onSave:      (name: String, type: AccountType, balance: Double, color: String, currency: String, description: String, includeInTotal: Boolean, icon: String, creditLimit: Double) -> Unit,
    onDismiss:   () -> Unit
) {
    var name           by remember { mutableStateOf(existing?.name ?: "") }
    var type           by remember { mutableStateOf(existing?.type ?: initialType) }
    var balanceStr     by remember {
        mutableStateOf(
            existing?.balance?.let { if (it == 0.0) "" else it.toBigDecimal().stripTrailingZeros().toPlainString() } ?: ""
        )
    }
    var creditLimit    by remember { mutableStateOf(existing?.creditLimit ?: 0.0) }
    var colorHex       by remember { mutableStateOf(existing?.colorHex ?: "#D81B60") }
    var iconKey        by remember { mutableStateOf(existing?.icon ?: "account_balance_wallet") }
    var currency       by remember { mutableStateOf(existing?.currency ?: "UAH") }
    var description    by remember { mutableStateOf(existing?.description ?: "") }
    var includeInTotal by remember { mutableStateOf(existing?.includeInTotal ?: true) }

    var showIconColorPicker  by remember { mutableStateOf(false) }
    var showTypePicker       by remember { mutableStateOf(false) }
    var showCurrencyPicker   by remember { mutableStateOf(false) }
    var showDescEditor       by remember { mutableStateOf(false) }
    var showBalanceInput     by remember { mutableStateOf(false) }
    var showCreditLimitInput by remember { mutableStateOf(false) }

    val accentColor = remember(colorHex) {
        try { Color(colorHex.toColorInt()) }
        catch (_: Exception) { Color(0xFFD81B60) }
    }
    val iconTint      = if (accentColor.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White
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
                                    onSave(name, type, b, colorHex, currency, description, includeInTotal, iconKey, creditLimit)
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
                                        tint     = iconTint,
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
                            value   = if (creditLimit == 0.0) "0 $sym" else "${creditLimit.toBigDecimal().stripTrailingZeros().toPlainString()} $sym",
                            onClick = { showCreditLimitInput = true }
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

    if (showCreditLimitInput) {
        AmountCalculatorSheet(
            initial        = creditLimit,
            currencySymbol = sym,
            title          = "Кредитний ліміт",
            onResult       = { v -> creditLimit = v; showCreditLimitInput = false },
            onDismiss      = { showCreditLimitInput = false }
        )
    }
}
