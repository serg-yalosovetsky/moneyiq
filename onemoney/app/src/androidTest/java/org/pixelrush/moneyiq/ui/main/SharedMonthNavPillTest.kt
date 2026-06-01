package org.syalosovetskyi.onemoney.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.syalosovetskyi.onemoney.data.repository.AppMonth
import org.syalosovetskyi.onemoney.data.repository.PeriodMode
import org.syalosovetskyi.onemoney.ui.theme.onemoneyTheme

class SharedMonthNavPillTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersMonthLabelBadgeAndPeriodSheetOptions() {
        compose.setContent {
            onemoneyTheme {
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
            onemoneyTheme {
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
