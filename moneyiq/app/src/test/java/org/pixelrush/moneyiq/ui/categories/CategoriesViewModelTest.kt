package org.pixelrush.moneyiq.ui.categories

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catRepo: CategoryRepository = mockk(relaxed = true)
    private val txRepo: TransactionRepository = mockk(relaxed = true)
    private val monthRepo: SelectedMonthRepository = mockk(relaxed = true)
    private val accountRepo: AccountRepository = mockk(relaxed = true)

    private val monthFlow = MutableStateFlow(AppMonth(2025, 4))

    private lateinit var vm: CategoriesViewModel

    @Before
    fun setup() {
        every { monthRepo.month } returns monthFlow
        every { monthRepo.computeRange(any()) } returns (0L to Long.MAX_VALUE / 2)
        every { monthRepo.daysInPeriod(any()) } returns 31
        every { monthRepo.pillLabel(any()) } returns "ТРАВЕНЬ 2025"
        every { monthRepo.pillBadge(any()) } returns "31"

        every { catRepo.getByType(TransactionType.EXPENSE) } returns flowOf(emptyList())
        every { catRepo.getByType(TransactionType.INCOME) } returns flowOf(emptyList())
        every { txRepo.getCategorySpending(TransactionType.EXPENSE, any(), any()) } returns flowOf(emptyList())
        every { txRepo.getCategorySpending(TransactionType.INCOME, any(), any()) } returns flowOf(emptyList())
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())

        vm = CategoriesViewModel(catRepo, txRepo, monthRepo, accountRepo)
    }

    @Test
    fun `initial state has empty categories`() = runTest {
        vm.state.test {
            val state = awaitItem()
            assertTrue(state.expenseCategories.isEmpty())
            assertTrue(state.incomeCategories.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects expense categories from repo`() = runTest {
        val expenses = listOf(
            CategoryEntity(id = 1L, name = "Продукти", type = TransactionType.EXPENSE, colorHex = "#03A9F4", icon = "shopping"),
            CategoryEntity(id = 2L, name = "Транспорт", type = TransactionType.EXPENSE, colorHex = "#FF9800", icon = "car")
        )
        every { catRepo.getByType(TransactionType.EXPENSE) } returns flowOf(expenses)
        vm = CategoriesViewModel(catRepo, txRepo, monthRepo, accountRepo)

        vm.state.test {
            val state = awaitItem()
            assertEquals(2, state.expenseCategories.size)
            assertEquals("Продукти", state.expenseCategories[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects monthly spending from txRepo`() = runTest {
        val spending = listOf(
            CategorySpending(categoryId = 1L, categoryName = "Продукти", categoryColor = "#03A9F4", categoryIcon = "shopping", total = 500.0, count = 3)
        )
        every { txRepo.getCategorySpending(TransactionType.EXPENSE, any(), any()) } returns flowOf(spending)
        vm = CategoriesViewModel(catRepo, txRepo, monthRepo, accountRepo)

        vm.state.test {
            val state = awaitItem()
            assertEquals(500.0, state.monthSpending[1L] ?: 0.0, 0.001)
            assertEquals(500.0, state.totalExpense, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects accounts`() = runTest {
        val accounts = listOf(
            org.pixelrush.moneyiq.data.db.entities.AccountEntity(id = 1L, name = "Готівка", balance = 1000.0)
        )
        every { accountRepo.getAllAccounts() } returns flowOf(accounts)
        vm = CategoriesViewModel(catRepo, txRepo, monthRepo, accountRepo)

        vm.state.test {
            val state = awaitItem()
            assertEquals(1, state.accounts.size)
            assertEquals("Готівка", state.accounts[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleSubcategories flips showSubcategories flag`() = runTest {
        vm.state.test {
            val initial = awaitItem()
            assertFalse(initial.showSubcategories)

            vm.toggleSubcategories()
            val toggled = awaitItem()
            assertTrue(toggled.showSubcategories)

            vm.toggleSubcategories()
            val restored = awaitItem()
            assertFalse(restored.showSubcategories)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `prevMonth delegates to monthRepo`() = runTest {
        vm.prevMonth()
        verify { monthRepo.prevMonth() }
    }

    @Test
    fun `nextMonth delegates to monthRepo`() = runTest {
        vm.nextMonth()
        verify { monthRepo.nextMonth() }
    }

    @Test
    fun `goToMonth delegates to monthRepo`() = runTest {
        vm.goToMonth(2024, 11)
        verify { monthRepo.goToMonth(2024, 11) }
    }

    @Test
    fun `add delegates to catRepo save`() = runTest {
        vm.add("Нова", TransactionType.EXPENSE, "#FF0000", "home")
        coVerify { catRepo.save(match { it.name == "Нова" && it.type == TransactionType.EXPENSE && it.colorHex == "#FF0000" }) }
    }

    @Test
    fun `update delegates to catRepo update`() = runTest {
        val cat = CategoryEntity(id = 1L, name = "Test", type = TransactionType.EXPENSE, colorHex = "#000", icon = "home")
        vm.update(cat)
        coVerify { catRepo.update(cat) }
    }

    @Test
    fun `delete delegates to catRepo delete`() = runTest {
        val cat = CategoryEntity(id = 1L, name = "Test", type = TransactionType.EXPENSE, colorHex = "#000", icon = "home")
        vm.delete(cat)
        coVerify { catRepo.delete(cat) }
    }

    @Test
    fun `recordTransaction calls txRepo addTransaction with correct type`() = runTest {
        val cat = CategoryEntity(id = 3L, name = "Продукти", type = TransactionType.EXPENSE, colorHex = "#03A9F4", icon = "shopping")
        vm.recordTransaction(accountId = 1L, category = cat, amount = 200.0, note = "Моноліт")

        coVerify {
            txRepo.addTransaction(match {
                it.accountId == 1L &&
                it.categoryId == 3L &&
                it.amount == 200.0 &&
                it.type == TransactionType.EXPENSE &&
                it.note == "Моноліт"
            })
        }
    }

    @Test
    fun `state reacts to month changes via flatMapLatest`() = runTest {
        vm.state.test {
            awaitItem() // initial

            // Simulate month change
            monthFlow.value = AppMonth(2025, 5)
            awaitItem() // state re-emitted for new month

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pillLabel comes from monthRepo`() = runTest {
        every { monthRepo.pillLabel(any()) } returns "ЧЕРВЕНЬ 2025"
        monthFlow.value = AppMonth(2025, 5)

        vm = CategoriesViewModel(catRepo, txRepo, monthRepo, accountRepo)
        vm.state.test {
            val state = awaitItem()
            assertEquals("ЧЕРВЕНЬ 2025", state.pillLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
