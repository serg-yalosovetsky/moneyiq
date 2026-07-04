package org.syalosovetskyi.onemoney.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.syalosovetskyi.onemoney.data.db.dao.TransactionWithDetails
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType
import org.syalosovetskyi.onemoney.ui.theme.onemoneyTheme

class TransactionListItemUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expenseWithoutCategoryUsesDefaultLabelAndAccountDetails() {
        compose.setContent {
            onemoneyTheme {
                TransactionListItem(
                    tx = TransactionWithDetails(
                        id = 1,
                        type = TransactionType.EXPENSE,
                        amount = 99.5,
                        accountId = 10,
                        accountName = "Гаманець",
                        accountColor = "#4CAF50",
                        accountCurrency = "UAH",
                        toAccountId = null,
                        toAccountName = null,
                        categoryId = null,
                        categoryName = null,
                        categoryColor = null,
                        categoryIcon = null,
                        note = "кава",
                        date = 1_717_286_400_000
                    ),
                    onClick = {}
                )
            }
        }

        compose.onNodeWithText("Витрата").assertIsDisplayed()
        compose.onNodeWithText("кава", substring = true).fetchSemanticsNode()
        compose.onNodeWithText("Гаманець", substring = true).fetchSemanticsNode()
    }

    @Test
    fun transferShowsBothAccountsInSupportingText() {
        compose.setContent {
            onemoneyTheme {
                TransactionListItem(
                    tx = TransactionWithDetails(
                        id = 2,
                        type = TransactionType.TRANSFER,
                        amount = 250.0,
                        accountId = 10,
                        accountName = "Карта",
                        accountColor = "#4CAF50",
                        accountCurrency = "UAH",
                        toAccountId = 11,
                        toAccountName = "Готівка",
                        categoryId = null,
                        categoryName = null,
                        categoryColor = null,
                        categoryIcon = null,
                        note = "",
                        date = 1_717_286_400_000
                    ),
                    onClick = {}
                )
            }
        }

        compose.onNodeWithText("Перевід").assertIsDisplayed()
        compose.onNodeWithText("Карта", substring = true).fetchSemanticsNode()
        compose.onNodeWithText("Готівка", substring = true).fetchSemanticsNode()
    }
}
