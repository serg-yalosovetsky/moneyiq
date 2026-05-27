package org.pixelrush.moneyiq.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import java.util.*
import javax.inject.Inject

data class CategoriesUiState(
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    /** Потрачено по категории за текущий месяц, ключ — categoryId */
    val monthSpending: Map<Long, Double> = emptyMap()
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository,
    private val txRepo: TransactionRepository
) : ViewModel() {

    val state: StateFlow<CategoriesUiState> = combine(
        repo.getByType(TransactionType.EXPENSE),
        repo.getByType(TransactionType.INCOME),
        txRepo.getCategorySpending(TransactionType.EXPENSE, monthStart(), monthEnd())
    ) { expense, income, spending ->
        CategoriesUiState(
            expenseCategories = expense,
            incomeCategories = income,
            monthSpending = spending.associate { it.categoryId to it.total }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

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

    private fun monthStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun monthEnd(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
