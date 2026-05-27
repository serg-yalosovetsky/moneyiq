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
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import java.util.*
import javax.inject.Inject

data class SelectedMonth(val year: Int, val month: Int) // month 0-based

data class CategoriesUiState(
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories:  List<CategoryEntity> = emptyList(),
    val monthSpending:     Map<Long, Double>     = emptyMap(),
    val monthIncome:       Map<Long, Double>     = emptyMap(),
    val selectedMonth:     SelectedMonth = SelectedMonth(
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH)
    ),
    val daysInMonth:  Int    = 31,
    val totalExpense: Double = 0.0,
    val totalIncome:  Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo:      CategoryRepository,
    private val txRepo:    TransactionRepository,
    private val monthRepo: SelectedMonthRepository       // ← общий репозиторий месяца
) : ViewModel() {

    val state: StateFlow<CategoriesUiState> = monthRepo.month.flatMapLatest { appMonth ->
        val sel  = SelectedMonth(appMonth.year, appMonth.month)
        val (from, to) = monthRange(sel)
        combine(
            repo.getByType(TransactionType.EXPENSE),
            repo.getByType(TransactionType.INCOME),
            txRepo.getCategorySpending(TransactionType.EXPENSE, from, to),
            txRepo.getCategorySpending(TransactionType.INCOME,  from, to)
        ) { expense, income, expSpending, incSpending ->
            CategoriesUiState(
                expenseCategories = expense,
                incomeCategories  = income,
                monthSpending     = expSpending.associate { it.categoryId to it.total },
                monthIncome       = incSpending.associate { it.categoryId to it.total },
                selectedMonth     = sel,
                daysInMonth       = monthRepo.daysInMonth(sel.year, sel.month),
                totalExpense      = expSpending.sumOf { it.total },
                totalIncome       = incSpending.sumOf { it.total }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    /** Делегируем навигацию в общий репозиторий */
    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)

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
        cal.set(sel.year, sel.month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59);      cal.set(Calendar.MILLISECOND, 999)
        return from to cal.timeInMillis
    }
}
