@file:OptIn(ExperimentalLayoutApi::class)

package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.ui.accounts.accountIconFromKey
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.main.formatMoney

@Composable
internal fun TxSearchScreen(
    query:              String,
    filterTypes:        Set<String>,
    filterAccountIds:   Set<Long>,
    filterCategoryIds:  Set<Long>,
    accounts:           List<AccountEntity>,
    expenseCategories:  List<CategoryEntity>,
    incomeCategories:   List<CategoryEntity>,
    onQueryChange:      (String) -> Unit,
    onTypesChange:      (Set<String>) -> Unit,
    onAccountsChange:   (Set<Long>) -> Unit,
    onCategoriesChange: (Set<Long>) -> Unit,
    onReset:            () -> Unit,
    onDone:             () -> Unit
) {
    val expenseColor  = Color(0xFFE91E63)
    val incomeColor   = Color(0xFF009688)
    val transferColor = Color(0xFF607D8B)

    Dialog(
        onDismissRequest = onDone,
        properties       = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Close, "Скинути та закрити",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "Пошук",
                        modifier   = Modifier.weight(1f).padding(start = 4.dp),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick        = onDone,
                        shape          = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text("Готово", fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                item {
                    SearchSectionHeader("Нотатки", MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = query,
                        onValueChange = onQueryChange,
                        placeholder   = { Text("Нотатки...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        leadingIcon   = {
                            Icon(Icons.Default.Notes, null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        },
                        trailingIcon  = if (query.isNotEmpty()) {{
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }} else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    SearchSectionHeader("Тип операції", expenseColor)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple("EXPENSE",  "Витрата",  expenseColor),
                            Triple("INCOME",   "Дохід",    incomeColor),
                            Triple("TRANSFER", "Переказ",  transferColor)
                        ).forEach { (key, label, color) ->
                            val selected = key in filterTypes
                            TypeFilterCard(
                                label    = label,
                                color    = color,
                                selected = selected,
                                icon     = when (key) {
                                    "EXPENSE"  -> Icons.Default.ArrowUpward
                                    "INCOME"   -> Icons.Default.ArrowDownward
                                    else       -> Icons.Default.SwapHoriz
                                },
                                modifier = Modifier.weight(1f),
                                onClick  = {
                                    onTypesChange(if (selected) filterTypes - key else filterTypes + key)
                                }
                            )
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Рахунки", MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                val selected = acc.id in filterAccountIds
                                val accColor = try { Color(android.graphics.Color.parseColor(acc.colorHex)) }
                                               catch (_: Exception) { Color(0xFF3949AB) }
                                ColoredFilterChip(
                                    label    = acc.name,
                                    subLabel = formatMoney(acc.balance),
                                    icon     = accountIconFromKey(acc.icon),
                                    color    = accColor,
                                    selected = selected,
                                    onClick  = {
                                        onAccountsChange(if (selected) filterAccountIds - acc.id else filterAccountIds + acc.id)
                                    }
                                )
                            }
                        }
                    }
                }

                if (expenseCategories.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Витрати", expenseColor)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            expenseCategories.forEach { cat ->
                                val selected = cat.id in filterCategoryIds
                                val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                               catch (_: Exception) { Color(0xFF607D8B) }
                                ColoredFilterChip(
                                    label    = cat.name,
                                    icon     = categoryIconFor(cat.icon),
                                    color    = catColor,
                                    selected = selected,
                                    onClick  = {
                                        onCategoriesChange(if (selected) filterCategoryIds - cat.id else filterCategoryIds + cat.id)
                                    }
                                )
                            }
                        }
                    }
                }

                if (incomeCategories.isNotEmpty()) {
                    item {
                        SearchSectionHeader("Доходи", incomeColor)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            incomeCategories.forEach { cat ->
                                val selected = cat.id in filterCategoryIds
                                val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                               catch (_: Exception) { Color(0xFF607D8B) }
                                ColoredFilterChip(
                                    label    = cat.name,
                                    icon     = categoryIconFor(cat.icon),
                                    color    = catColor,
                                    selected = selected,
                                    onClick  = {
                                        onCategoriesChange(if (selected) filterCategoryIds - cat.id else filterCategoryIds + cat.id)
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    }
}

@Composable
internal fun SearchSectionHeader(title: String, color: Color) {
    Text(
        title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = color
    )
}

@Composable
internal fun TypeFilterCard(
    label:    String,
    color:    Color,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Surface(
        onClick      = onClick,
        modifier     = modifier.height(80.dp),
        shape        = RoundedCornerShape(16.dp),
        color        = if (selected) color else MaterialTheme.colorScheme.surface,
        border       = BorderStroke(
            width = 1.5.dp,
            color = color
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = if (selected) Color.White else color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (selected) Color.White else color
            )
        }
    }
}

@Composable
internal fun ColoredFilterChip(
    label:    String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    color:    Color,
    selected: Boolean,
    subLabel: String? = null,
    onClick:  () -> Unit
) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(50),
        color    = if (selected) color else MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.5.dp, color)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, null,
                tint     = if (selected) Color.White else color,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    label,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selected) Color.White else color
                )
                if (subLabel != null) {
                    Text(
                        subLabel,
                        style  = MaterialTheme.typography.labelSmall,
                        color  = if (selected) Color.White.copy(alpha = 0.8f)
                                 else color.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
