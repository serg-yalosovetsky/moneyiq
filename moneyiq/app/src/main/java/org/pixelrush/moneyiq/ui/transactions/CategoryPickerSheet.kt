package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.accounts.accountTypeIcon
import org.pixelrush.moneyiq.ui.categories.categoryIconFor
import org.pixelrush.moneyiq.ui.main.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryPickerSheet(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    accounts:          List<AccountEntity>,
    categorySpending:  Map<Long?, Double>,
    onSelect:          (CategoryEntity) -> Unit,
    onTransfer:        (AccountEntity) -> Unit,
    onDismiss:         () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }  // 0=Дохід, 1=Витрата, 2=Переказ
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    data class TabDef(val type: TransactionType?, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tabDefs = listOf(
        TabDef(TransactionType.INCOME,  "Дохід",   Icons.Default.ArrowUpward),
        TabDef(TransactionType.EXPENSE, "Витрата", Icons.Default.ArrowDownward),
        TabDef(null,                    "Переказ", Icons.Default.SwapHoriz)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(screenH * 0.67f)) {

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = MaterialTheme.colorScheme.primary
            ) {
                tabDefs.forEachIndexed { i, tab ->
                    val active = selectedTab == i
                    val tint = if (active) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    Tab(
                        selected = active,
                        onClick  = { selectedTab = i },
                        text     = {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, tint, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tab.icon, null, tint = tint, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    tab.label,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = tint
                                )
                            }
                        }
                    )
                }
            }

            val currentType = tabDefs[selectedTab].type
            val categories  = when (currentType) {
                TransactionType.INCOME  -> incomeCategories
                TransactionType.EXPENSE -> expenseCategories
                else                   -> emptyList()
            }

            if (currentType == null) {
                val totalBal = accounts.filter { it.includeInTotal }.sumOf { it.balance }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Рахунки",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${formatMoney(totalBal)} ₴",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (totalBal < 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    accounts.forEach { acc ->
                        AccountPickerRow(account = acc, onClick = { onTransfer(acc) })
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns            = GridCells.Fixed(4),
                    modifier           = Modifier.fillMaxSize(),
                    contentPadding     = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { cat ->
                        CategoryPickerCell(
                            cat      = cat,
                            amount   = categorySpending[cat.id] ?: 0.0,
                            onClick  = { onSelect(cat) }
                        )
                    }
                    item {
                        Column(
                            modifier            = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(30.dp))
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add, null,
                                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryPickerCell(
    cat:    CategoryEntity,
    amount: Double,
    onClick: () -> Unit
) {
    val catColor = remember(cat.colorHex) {
        try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val hasSpending = amount > 0
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            cat.name,
            style      = MaterialTheme.typography.labelSmall,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.fillMaxWidth().heightIn(min = 30.dp),
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (hasSpending) catColor else catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIconFor(cat.icon), null,
                tint     = if (hasSpending) Color.White else catColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatMoney(amount)} ₴",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (hasSpending) FontWeight.Bold else FontWeight.Normal,
            color      = if (hasSpending) catColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
            maxLines   = 1
        )
    }
}

@Composable
internal fun AccountPickerRow(account: AccountEntity, onClick: () -> Unit) {
    val accColor = remember(account.colorHex) {
        try { Color(android.graphics.Color.parseColor(account.colorHex)) }
        catch (_: Exception) { Color(0xFF3949AB) }
    }
    val balColor = when {
        account.balance < 0 -> Color(0xFFD32F2F)
        account.balance > 0 -> MaterialTheme.colorScheme.onSurface
        else                -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(accColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        accountTypeIcon(account.type),
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                if (account.isDefault) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint     = Color(0xFFFFB300),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomStart)
                    )
                }
            }
        },
        headlineContent = {
            Text(account.name, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                "${formatMoney(account.balance)} ₴",
                color = balColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (account.balance != 0.0) FontWeight.Bold else FontWeight.Normal
            )
        }
    )
}
