package org.pixelrush.moneyiq.ui.accounts

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.repository.AccountRepository
import org.pixelrush.moneyiq.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: AccountRepository = mockk(relaxed = true)
    private lateinit var vm: AccountsViewModel

    @Before
    fun setup() {
        every { repo.getAllAccounts() } returns flowOf(emptyList())
        every { repo.getTotalBalance() } returns flowOf(null)
        vm = AccountsViewModel(repo)
    }

    @Test
    fun `initial state has empty accounts and zero balance`() = runTest {
        vm.state.test {
            val state = awaitItem()
            assertTrue(state.accounts.isEmpty())
            assertEquals(0.0, state.totalBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects accounts from repo`() = runTest {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "Cash", balance = 500.0),
            AccountEntity(id = 2L, name = "Card", balance = 1000.0)
        )
        every { repo.getAllAccounts() } returns flowOf(accounts)
        every { repo.getTotalBalance() } returns flowOf(1500.0)
        vm = AccountsViewModel(repo)

        vm.state.test {
            val state = awaitItem()
            assertEquals(2, state.accounts.size)
            assertEquals(1500.0, state.totalBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `add first account sets isDefault true`() = runTest {
        every { repo.getAllAccounts() } returns flowOf(emptyList())
        coEvery { repo.save(any()) } returns 1L
        vm = AccountsViewModel(repo)

        vm.add("Wallet", AccountType.CASH, 0.0, "#4CAF50")

        coVerify { repo.save(match { it.isDefault }) }
    }

    @Test
    fun `add second account sets isDefault false`() = runTest {
        val existing = listOf(AccountEntity(id = 1L, name = "Existing", balance = 0.0, isDefault = true))
        every { repo.getAllAccounts() } returns flowOf(existing)
        coEvery { repo.save(any()) } returns 2L
        vm = AccountsViewModel(repo)

        vm.add("Second", AccountType.CARD, 0.0, "#1E88E5")

        coVerify { repo.save(match { !it.isDefault }) }
    }

    @Test
    fun `add passes correct name and type`() = runTest {
        every { repo.getAllAccounts() } returns flowOf(emptyList())
        coEvery { repo.save(any()) } returns 1L
        vm = AccountsViewModel(repo)

        vm.add("MyAccount", AccountType.SAVING, 200.0, "#FF5722")

        coVerify { repo.save(match { it.name == "MyAccount" && it.type == AccountType.SAVING && it.balance == 200.0 }) }
    }

    @Test
    fun `update delegates to repo`() = runTest {
        val account = AccountEntity(id = 1L, name = "Test", balance = 100.0)
        vm.update(account)
        coVerify { repo.update(account) }
    }

    @Test
    fun `delete delegates to repo`() = runTest {
        val account = AccountEntity(id = 1L, name = "Test", balance = 0.0)
        vm.delete(account)
        coVerify { repo.delete(account) }
    }

    @Test
    fun `setDefault calls repo setDefault with account id`() = runTest {
        val account = AccountEntity(id = 5L, name = "Test", balance = 0.0)
        vm.setDefault(account)
        coVerify { repo.setDefault(5L) }
    }

    @Test
    fun `state totalBalance handles null from dao`() = runTest {
        every { repo.getTotalBalance() } returns flowOf(null)
        vm = AccountsViewModel(repo)

        vm.state.test {
            val state = awaitItem()
            assertEquals(0.0, state.totalBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
