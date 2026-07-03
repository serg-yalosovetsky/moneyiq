package org.syalosovetskyi.onemoney.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.syalosovetskyi.onemoney.data.db.dao.AccountDao
import org.syalosovetskyi.onemoney.data.db.entities.AccountEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
    private val ratesRepo: CurrencyRatesRepository
) {

    fun getAllAccounts(): Flow<List<AccountEntity>> = dao.getAllAccounts()

    /**
     * Загальний баланс усіх рахунків, приведений до гривні за курсами НБУ.
     * Раніше рахувалося як сирий SUM(balance) — валюти складалися без конвертації.
     * Рахунки у валюті без відомого курсу (напр. крипто) дають внесок 0.
     */
    fun getTotalBalance(): Flow<Double?> =
        combine(dao.getAllAccounts(), ratesRepo.rates) { accounts, rates ->
            accounts
                .filter { it.includeInTotal }
                .sumOf { it.balance * (rates[it.currency] ?: if (it.currency == "UAH") 1.0 else 0.0) }
        }

    suspend fun getById(id: Long): AccountEntity? = dao.getAccountById(id)

    suspend fun save(account: AccountEntity): Long = dao.insertAccount(account)

    suspend fun update(account: AccountEntity) = dao.updateAccount(account)

    suspend fun delete(account: AccountEntity) = dao.deleteAccount(account)

    suspend fun setDefault(accountId: Long) {
        dao.clearAllDefault()
        dao.setDefault(accountId)
    }
}
