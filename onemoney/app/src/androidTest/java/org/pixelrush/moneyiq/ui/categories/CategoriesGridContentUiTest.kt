package org.syalosovetskyi.onemoney.ui.categories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType
import org.syalosovetskyi.onemoney.ui.theme.onemoneyTheme

class CategoriesGridContentUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyStatePromptsToAddCategory() {
        compose.setContent {
            onemoneyTheme {
                CategoriesGridContent(
                    categories = emptyList(),
                    spending = emptyMap(),
                    totalExpense = 0.0,
                    totalIncome = 0.0,
                    selectedTab = 0,
                    onToggleTab = {},
                    bottomPadding = 0.dp,
                    onChipClick = {},
                    onAdd = {}
                )
            }
        }

        compose.onNodeWithText("Немає категорій").assertIsDisplayed()
        compose.onNodeWithText("Натисніть + щоб додати").assertIsDisplayed()
    }

    @Test
    fun categoriesAreSortedByCurrentPeriodSpending() {
        val food = category(1, "Їжа")
        val rent = category(2, "Оренда")
        val taxi = category(3, "Таксі")

        compose.setContent {
            onemoneyTheme {
                CategoriesGridContent(
                    categories = listOf(rent, taxi, food),
                    spending = mapOf(
                        food.id to 900.0,
                        rent.id to 300.0,
                        taxi.id to 600.0
                    ),
                    totalExpense = 1800.0,
                    totalIncome = 0.0,
                    selectedTab = 0,
                    onToggleTab = {},
                    bottomPadding = 0.dp,
                    onChipClick = {},
                    onAdd = {}
                )
            }
        }

        val foodLeft = compose.onNodeWithText("Їжа").fetchSemanticsNode().boundsInRoot.left
        val taxiLeft = compose.onNodeWithText("Таксі").fetchSemanticsNode().boundsInRoot.left
        val rentLeft = compose.onNodeWithText("Оренда").fetchSemanticsNode().boundsInRoot.left

        assertTrue(foodLeft < taxiLeft)
        assertTrue(taxiLeft < rentLeft)
    }

    private fun category(id: Long, name: String) = CategoryEntity(
        id = id,
        name = name,
        type = TransactionType.EXPENSE,
        colorHex = "#E53935",
        icon = "category"
    )
}
