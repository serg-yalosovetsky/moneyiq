package org.syalosovetskyi.onemoney.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.syalosovetskyi.onemoney.data.db.dao.AccountDao
import org.syalosovetskyi.onemoney.data.db.entities.AccountEntity

class AccountRepositoryTest {

    private val dao: AccountDao = mockk(relaxed = true)
    private val ratesRepo: CurrencyRatesRepository = mockk(relaxed = true)
    private lateinit var repo: AccountRepository

    @Before
    fun setup() {
        repo = AccountRepository(dao, ratesRepo)
    }

    @Test
    fun `setDefault calls clearAllDefault then setDefault in order`() = runTest {
        repo.setDefault(5L)
        coVerifyOrder {
            dao.clearAllDefault()
            dao.setDefault(5L)
        }
    }

    @Test
    fun `setDefault passes correct id`() = runTest {
        repo.setDefault(42L)
        coVerify { dao.setDefault(42L) }
    }

    @Test
    fun `save delegates to dao insertAccount`() = runTest {
        val account = AccountEntity(name = "Test", balance = 100.0)
        coEvery { dao.insertAccount(any()) } returns 1L
        repo.save(account)
        coVerify { dao.insertAccount(account) }
    }

    @Test
    fun `save returns id from dao`() = runTest {
        val account = AccountEntity(name = "Test", balance = 100.0)
        coEvery { dao.insertAccount(any()) } returns 7L
        val id = repo.save(account)
        assertEquals(7L, id)
    }

    @Test
    fun `update delegates to dao updateAccount`() = runTest {
        val account = AccountEntity(id = 1L, name = "Updated", balance = 200.0)
        repo.update(account)
        coVerify { dao.updateAccount(account) }
    }

    @Test
    fun `delete delegates to dao deleteAccount`() = runTest {
        val account = AccountEntity(id = 1L, name = "Test", balance = 0.0)
        repo.delete(account)
        coVerify { dao.deleteAccount(account) }
    }

    @Test
    fun `getAllAccounts returns flow from dao`() {
        val flow = flowOf(emptyList<AccountEntity>())
        every { dao.getAllAccounts() } returns flow
        val result = repo.getAllAccounts()
        assertEquals(flow, result)
    }

    @Test
    fun `getTotalBalance converts balances to UAH via NBU rates`() = runTest {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "UAH", balance = 100.0, currency = "UAH"),
            AccountEntity(id = 2L, name = "USD", balance = 10.0,  currency = "USD")
        )
        every { dao.getAllAccounts() } returns flowOf(accounts)
        every { ratesRepo.rates } returns flowOf(mapOf("UAH" to 1.0, "USD" to 40.0))
        val result = repo.getTotalBalance().first()
        assertEquals(100.0 + 10.0 * 40.0, result!!, 0.001)
    }

    @Test
    fun `getTotalBalance excludes accounts with includeInTotal false`() = runTest {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "In",  balance = 100.0, currency = "UAH", includeInTotal = true),
            AccountEntity(id = 2L, name = "Out", balance = 999.0, currency = "UAH", includeInTotal = false)
        )
        every { dao.getAllAccounts() } returns flowOf(accounts)
        every { ratesRepo.rates } returns flowOf(mapOf("UAH" to 1.0))
        val result = repo.getTotalBalance().first()
        assertEquals(100.0, result!!, 0.001)
    }

    @Test
    fun `getById delegates to dao getAccountById`() = runTest {
        val account = AccountEntity(id = 3L, name = "Found", balance = 100.0)
        coEvery { dao.getAccountById(3L) } returns account
        val result = repo.getById(3L)
        assertEquals(account, result)
    }
}
