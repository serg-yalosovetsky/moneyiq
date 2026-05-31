package org.pixelrush.moneyiq.ui.budget

import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.data.db.dao.CategorySpending
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.AppMonth
import org.pixelrush.moneyiq.data.repository.CategoryRepository
import org.pixelrush.moneyiq.data.repository.SelectedMonthRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.util.MainDispatcherRule
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepo: CategoryRepository = mockk(relaxed = true)
    private val txRepo: TransactionRepository = mockk(relaxed = true)
    private val accountRepo: AccountRepository = mockk(relaxed = true)
    private lateinit var monthRepo: SelectedMonthRepository

    private val expenseCats = listOf(
        CategoryEntity(id = 1L, name = "Продукти", type = TransactionType.EXPENSE, budgetAmount = 500.0),
        CategoryEntity(id = 2L, name = "Таксі", type = TransactionType.EXPENSE, budgetAmount = 0.0)
    )
    private val incomeCats = listOf(
        CategoryEntity(id = 10L, name = "Зарплата", type = TransactionType.INCOME, budgetAmount = 20_000.0),
        CategoryEntity(id = 11L, name = "Фриланс", type = TransactionType.INCOME, budgetAmount = 5_000.0)
    )

    @Before
    fun setup() {
        monthRepo = SelectedMonthRepository().also {
            it.setPeriod(AppMonth(2026, Calendar.JUNE))
        }
        every { categoryRepo.getByType(TransactionType.EXPENSE) } returns flowOf(expenseCats)
        every { categoryRepo.getByType(TransactionType.INCOME) } returns flowOf(incomeCats)
        every { txRepo.getCategorySpending(TransactionType.EXPENSE, any(), any()) } returns flowOf(
            listOf(CategorySpending(1L, "Продукти", "#4AAFE8", "grocery", 320.0, 2))
        )
        every { txRepo.getCategorySpending(TransactionType.INCOME, any(), any()) } returns flowOf(
            listOf(CategorySpending(10L, "Зарплата", "#4CAF50", "work", 18_000.0, 1))
        )
        every { accountRepo.getTotalBalance() } returns flowOf(12_345.0)
    }

    private fun buildVm() = BudgetViewModel(categoryRepo, txRepo, accountRepo, monthRepo)

    @Test
    fun `state uses per-category income budgets`() = runTest {
        val vm = buildVm()

        vm.state.test {
            val state = awaitItem()
            assertEquals(25_000.0, state.incomeSection.totalBudget, 0.001)
            assertEquals(18_000.0, state.incomeSection.totalAmount, 0.001)
            assertEquals(listOf(10L, 11L), state.incomeSection.rows.map { it.category.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state includes unbudgeted categories so UI can render chips`() = runTest {
        val vm = buildVm()

        vm.state.test {
            val state = awaitItem()
            val taxi = state.expenseSection.rows.first { it.category.name == "Таксі" }
            assertEquals(0.0, taxi.category.budgetAmount, 0.001)
            assertEquals(0.0, taxi.amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateCategoryBudget persists amount and currency code`() = runTest {
        val vm = buildVm()
        val category = CategoryEntity(
            id = 5L,
            name = "Подорожі",
            type = TransactionType.EXPENSE,
            budgetAmount = 100.0,
            currencyCode = "UAH"
        )

        vm.updateCategoryBudget(category, 250.0, "EUR")

        coVerify {
            categoryRepo.update(match {
                it.id == 5L && it.budgetAmount == 250.0 && it.currencyCode == "EUR"
            })
        }
    }

    @Test
    fun `clearAllBudgets clears both expense and income budgets`() = runTest {
        val vm = buildVm()

        vm.clearAllBudgets()

        coVerify {
            categoryRepo.update(match { it.type == TransactionType.EXPENSE && it.budgetAmount == 0.0 })
            categoryRepo.update(match { it.type == TransactionType.INCOME && it.budgetAmount == 0.0 })
        }
    }
}
