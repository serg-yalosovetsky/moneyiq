package org.pixelrush.moneyiq.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import java.util.Calendar
import javax.inject.Inject

data class MainUiState(
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val accounts: List<AccountEntity> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val accountRepo: AccountRepository,
    private val txRepo: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        val (monthStart, monthEnd) = currentMonthRange()

        combine(
            accountRepo.getTotalBalance().map { it ?: 0.0 },
            accountRepo.getAllAccounts(),
            txRepo.getIncomeSum(monthStart, monthEnd),
            txRepo.getExpenseSum(monthStart, monthEnd),
            txRepo.getRecentTransactions(20)
        ) { total, accounts, income, expense, recent ->
            MainUiState(
                totalBalance = total,
                monthIncome = income,
                monthExpense = expense,
                accounts = accounts,
                recentTransactions = recent,
                isLoading = false
            )
        }.onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        return start to cal.timeInMillis
    }
}
