package org.syalosovetskyi.onemoney.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.syalosovetskyi.onemoney.data.db.dao.TransactionWithDetails
import org.syalosovetskyi.onemoney.data.db.entities.AccountEntity
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType
import org.syalosovetskyi.onemoney.data.repository.AccountRepository
import org.syalosovetskyi.onemoney.data.repository.AppMonth
import org.syalosovetskyi.onemoney.data.repository.CategoryRepository
import org.syalosovetskyi.onemoney.data.repository.SelectedMonthRepository
import org.syalosovetskyi.onemoney.data.repository.TransactionRepository
import org.syalosovetskyi.onemoney.util.calculateNextRepeatDate
import java.util.Calendar
import javax.inject.Inject

data class TxSelectedMonth(val year: Int, val month: Int)

data class TxListUiState(
    val selectedMonth: TxSelectedMonth = run {
        val cal = Calendar.getInstance()
        TxSelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    },
    val appMonth:          AppMonth                 = AppMonth(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)),
    val daysInMonth:       Int                      = 31,
    val pillBadge:         String                   = "31",
    val transactions:      List<TransactionWithDetails> = emptyList(),
    val totalIncome:       Double                   = 0.0,
    val totalExpense:      Double                   = 0.0,
    val closingBalance:    Double                   = 0.0,
    val openingBalance:    Double                   = 0.0,
    val accounts:          List<AccountEntity>      = emptyList(),
    val expenseCategories: List<CategoryEntity>     = emptyList(),
    val incomeCategories:  List<CategoryEntity>     = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsListViewModel @Inject constructor(
    private val txRepo:       TransactionRepository,
    private val accountRepo:  AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val monthRepo:    SelectedMonthRepository
) : ViewModel() {

    val state: StateFlow<TxListUiState> = monthRepo.month.flatMapLatest { am ->
        val sel        = TxSelectedMonth(am.year, am.month)
        val (from, to) = monthRepo.computeRange(am)
        combine(
            txRepo.getTransactionsByPeriod(from, to),
            accountRepo.getTotalBalance(),
            accountRepo.getAllAccounts(),
            categoryRepo.getAll()
        ) { txList, rawBalance, accounts, categories ->
            val balance = rawBalance ?: 0.0
            val income  = txList.filter { it.type == TransactionType.INCOME || it.type == TransactionType.BORROW }.sumOf { it.amount }
            val expense = txList.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LEND || it.type == TransactionType.REPAY }.sumOf { it.amount }
            TxListUiState(
                selectedMonth     = sel,
                appMonth          = am,
                daysInMonth       = monthRepo.daysInPeriod(am),
                pillBadge         = monthRepo.pillBadge(am),
                transactions      = txList,
                totalIncome       = income,
                totalExpense      = expense,
                closingBalance    = balance,
                openingBalance    = balance - income + expense,
                accounts          = accounts,
                expenseCategories = categories.filter { it.type == TransactionType.EXPENSE && !it.archived },
                incomeCategories  = categories.filter { it.type == TransactionType.INCOME && !it.archived }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxListUiState())

    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)
    fun setPeriod(appMonth: AppMonth)    = monthRepo.setPeriod(appMonth)

    fun recordTransaction(
        accountId:    Long,
        category:     CategoryEntity,
        amount:       Double,
        note:         String,
        date:         Long,
        repeatMode:   String = "NEVER",
        reminderMode: String = "NEVER"
    ) {
        viewModelScope.launch {
            val nextRepeatDate = if (repeatMode != "NEVER")
                calculateNextRepeatDate(date, repeatMode) else null
            txRepo.addTransaction(
                TransactionEntity(
                    type           = category.type,
                    amount         = amount,
                    accountId      = accountId,
                    categoryId     = category.id,
                    note           = note,
                    date           = date,
                    repeatMode     = repeatMode,
                    reminderMode   = reminderMode,
                    nextRepeatDate = nextRepeatDate
                )
            )
        }
    }

    fun recordTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, date: Long) {
        viewModelScope.launch {
            txRepo.addTransaction(
                TransactionEntity(
                    type        = TransactionType.TRANSFER,
                    amount      = amount,
                    accountId   = fromAccountId,
                    toAccountId = toAccountId,
                    note        = "",
                    date        = date
                )
            )
        }
    }

    fun deleteTransaction(tx: TransactionWithDetails) {
        viewModelScope.launch {
            val entity = txRepo.getById(tx.id) ?: return@launch
            txRepo.deleteTransaction(entity)
        }
    }

    fun updateTransaction(tx: TransactionWithDetails, note: String, amount: Double, date: Long) {
        viewModelScope.launch {
            val orig = txRepo.getById(tx.id) ?: return@launch
            txRepo.updateTransaction(orig, orig.copy(note = note, amount = amount, date = date))
        }
    }

    fun duplicateTransaction(tx: TransactionWithDetails) {
        viewModelScope.launch {
            txRepo.addTransaction(
                TransactionEntity(
                    type        = tx.type,
                    amount      = tx.amount,
                    accountId   = tx.accountId,
                    toAccountId = tx.toAccountId,
                    categoryId  = tx.categoryId,
                    note        = tx.note,
                    date        = System.currentTimeMillis()
                )
            )
        }
    }
}
