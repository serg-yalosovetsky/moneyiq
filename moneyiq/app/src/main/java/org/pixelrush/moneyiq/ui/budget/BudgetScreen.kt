package org.pixelrush.moneyiq.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.dao.CategorySpending
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.util.*
import javax.inject.Inject
import kotlin.math.min

// ── Период ────────────────────────────────────────────────────────────────────

enum class BudgetPeriod(val label: String) {
    WEEK("Неделя"), MONTH("Месяц"), YEAR("Год")
}

// ── UiState ───────────────────────────────────────────────────────────────────

data class BudgetCategoryRow(
    val category: CategoryEntity,
    val spent: Double
)

data class BudgetUiState(
    val period: BudgetPeriod = BudgetPeriod.MONTH,
    val rows: List<BudgetCategoryRow> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository,
    private val txRepo: TransactionRepository
) : ViewModel() {

    private val _period = MutableStateFlow(BudgetPeriod.MONTH)

    val state: StateFlow<BudgetUiState> = _period.flatMapLatest { period ->
        val (from, to) = periodRange(period)

        combine(
            categoryRepo.getAll(),
            txRepo.getCategorySpending(TransactionType.EXPENSE, from, to)
        ) { allCategories, spending ->
            val spendMap = spending.associate { it.categoryId to it.total }
            val budgetCats = allCategories.filter {
                it.type == TransactionType.EXPENSE && it.budgetAmount > 0
            }
            val rows = budgetCats.map { cat ->
                BudgetCategoryRow(category = cat, spent = spendMap[cat.id] ?: 0.0)
            }
            val adjustedBudget = adjustBudget(budgetCats, period)
            BudgetUiState(
                period       = period,
                rows         = rows,
                totalBudget  = adjustedBudget,
                totalSpent   = rows.sumOf { it.spent }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    fun setPeriod(p: BudgetPeriod) { _period.value = p }

    /** Для недели/года пересчитываем месячный лимит пропорционально */
    private fun adjustBudget(cats: List<CategoryEntity>, period: BudgetPeriod): Double {
        val monthly = cats.sumOf { it.budgetAmount }
        return when (period) {
            BudgetPeriod.WEEK  -> monthly / 4.33
            BudgetPeriod.MONTH -> monthly
            BudgetPeriod.YEAR  -> monthly * 12
        }
    }

    private fun periodRange(p: BudgetPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (p) {
            BudgetPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val from = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                from to cal.timeInMillis
            }
            BudgetPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val from = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                from to cal.timeInMillis
            }
            BudgetPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val from = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                from to cal.timeInMillis
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun BudgetScreen(
    padding: PaddingValues = PaddingValues(),
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
    ) {
        // ── Шапка ────────────────────────────────────────────────────────
        BudgetTopBar(
            totalSpent  = state.totalSpent,
            totalBudget = state.totalBudget
        )

        // ── Период-селектор ──────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = BudgetPeriod.entries.indexOf(state.period),
            edgePadding      = 16.dp,
            divider          = {},
            containerColor   = MaterialTheme.colorScheme.surface
        ) {
            BudgetPeriod.entries.forEachIndexed { idx, p ->
                Tab(
                    selected = state.period == p,
                    onClick  = { viewModel.setPeriod(p) },
                    text = {
                        Text(
                            p.label,
                            fontWeight = if (state.period == p) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // ── Суммарный прогресс ───────────────────────────────────────────
        if (state.totalBudget > 0) {
            BudgetSummaryCard(
                spent  = state.totalSpent,
                budget = state.totalBudget
            )
        }

        // ── Список категорий с бюджетом ──────────────────────────────────
        if (state.rows.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PieChart, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Нет категорий с бюджетом",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Установите лимиты в разделе «Категории»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 4.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.rows) { row ->
                    BudgetCategoryCard(row = row, period = state.period)
                }
            }
        }
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetTopBar(totalSpent: Double, totalBudget: Double) {
    val overBudget = totalBudget > 0 && totalSpent > totalBudget
    val balanceColor = when {
        totalBudget == 0.0 -> MaterialTheme.colorScheme.onSurface
        overBudget         -> MaterialTheme.colorScheme.error
        else               -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PieChart, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Бюджет",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            if (totalBudget > 0) {
                Text(
                    "${formatMoney(totalSpent)} / ${formatMoney(totalBudget)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            } else {
                Text(
                    "Нет лимитов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.width(44.dp)) // симметрия
    }
}

// ── Суммарная карточка ────────────────────────────────────────────────────────

@Composable
private fun BudgetSummaryCard(spent: Double, budget: Double) {
    val progress   = min(1f, (spent / budget).toFloat())
    val overBudget = spent > budget
    val remaining  = budget - spent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (overBudget)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Потрачено",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        formatMoney(spent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (overBudget) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (overBudget) "Превышено" else "Остаток",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        formatMoney(if (overBudget) -remaining else remaining),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (overBudget) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color    = if (overBudget) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Бюджет: ${formatMoney(budget)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

// ── Карточка категории ────────────────────────────────────────────────────────

@Composable
private fun BudgetCategoryCard(row: BudgetCategoryRow, period: BudgetPeriod) {
    val cat        = row.category
    val accentColor = remember(cat.colorHex) {
        try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }

    // Пересчитываем бюджет лимит для периода
    val periodBudget = when (period) {
        BudgetPeriod.WEEK  -> cat.budgetAmount / 4.33
        BudgetPeriod.MONTH -> cat.budgetAmount
        BudgetPeriod.YEAR  -> cat.budgetAmount * 12
    }
    val progress   = min(1f, (row.spent / periodBudget).toFloat())
    val overBudget = row.spent > periodBudget

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка категории
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Category, null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${formatMoney(row.spent)} / ${formatMoney(periodBudget)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (overBudget) MaterialTheme.colorScheme.error
                                else accentColor
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color      = if (overBudget) MaterialTheme.colorScheme.error else accentColor,
                    trackColor = accentColor.copy(alpha = 0.14f)
                )

                if (overBudget) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Превышено на ${formatMoney(row.spent - periodBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
