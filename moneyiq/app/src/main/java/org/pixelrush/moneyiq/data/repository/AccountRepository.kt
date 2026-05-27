package org.pixelrush.moneyiq.data.repository

import kotlinx.coroutines.flow.Flow
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(private val dao: AccountDao) {

    fun getAllAccounts(): Flow<List<AccountEntity>> = dao.getAllAccounts()

    fun getTotalBalance(): Flow<Double?> = dao.getTotalBalance()

    suspend fun getById(id: Long): AccountEntity? = dao.getAccountById(id)

    suspend fun save(account: AccountEntity): Long = dao.insertAccount(account)

    suspend fun update(account: AccountEntity) = dao.updateAccount(account)

    suspend fun delete(account: AccountEntity) = dao.deleteAccount(account)
}
