package org.pixelrush.moneyiq.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.ui.theme.MoneyIQTheme

class SharedTopBarTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun actionIconMatchesCurrentPageContract() {
        val pages = listOf(
            0 to "Новий рахунок",
            1 to "Редагувати категорії",
            2 to "Пошук операцій",
            3 to "Налаштування бюджету",
            4 to "Налаштування"
        )

        pages.forEach { (page, description) ->
            compose.setContent {
                MoneyIQTheme {
                    SharedTopBar(
                        totalBalance = 1234.56,
                        currentPage = page,
                        onPlusClick = {}
                    )
                }
            }

            compose.onNodeWithText("Всі рахунки").assertIsDisplayed()
            compose.onNodeWithContentDescription(description).assertIsDisplayed()
        }
    }

    @Test
    fun actionIconDispatchesPageSpecificCallback() {
        var add = 0
        var edit = 0
        var search = 0
        var budget = 0
        var settings = 0

        fun render(page: Int) {
            compose.setContent {
                MoneyIQTheme {
                    SharedTopBar(
                        totalBalance = 0.0,
                        currentPage = page,
                        onPlusClick = { add++ },
                        onEditCategories = { edit++ },
                        onSearchTx = { search++ },
                        onBudgetSettings = { budget++ },
                        onSettings = { settings++ }
                    )
                }
            }
        }

        render(0)
        compose.onNodeWithContentDescription("Новий рахунок").performClick()
        render(1)
        compose.onNodeWithContentDescription("Редагувати категорії").performClick()
        render(2)
        compose.onNodeWithContentDescription("Пошук операцій").performClick()
        render(3)
        compose.onNodeWithContentDescription("Налаштування бюджету").performClick()
        render(4)
        compose.onNodeWithContentDescription("Налаштування").performClick()

        compose.runOnIdle {
            assertEquals(1, add)
            assertEquals(1, edit)
            assertEquals(1, search)
            assertEquals(1, budget)
            assertEquals(1, settings)
        }
    }
}
