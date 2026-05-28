package org.pixelrush.moneyiq.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.AppMonth
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
    val appMonth:     AppMonth            = AppMonth(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)),
    val daysInMonth:  Int                = 31,
    val pillLabel:    String             = "",
    val pillBadge:    String             = "31",
    val totalExpense: Double             = 0.0,
    val totalIncome:  Double             = 0.0,
    val accounts:     List<AccountEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo:        CategoryRepository,
    private val txRepo:      TransactionRepository,
    private val monthRepo:   SelectedMonthRepository,
    private val accountRepo: AccountRepository
) : ViewModel() {

    /** Месячные данные по категориям */
    private val monthlyState: StateFlow<CategoriesUiState> =
        monthRepo.month.flatMapLatest { am ->
            val sel        = SelectedMonth(am.year, am.month)
            val (from, to) = monthRepo.computeRange(am)
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
                    appMonth          = am,
                    daysInMonth       = monthRepo.daysInPeriod(am),
                    pillLabel         = monthRepo.pillLabel(am),
                    pillBadge         = monthRepo.pillBadge(am),
                    totalExpense      = expSpending.sumOf { it.total },
                    totalIncome       = incSpending.sumOf { it.total }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    /** Полный state = месячные данные + список счетов */
    val state: StateFlow<CategoriesUiState> = combine(
        monthlyState,
        accountRepo.getAllAccounts()
    ) { catState, accounts ->
        catState.copy(accounts = accounts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    fun prevMonth()                          = monthRepo.prevMonth()
    fun nextMonth()                          = monthRepo.nextMonth()
    fun goToMonth(year: Int, month: Int)     = monthRepo.goToMonth(year, month)
    fun setPeriod(appMonth: AppMonth)        = monthRepo.setPeriod(appMonth)

    fun add(
        name:   String,
        type:   TransactionType,
        color:  String,
        icon:   String  = "category",
        budget: Double  = 0.0,
        period: String  = "MONTHLY"
    ) {
        viewModelScope.launch {
            repo.save(
                CategoryEntity(
                    name         = name,
                    type         = type,
                    colorHex     = color,
                    icon         = icon,
                    budgetAmount = budget,
                    budgetPeriod = period
                )
            )
        }
    }

    fun update(category: CategoryEntity) {
        viewModelScope.launch { repo.update(category) }
    }

    fun delete(category: CategoryEntity) {
        viewModelScope.launch { repo.delete(category) }
    }

    /** Записывает транзакцию (расход или доход) с балансовым обновлением счёта */
    fun recordTransaction(
        accountId:  Long,
        category:   CategoryEntity,
        amount:     Double,
        note:       String,
        date:       Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            txRepo.addTransaction(
                TransactionEntity(
                    type       = category.type,
                    amount     = amount,
                    accountId  = accountId,
                    categoryId = category.id,
                    note       = note,
                    date       = date
                )
            )
        }
    }

}
