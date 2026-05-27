package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.SectionHeader
import org.pixelrush.moneyiq.ui.main.TransactionListItem
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

enum class TxPeriod(val label: String) {
    DAY("День"), WEEK("Неделя"), MONTH("Месяц"), YEAR("Год"), ALL("Всё")
}

data class TxListUiState(
    val period: TxPeriod = TxPeriod.MONTH,
    val periodLabel: String = "",
    val transactions: List<TransactionWithDetails> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

@HiltViewModel
class TransactionsListViewModel @Inject constructor(
    private val repo: TransactionRepository
) : ViewModel() {

    private val _period = MutableStateFlow(TxPeriod.MONTH)

    val state: StateFlow<TxListUiState> = _period.flatMapLatest { period ->
        val (from, to, label) = periodRange(period)
        val txFlow = if (period == TxPeriod.ALL)
            repo.getRecentTransactions(1000)
        else
            repo.getTransactionsByPeriod(from, to)

        txFlow.map { txList ->
            TxListUiState(
                period = period,
                periodLabel = label,
                transactions = txList,
                totalIncome  = txList.filter { it.type.name == "INCOME" || it.type.name == "BORROW" }
                    .sumOf { it.amount },
                totalExpense = txList.filter { it.type.name == "EXPENSE" || it.type.name == "LEND" || it.type.name == "REPAY" }
                    .sumOf { it.amount }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxListUiState())

    fun setPeriod(p: TxPeriod) { _period.value = p }

    private fun periodRange(p: TxPeriod): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return when (p) {
            TxPeriod.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val from = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                Triple(from, cal.timeInMillis, "Сегодня, ${fmt.format(Date(from))}")
            }
            TxPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val from = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                Triple(from, cal.timeInMillis, "Эта неделя")
            }
            TxPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val from = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                val label = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
                    .format(Date()).replaceFirstChar { it.uppercaseChar() }
                Triple(from, cal.timeInMillis, label)
            }
            TxPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val from = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                Triple(from, cal.timeInMillis, cal.get(Calendar.YEAR).toString())
            }
            TxPeriod.ALL -> Triple(0L, Long.MAX_VALUE, "Все транзакции")
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun TransactionsListScreen(
    padding: PaddingValues = PaddingValues(),
    onEditTransaction: (Long) -> Unit = {},
    viewModel: TransactionsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Период-селектор
        ScrollableTabRow(
            selectedTabIndex = TxPeriod.entries.indexOf(state.period),
            edgePadding = 16.dp,
            divider = {},
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TxPeriod.entries.forEachIndexed { index, period ->
                Tab(
                    selected = state.period == period,
                    onClick = { viewModel.setPeriod(period) },
                    text = {
                        Text(
                            period.label,
                            fontWeight = if (state.period == period) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Мини-итоги
        if (state.transactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryChip(
                    label = "+${formatMoney(state.totalIncome)}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "−${formatMoney(state.totalExpense)}",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Список
        if (state.transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Нет транзакций за период",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                val grouped = state.transactions.groupBy { tx ->
                    SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(tx.date))
                }
                grouped.forEach { (dateLabel, txList) ->
                    item {
                        SectionHeader(
                            dateLabel,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(txList) { tx ->
                        TransactionListItem(tx = tx, onClick = { onEditTransaction(tx.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String, containerColor: Color,
    contentColor: Color, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(label, color = contentColor, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
