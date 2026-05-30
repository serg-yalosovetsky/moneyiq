package org.pixelrush.moneyiq.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.AppMonth
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import java.util.Calendar
import javax.inject.Inject

data class BudgetSelMonth(val year: Int, val month: Int)

data class BudgetCatRow(
    val category: CategoryEntity,
    val amount:   Double
)

data class BudgetSectionData(
    val totalBudget: Double,
    val totalAmount: Double,
    val rows:        List<BudgetCatRow>
)

data class BudgetUiState(
    val selectedMonth: BudgetSelMonth = run {
        val cal = Calendar.getInstance()
        BudgetSelMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    },
    val appMonth:       AppMonth          = AppMonth(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)),
    val daysInMonth:    Int              = 31,
    val daysPassed:     Int              = 0,
    val pillLabel:      String           = "",
    val pillBadge:      String           = "31",
    val totalBalance:   Double           = 0.0,
    val expenseSection: BudgetSectionData = BudgetSectionData(0.0, 0.0, emptyList()),
    val incomeSection:  BudgetSectionData = BudgetSectionData(0.0, 0.0, emptyList())
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository,
    private val txRepo:       TransactionRepository,
    private val accountRepo:  AccountRepository,
    private val monthRepo:    SelectedMonthRepository
) : ViewModel() {

    val state: StateFlow<BudgetUiState> = monthRepo.month.flatMapLatest { am ->
        val sel        = BudgetSelMonth(am.year, am.month)
        val (from, to) = monthRepo.computeRange(am)
        val totalDays = monthRepo.daysInPeriod(am)
        val daysPassed = run {
            val now = Calendar.getInstance()
            val curYear  = now.get(Calendar.YEAR)
            val curMonth = now.get(Calendar.MONTH)
            when {
                am.year < curYear || (am.year == curYear && am.month < curMonth) -> totalDays
                am.year == curYear && am.month == curMonth -> now.get(Calendar.DAY_OF_MONTH)
                else -> 0
            }
        }
        combine(
            categoryRepo.getByType(TransactionType.EXPENSE),
            categoryRepo.getByType(TransactionType.INCOME),
            txRepo.getCategorySpending(TransactionType.EXPENSE, from, to),
            txRepo.getCategorySpending(TransactionType.INCOME, from, to),
            accountRepo.getTotalBalance()
        ) { expCats, incCats, expSpend, incSpend, rawBalance ->
            val expMap  = expSpend.associate { it.categoryId to it.total }
            val incMap  = incSpend.associate { it.categoryId to it.total }
            val expRows = expCats.map { BudgetCatRow(it, expMap[it.id] ?: 0.0) }
            val incRows = incCats.map { BudgetCatRow(it, incMap[it.id] ?: 0.0) }
            BudgetUiState(
                selectedMonth  = sel,
                appMonth       = am,
                daysInMonth    = totalDays,
                daysPassed     = daysPassed,
                pillLabel      = monthRepo.pillLabel(am),
                pillBadge      = monthRepo.pillBadge(am),
                totalBalance   = rawBalance ?: 0.0,
                expenseSection = BudgetSectionData(
                    totalBudget = expCats.sumOf { it.budgetAmount },
                    totalAmount = expRows.sumOf { it.amount },
                    rows        = expRows
                ),
                incomeSection = BudgetSectionData(
                    totalBudget = incCats.sumOf { it.budgetAmount },
                    totalAmount = incRows.sumOf { it.amount },
                    rows        = incRows
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    fun prevMonth()                      = monthRepo.prevMonth()
    fun nextMonth()                      = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int) = monthRepo.goToMonth(year, month)
    fun setPeriod(appMonth: AppMonth)    = monthRepo.setPeriod(appMonth)

    fun updateCategoryBudget(category: CategoryEntity, newBudget: Double) {
        viewModelScope.launch { categoryRepo.update(category.copy(budgetAmount = newBudget)) }
    }

    fun clearAllBudgets() {
        viewModelScope.launch {
            val allCats = categoryRepo.getByType(TransactionType.EXPENSE).first() +
                          categoryRepo.getByType(TransactionType.INCOME).first()
            allCats.forEach { categoryRepo.update(it.copy(budgetAmount = 0.0)) }
        }
    }
}
