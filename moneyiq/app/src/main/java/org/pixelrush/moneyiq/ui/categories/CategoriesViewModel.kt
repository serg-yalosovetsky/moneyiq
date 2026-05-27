package org.pixelrush.moneyiq.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import java.util.*
import javax.inject.Inject

data class SelectedMonth(val year: Int, val month: Int) // month 0-based (Calendar.MONTH)

data class CategoriesUiState(
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    /** Потрачено по категории за выбранный месяц, ключ — categoryId */
    val monthSpending: Map<Long, Double> = emptyMap(),
    /** Получено по доходным категориям за выбранный месяц */
    val monthIncome: Map<Long, Double> = emptyMap(),
    val selectedMonth: SelectedMonth = run {
        val cal = Calendar.getInstance()
        SelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    },
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository,
    private val txRepo: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(run {
        val cal = Calendar.getInstance()
        SelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    })

    val state: StateFlow<CategoriesUiState> = _selectedMonth.flatMapLatest { sel ->
        val (from, to) = monthRange(sel)
        combine(
            repo.getByType(TransactionType.EXPENSE),
            repo.getByType(TransactionType.INCOME),
            txRepo.getCategorySpending(TransactionType.EXPENSE, from, to),
            txRepo.getCategorySpending(TransactionType.INCOME, from, to)
        ) { expense, income, expSpending, incSpending ->
            CategoriesUiState(
                expenseCategories = expense,
                incomeCategories  = income,
                monthSpending     = expSpending.associate { it.categoryId to it.total },
                monthIncome       = incSpending.associate { it.categoryId to it.total },
                selectedMonth     = sel,
                totalExpense      = expSpending.sumOf { it.total },
                totalIncome       = incSpending.sumOf { it.total }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.run {
            if (month == 0) SelectedMonth(year - 1, 11) else SelectedMonth(year, month - 1)
        }
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.run {
            if (month == 11) SelectedMonth(year + 1, 0) else SelectedMonth(year, month + 1)
        }
    }

    fun add(name: String, type: TransactionType, color: String, budget: Double) {
        viewModelScope.launch {
            repo.save(CategoryEntity(name = name, type = type, colorHex = color, budgetAmount = budget))
        }
    }

    fun update(category: CategoryEntity) {
        viewModelScope.launch { repo.update(category) }
    }

    fun delete(category: CategoryEntity) {
        viewModelScope.launch { repo.delete(category) }
    }

    private fun monthRange(sel: SelectedMonth): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(sel.year, sel.month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }
}
