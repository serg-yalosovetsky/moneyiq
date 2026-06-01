package org.pixelrush.moneyiq.ui

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.MainActivity
import org.pixelrush.moneyiq.data.db.AppDatabase
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import javax.inject.Inject

@HiltAndroidTest
class MainActivityE2ETest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var db: AppDatabase

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hilt.inject()
        runBlocking {
            db.transactionDao().deleteAllTransactions()
            db.categoryDao().deleteAllCategories()
            db.accountDao().deleteAllAccounts()

            db.accountDao().insertAccount(
                AccountEntity(
                    id = 1,
                    name = "Тестова карта",
                    type = AccountType.CARD,
                    balance = 1000.0,
                    currency = "UAH",
                    colorHex = "#3949AB",
                    icon = "credit_card",
                    includeInTotal = true,
                    isDefault = true
                )
            )
            db.categoryDao().insertCategory(
                CategoryEntity(
                    id = 10,
                    name = "Продукти",
                    type = TransactionType.EXPENSE,
                    colorHex = "#4AAFE8",
                    icon = "grocery",
                    isDefault = true,
                    sortOrder = 1
                )
            )
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun quickExpenseFromTransactionsTabPersistsAndUpdatesBalance() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        compose.onNodeWithText("Операції").performClick()
        compose.onNodeWithContentDescription("Нова транзакція").performClick()
        compose.onNodeWithText("Продукти").performClick()

        compose.onNodeWithTag("calc_key_1").performClick()
        compose.onNodeWithTag("calc_key_2").performClick()
        compose.onNodeWithTag("calc_key_3").performClick()
        compose.onNodeWithContentDescription("Зберегти транзакцію").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking { db.transactionDao().getAllTransactions().isNotEmpty() }
        }

        check(compose.onAllNodesWithText("Продукти").fetchSemanticsNodes().isNotEmpty()) {
            "Expected transaction category to be visible"
        }

        val account = runBlocking { db.accountDao().getAccountById(1) }
        val tx = runBlocking { db.transactionDao().getAllTransactions() }.single()

        check(account?.balance == 877.0) { "Expected balance 877.0, got ${account?.balance}" }
        check(tx.amount == 123.0) { "Expected amount 123.0, got ${tx.amount}" }
        check(tx.categoryId == 10L) { "Expected category 10, got ${tx.categoryId}" }
        check(tx.accountId == 1L) { "Expected account 1, got ${tx.accountId}" }
        check(tx.type == TransactionType.EXPENSE) { "Expected EXPENSE, got ${tx.type}" }
    }
}
