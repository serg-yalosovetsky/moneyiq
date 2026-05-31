package org.pixelrush.moneyiq.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.pixelrush.moneyiq.data.repository.AppMonth
import org.pixelrush.moneyiq.data.repository.PeriodMode
import org.pixelrush.moneyiq.ui.theme.MoneyIQTheme

class SharedMonthNavPillTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersMonthLabelBadgeAndPeriodSheetOptions() {
        compose.setContent {
            MoneyIQTheme {
                SharedMonthNavPill(
                    appMonth = AppMonth(2026, 4, PeriodMode.MONTH),
                    daysInPeriod = 31,
                    onPrev = {},
                    onNext = {}
                )
            }
        }

        compose.onNodeWithText("ТРАВЕНЬ 2026").assertIsDisplayed()
        compose.onNodeWithText("31").assertIsDisplayed()

        compose.onNodeWithTag("month_pill").performClick()

        compose.onNodeWithText("Період").assertIsDisplayed()
        compose.onNodeWithText("Весь час").assertIsDisplayed()
        compose.onNodeWithText("Виберіть день").assertIsDisplayed()
        compose.onNodeWithText("Тиждень").assertIsDisplayed()
        compose.onNodeWithText("Місяць").assertIsDisplayed()
    }

    @Test
    fun prevNextButtonsDispatchCallbacks() {
        var prev = 0
        var next = 0

        compose.setContent {
            MoneyIQTheme {
                SharedMonthNavPill(
                    appMonth = AppMonth(2026, 4),
                    daysInPeriod = 31,
                    onPrev = { prev++ },
                    onNext = { next++ }
                )
            }
        }

        compose.onNodeWithTag("month_pill_prev").performClick()
        compose.onNodeWithTag("month_pill_next").performClick()

        compose.runOnIdle {
            assertEquals(1, prev)
            assertEquals(1, next)
        }
    }
}
