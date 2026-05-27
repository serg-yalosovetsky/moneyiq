package org.pixelrush.moneyiq.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA
import org.pixelrush.moneyiq.data.repository.MONTH_NAMES_UA_FULL
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.ui.main.SectionHeader
import org.pixelrush.moneyiq.ui.main.TransactionListItem
import org.pixelrush.moneyiq.ui.main.formatMoney
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── UiState ───────────────────────────────────────────────────────────────────

data class TxSelectedMonth(val year: Int, val month: Int)

data class TxListUiState(
    val selectedMonth: TxSelectedMonth = run {
        val cal = Calendar.getInstance()
        TxSelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    },
    val daysInMonth: Int = 31,
    val transactions: List<TransactionWithDetails> = emptyList(),
    val totalIncome:  Double = 0.0,
    val totalExpense: Double = 0.0,
    /** Текущий суммарный баланс всех счетов */
    val closingBalance:  Double = 0.0,
    /** Приблизительный баланс до начала периода */
    val openingBalance:  Double = 0.0
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsListViewModel @Inject constructor(
    private val txRepo:      TransactionRepository,
    private val accountRepo: AccountRepository,
    private val monthRepo:   SelectedMonthRepository          // ← общий репозиторий
) : ViewModel() {

    val state: StateFlow<TxListUiState> = monthRepo.month.flatMapLatest { appMonth ->
        val sel = TxSelectedMonth(appMonth.year, appMonth.month)
        val (from, to) = monthRange(sel)
        combine(
            txRepo.getTransactionsByPeriod(from, to),
            accountRepo.getTotalBalance()
        ) { txList, rawBalance ->
            val balance = rawBalance ?: 0.0
            val income  = txList.filter { it.type.name == "INCOME"  || it.type.name == "BORROW" }
                .sumOf { it.amount }
            val expense = txList.filter { it.type.name == "EXPENSE" || it.type.name == "LEND" || it.type.name == "REPAY" }
                .sumOf { it.amount }
            val cal = Calendar.getInstance().also { it.set(sel.year, sel.month, 1) }
            TxListUiState(
                selectedMonth   = sel,
                daysInMonth     = cal.getActualMaximum(Calendar.DAY_OF_MONTH),
                transactions    = txList,
                totalIncome     = income,
                totalExpense    = expense,
                closingBalance  = balance,
                openingBalance  = balance - income + expense
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxListUiState())

    /** Делегируем навигацию в общий репозиторий */
    fun prevMonth() = monthRepo.prevMonth()
    fun nextMonth() = monthRepo.nextMonth()

    private fun monthRange(sel: TxSelectedMonth): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(sel.year, sel.month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun TransactionsListScreen(
    padding:           PaddingValues = PaddingValues(),
    onEditTransaction: (Long) -> Unit = {},
    embeddedMode:      Boolean        = false,
    viewModel:         TransactionsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (embeddedMode) 0.dp else padding.calculateTopPadding())
    ) {
        // ── Шапка ──────────────────────────────────────────────────────────
        if (!embeddedMode) {
            TxTopBar(totalBalance = state.closingBalance)
        }

        // ── Пилюля-навигатор месяца ─────────────────────────────────────
        MonthNavPill(
            sel         = state.selectedMonth,
            daysInMonth = state.daysInMonth,
            onPrev      = viewModel::prevMonth,
            onNext      = viewModel::nextMonth
        )

        // ── Карточки начального / конечного баланса ──────────────────────
        BalanceCardsRow(
            openingBalance = state.openingBalance,
            closingBalance = state.closingBalance
        )

        // ── Контент ─────────────────────────────────────────────────────
        if (state.transactions.isEmpty()) {
            EmptyMonthState(sel = state.selectedMonth)
        } else {
            TxMonthSummaryRow(
                income  = state.totalIncome,
                expense = state.totalExpense
            )
            TransactionsList(
                transactions  = state.transactions,
                bottomPadding = padding.calculateBottomPadding(),
                onEdit        = onEditTransaction
            )
        }
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun TxTopBar(totalBalance: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар слева
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Центр: «Все счета» + баланс
        Column(
            modifier            = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Все счета",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(totalBalance),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Поиск справа
        IconButton(
            onClick  = { /* TODO: поиск */ },
            modifier = Modifier.size(44.dp).clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Search, "Поиск",
                tint     = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Пилюля навигации месяца ───────────────────────────────────────────────────

@Composable
private fun MonthNavPill(
    sel:         TxSelectedMonth,
    daysInMonth: Int,
    onPrev:      () -> Unit,
    onNext:      () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // « (двойная стрелка влево = предыдущий месяц)
        IconButton(onClick = onPrev) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(20.dp))
            }
        }

        // Пилюля с [дни] МЕСЯЦ ГОД ▾
        val pillAccent = Color(0xFFD81B60)
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = pillAccent.copy(alpha = 0.12f)
        ) {
            Row(
                modifier              = Modifier.padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Бейдж с кол-вом дней
                Surface(
                    shape = CircleShape,
                    color = pillAccent
                ) {
                    Text(
                        "$daysInMonth",
                        modifier   = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
                Text(
                    "${MONTH_NAMES_UA[sel.month]} ${sel.year}",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = pillAccent
                )
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint     = pillAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // » (двойная стрелка вправо = следующий месяц)
        IconButton(onClick = onNext) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Карточки начального/конечного баланса ─────────────────────────────────────

@Composable
private fun BalanceCardsRow(openingBalance: Double, closingBalance: Double) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BalanceCard(
            label    = "Начальный баланс",
            amount   = openingBalance,
            modifier = Modifier.weight(1f)
        )
        BalanceCard(
            label    = "Конечный баланс",
            amount   = closingBalance,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BalanceCard(
    label:    String,
    amount:   Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatMoney(amount),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Строка-итоги (когда есть транзакции) ─────────────────────────────────────

@Composable
private fun TxMonthSummaryRow(income: Double, expense: Double) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryChip(
            label          = "+${formatMoney(income)}",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier       = Modifier.weight(1f)
        )
        SummaryChip(
            label          = "−${formatMoney(expense)}",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor   = MaterialTheme.colorScheme.onErrorContainer,
            modifier       = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(
    label:          String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor:   androidx.compose.ui.graphics.Color,
    modifier:       Modifier = Modifier
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = containerColor) {
        Box(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label, color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style      = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Пустое состояние ─────────────────────────────────────────────────────────

@Composable
private fun EmptyMonthState(sel: TxSelectedMonth) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong, null,
                modifier = Modifier.size(96.dp),
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text      = "Тут ви можете переглянути транзакції за\n${MONTH_NAMES_UA_FULL[sel.month]} ${sel.year}",
                style     = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
            )
        }
    }
}

// ── Список транзакций, сгруппированных по дате ────────────────────────────────

@Composable
private fun TransactionsList(
    transactions:  List<TransactionWithDetails>,
    bottomPadding: Dp,
    onEdit:        (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 88.dp)
    ) {
        val grouped = transactions.groupBy { tx ->
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
                TransactionListItem(tx = tx, onClick = { onEdit(tx.id) })
            }
        }
    }
}
