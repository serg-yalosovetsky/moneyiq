package org.pixelrush.moneyiq.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.entities.AccountEntity

class AccountRepositoryTest {

    private val dao: AccountDao = mockk(relaxed = true)
    private lateinit var repo: AccountRepository

    @Before
    fun setup() {
        repo = AccountRepository(dao)
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
    fun `getTotalBalance returns flow from dao`() {
        val flow = flowOf(500.0)
        every { dao.getTotalBalance() } returns flow
        val result = repo.getTotalBalance()
        assertEquals(flow, result)
    }

    @Test
    fun `getById delegates to dao getAccountById`() = runTest {
        val account = AccountEntity(id = 3L, name = "Found", balance = 100.0)
        coEvery { dao.getAccountById(3L) } returns account
        val result = repo.getById(3L)
        assertEquals(account, result)
    }
}
