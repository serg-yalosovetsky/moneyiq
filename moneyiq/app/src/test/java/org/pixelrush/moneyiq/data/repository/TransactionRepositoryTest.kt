package org.pixelrush.moneyiq.data.repository

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import org.pixelrush.moneyiq.data.db.entities.TransactionEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType

class TransactionRepositoryTest {

    private val txDao: TransactionDao = mockk(relaxed = true)
    private val accountDao: AccountDao = mockk(relaxed = true)
    private lateinit var repo: TransactionRepository

    @Before
    fun setup() {
        repo = TransactionRepository(txDao, accountDao)
    }

    // ── addTransaction ───────────────────────────────────────────────────────

    @Test
    fun `addIncome increases account balance`() = runTest {
        val tx = TransactionEntity(type = TransactionType.INCOME, amount = 100.0, accountId = 1L)
        repo.addTransaction(tx)
        coVerify { txDao.insertTransaction(tx) }
        coVerify { accountDao.updateBalance(1L, +100.0) }
    }

    @Test
    fun `addExpense decreases account balance`() = runTest {
        val tx = TransactionEntity(type = TransactionType.EXPENSE, amount = 50.0, accountId = 1L)
        repo.addTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -50.0) }
    }

    @Test
    fun `addTransfer decreases from and increases to account`() = runTest {
        val tx = TransactionEntity(type = TransactionType.TRANSFER, amount = 200.0, accountId = 1L, toAccountId = 2L)
        repo.addTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -200.0) }
        coVerify { accountDao.updateBalance(2L, +200.0) }
    }

    @Test
    fun `addBorrow increases account balance`() = runTest {
        val tx = TransactionEntity(type = TransactionType.BORROW, amount = 300.0, accountId = 1L)
        repo.addTransaction(tx)
        coVerify { accountDao.updateBalance(1L, +300.0) }
    }

    @Test
    fun `addLend decreases account balance`() = runTest {
        val tx = TransactionEntity(type = TransactionType.LEND, amount = 150.0, accountId = 1L)
        repo.addTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -150.0) }
    }

    @Test
    fun `addRepay with toAccountId updates both accounts`() = runTest {
        val tx = TransactionEntity(type = TransactionType.REPAY, amount = 500.0, accountId = 1L, toAccountId = 2L)
        repo.addTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -500.0) }
        coVerify { accountDao.updateBalance(2L, +500.0) }
    }

    @Test
    fun `addRepay without toAccountId updates only from account`() = runTest {
        val tx = TransactionEntity(type = TransactionType.REPAY, amount = 500.0, accountId = 1L, toAccountId = null)
        repo.addTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -500.0) }
        coVerify(exactly = 1) { accountDao.updateBalance(any(), any()) }
    }

    // ── deleteTransaction ────────────────────────────────────────────────────

    @Test
    fun `deleteIncome reverses balance increase`() = runTest {
        val tx = TransactionEntity(type = TransactionType.INCOME, amount = 100.0, accountId = 1L)
        repo.deleteTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -100.0) }
        coVerify { txDao.deleteTransaction(tx) }
    }

    @Test
    fun `deleteExpense reverses balance decrease`() = runTest {
        val tx = TransactionEntity(type = TransactionType.EXPENSE, amount = 50.0, accountId = 1L)
        repo.deleteTransaction(tx)
        coVerify { accountDao.updateBalance(1L, +50.0) }
    }

    @Test
    fun `deleteTransfer reverses both balance changes`() = runTest {
        val tx = TransactionEntity(type = TransactionType.TRANSFER, amount = 200.0, accountId = 1L, toAccountId = 2L)
        repo.deleteTransaction(tx)
        coVerify { accountDao.updateBalance(1L, +200.0) }
        coVerify { accountDao.updateBalance(2L, -200.0) }
    }

    @Test
    fun `deleteBorrow reverses balance increase`() = runTest {
        val tx = TransactionEntity(type = TransactionType.BORROW, amount = 300.0, accountId = 1L)
        repo.deleteTransaction(tx)
        coVerify { accountDao.updateBalance(1L, -300.0) }
    }

    @Test
    fun `deleteLend reverses balance decrease`() = runTest {
        val tx = TransactionEntity(type = TransactionType.LEND, amount = 150.0, accountId = 1L)
        repo.deleteTransaction(tx)
        coVerify { accountDao.updateBalance(1L, +150.0) }
    }

    @Test
    fun `deleteRepay with toAccountId reverses both accounts`() = runTest {
        val tx = TransactionEntity(type = TransactionType.REPAY, amount = 500.0, accountId = 1L, toAccountId = 2L)
        repo.deleteTransaction(tx)
        coVerify { accountDao.updateBalance(1L, +500.0) }
        coVerify { accountDao.updateBalance(2L, -500.0) }
    }

    // ── updateTransaction ────────────────────────────────────────────────────

    @Test
    fun `updateTransaction reverts old income and applies new expense`() = runTest {
        val old = TransactionEntity(id = 1L, type = TransactionType.INCOME, amount = 100.0, accountId = 1L)
        val new = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 200.0, accountId = 1L)
        repo.updateTransaction(old, new)
        coVerify { accountDao.updateBalance(1L, -100.0) } // revert income
        coVerify { accountDao.updateBalance(1L, -200.0) } // apply expense
        coVerify { txDao.updateTransaction(new) }
    }

    @Test
    fun `updateTransaction with account change uses correct accounts`() = runTest {
        val old = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 50.0, accountId = 1L)
        val new = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 50.0, accountId = 2L)
        repo.updateTransaction(old, new)
        coVerify { accountDao.updateBalance(1L, +50.0) } // revert old expense
        coVerify { accountDao.updateBalance(2L, -50.0) } // apply new expense
    }

    @Test
    fun `updateTransaction with amount change applies correct delta`() = runTest {
        val old = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 100.0, accountId = 1L)
        val new = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 150.0, accountId = 1L)
        repo.updateTransaction(old, new)
        coVerify { accountDao.updateBalance(1L, +100.0) }
        coVerify { accountDao.updateBalance(1L, -150.0) }
    }

    @Test
    fun `updateTransaction follows revert-then-apply order`() = runTest {
        val old = TransactionEntity(id = 1L, type = TransactionType.INCOME, amount = 50.0, accountId = 1L)
        val new = TransactionEntity(id = 1L, type = TransactionType.INCOME, amount = 80.0, accountId = 1L)
        repo.updateTransaction(old, new)
        coVerifyOrder {
            accountDao.updateBalance(1L, -50.0) // revert old
            txDao.updateTransaction(new)        // persist
            accountDao.updateBalance(1L, +80.0) // apply new
        }
    }

    @Test
    fun `updateTransaction old transfer toAccount changes correctly`() = runTest {
        val old = TransactionEntity(id = 1L, type = TransactionType.TRANSFER, amount = 100.0, accountId = 1L, toAccountId = 2L)
        val new = TransactionEntity(id = 1L, type = TransactionType.TRANSFER, amount = 100.0, accountId = 1L, toAccountId = 3L)
        repo.updateTransaction(old, new)
        coVerify { accountDao.updateBalance(2L, -100.0) } // revert old to
        coVerify { accountDao.updateBalance(3L, +100.0) } // apply new to
    }
}
