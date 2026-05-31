package org.pixelrush.moneyiq.ui.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.data.repository.SettingsRepository
import org.pixelrush.moneyiq.util.BackupSerializer
import org.pixelrush.moneyiq.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class DataViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val txDao: TransactionDao = mockk(relaxed = true)
    private val accountDao: AccountDao = mockk(relaxed = true)
    private val categoryDao: CategoryDao = mockk(relaxed = true)
    private val settingsRepo: SettingsRepository = mockk(relaxed = true)

    private fun buildVm() = DataViewModel(txDao, accountDao, categoryDao, settingsRepo)

    @Test
    fun `buildExportJson serializes all dao data`() = runTest {
        coEvery { accountDao.getAllAccountsOnce() } returns listOf(
            AccountEntity(id = 1L, name = "Картка", balance = 100.0, creditLimit = 500.0)
        )
        coEvery { categoryDao.getAllCategoriesOnce() } returns listOf(
            CategoryEntity(id = 2L, name = "Продукти", type = TransactionType.EXPENSE, currencyCode = "EUR")
        )
        coEvery { txDao.getAllTransactions() } returns listOf(
            TransactionEntity(
                id = 3L,
                type = TransactionType.EXPENSE,
                amount = 25.0,
                accountId = 1L,
                categoryId = 2L,
                repeatMode = "MONTHLY",
                reminderMode = "1_DAY"
            )
        )

        val restored = BackupSerializer.deserialize(buildVm().buildExportJson())

        assertEquals(1, restored.accounts.size)
        assertEquals(500.0, restored.accounts[0].creditLimit, 0.001)
        assertEquals("EUR", restored.categories[0].currencyCode)
        assertEquals("MONTHLY", restored.transactions[0].repeatMode)
        assertEquals("1_DAY", restored.transactions[0].reminderMode)
    }

    @Test
    fun `deleteAllData clears transactions accounts and categories`() = runTest {
        val vm = buildVm()

        vm.deleteAllData()

        coVerify(timeout = 1_000) {
            txDao.deleteAllTransactions()
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
        }
    }

    @Test
    fun `clearMessage removes visible message`() {
        val vm = buildVm()

        vm.clearMessage()

        assertTrue(vm.state.value.message == null)
    }
}
