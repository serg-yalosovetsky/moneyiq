package org.pixelrush.moneyiq.data.repository

import kotlinx.coroutines.flow.Flow
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategorySpending
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import org.pixelrush.moneyiq.data.db.dao.TransactionWithDetails
import org.pixelrush.moneyiq.data.db.entities.TransactionEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {
    fun getRecentTransactions(limit: Int = 50): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsPaged(limit)

    fun getTransactionsByPeriod(from: Long, to: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsByDateRange(from, to)

    fun getIncomeSum(from: Long, to: Long): Flow<Double> =
        transactionDao.getSumByTypeAndPeriod(TransactionType.INCOME, from, to)

    fun getExpenseSum(from: Long, to: Long): Flow<Double> =
        transactionDao.getSumByTypeAndPeriod(TransactionType.EXPENSE, from, to)

    fun getCategorySpending(type: TransactionType, from: Long, to: Long): Flow<List<CategorySpending>> =
        transactionDao.getCategorySpending(type, from, to)

    suspend fun addTransaction(tx: TransactionEntity) {
        transactionDao.insertTransaction(tx)
        when (tx.type) {
            TransactionType.INCOME   -> accountDao.updateBalance(tx.accountId, +tx.amount)
            TransactionType.EXPENSE  -> accountDao.updateBalance(tx.accountId, -tx.amount)
            TransactionType.BORROW   -> accountDao.updateBalance(tx.accountId, +tx.amount)  // деньги пришли
            TransactionType.LEND     -> accountDao.updateBalance(tx.accountId, -tx.amount)  // деньги ушли
            TransactionType.REPAY    -> {
                // Если toAccountId задан — гасится долг другого счёта, иначе уменьшается основной
                accountDao.updateBalance(tx.accountId, -tx.amount)
                tx.toAccountId?.let { accountDao.updateBalance(it, +tx.amount) }
            }
            TransactionType.TRANSFER -> {
                accountDao.updateBalance(tx.accountId, -tx.amount)
                tx.toAccountId?.let { accountDao.updateBalance(it, +tx.amount) }
            }
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        when (tx.type) {
            TransactionType.INCOME   -> accountDao.updateBalance(tx.accountId, -tx.amount)
            TransactionType.EXPENSE  -> accountDao.updateBalance(tx.accountId, +tx.amount)
            TransactionType.BORROW   -> accountDao.updateBalance(tx.accountId, -tx.amount)
            TransactionType.LEND     -> accountDao.updateBalance(tx.accountId, +tx.amount)
            TransactionType.REPAY    -> {
                accountDao.updateBalance(tx.accountId, +tx.amount)
                tx.toAccountId?.let { accountDao.updateBalance(it, -tx.amount) }
            }
            TransactionType.TRANSFER -> {
                accountDao.updateBalance(tx.accountId, +tx.amount)
                tx.toAccountId?.let { accountDao.updateBalance(it, -tx.amount) }
            }
        }
        transactionDao.deleteTransaction(tx)
    }

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getTransactionById(id)

    /** Откатывает балансовые изменения старой транзакции, применяет новые. */
    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        // 1. Откатить старые изменения баланса
        when (oldTx.type) {
            TransactionType.INCOME   -> accountDao.updateBalance(oldTx.accountId, -oldTx.amount)
            TransactionType.EXPENSE  -> accountDao.updateBalance(oldTx.accountId, +oldTx.amount)
            TransactionType.BORROW   -> accountDao.updateBalance(oldTx.accountId, -oldTx.amount)
            TransactionType.LEND     -> accountDao.updateBalance(oldTx.accountId, +oldTx.amount)
            TransactionType.REPAY    -> {
                accountDao.updateBalance(oldTx.accountId, +oldTx.amount)
                oldTx.toAccountId?.let { accountDao.updateBalance(it, -oldTx.amount) }
            }
            TransactionType.TRANSFER -> {
                accountDao.updateBalance(oldTx.accountId, +oldTx.amount)
                oldTx.toAccountId?.let { accountDao.updateBalance(it, -oldTx.amount) }
            }
        }
        // 2. Записать новую версию
        transactionDao.updateTransaction(newTx)
        // 3. Применить новые изменения баланса
        when (newTx.type) {
            TransactionType.INCOME   -> accountDao.updateBalance(newTx.accountId, +newTx.amount)
            TransactionType.EXPENSE  -> accountDao.updateBalance(newTx.accountId, -newTx.amount)
            TransactionType.BORROW   -> accountDao.updateBalance(newTx.accountId, +newTx.amount)
            TransactionType.LEND     -> accountDao.updateBalance(newTx.accountId, -newTx.amount)
            TransactionType.REPAY    -> {
                accountDao.updateBalance(newTx.accountId, -newTx.amount)
                newTx.toAccountId?.let { accountDao.updateBalance(it, +newTx.amount) }
            }
            TransactionType.TRANSFER -> {
                accountDao.updateBalance(newTx.accountId, -newTx.amount)
                newTx.toAccountId?.let { accountDao.updateBalance(it, +newTx.amount) }
            }
        }
    }
}
