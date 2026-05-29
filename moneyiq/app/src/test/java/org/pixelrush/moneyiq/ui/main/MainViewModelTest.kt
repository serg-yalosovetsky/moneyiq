package org.pixelrush.moneyiq.ui.main

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.data.repository.TransactionRepository
import org.pixelrush.moneyiq.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepo: AccountRepository = mockk(relaxed = true)
    private val txRepo: TransactionRepository = mockk(relaxed = true)

    private fun buildVm(): MainViewModel {
        return MainViewModel(accountRepo, txRepo)
    }

    @Test
    fun `isLoading is true before any flow emits`() = runTest {
        // emptyFlow never emits, so combine never fires — _state stays at initial isLoading=true
        every { accountRepo.getTotalBalance() } returns emptyFlow()
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading is false after all flows emit`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(0.0)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state contains accounts from repo`() = runTest {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "Готівка", balance = 500.0),
            AccountEntity(id = 2L, name = "Картка", balance = 1000.0)
        )
        every { accountRepo.getTotalBalance() } returns flowOf(1500.0)
        every { accountRepo.getAllAccounts() } returns flowOf(accounts)
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertEquals(2, state.accounts.size)
            assertEquals("Готівка", state.accounts[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totalBalance reflects repo value`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(3500.0)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertEquals(3500.0, state.totalBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totalBalance handles null from dao`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(null)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertEquals(0.0, state.totalBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `monthIncome reflects txRepo income sum`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(0.0)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(12500.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertEquals(12500.0, state.monthIncome, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `monthExpense reflects txRepo expense sum`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(0.0)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(4800.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertEquals(4800.0, state.monthExpense, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recentTransactions reflects txRepo recent list`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(0.0)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(20) } returns flowOf(emptyList())

        val vm = buildVm()
        vm.state.test {
            val state = awaitItem()
            assertTrue(state.recentTransactions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRecentTransactions is called with limit 20`() = runTest {
        every { accountRepo.getTotalBalance() } returns flowOf(0.0)
        every { accountRepo.getAllAccounts() } returns flowOf(emptyList())
        every { txRepo.getIncomeSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getExpenseSum(any(), any()) } returns flowOf(0.0)
        every { txRepo.getRecentTransactions(any()) } returns flowOf(emptyList())

        buildVm()

        verify { txRepo.getRecentTransactions(20) }
    }
}
