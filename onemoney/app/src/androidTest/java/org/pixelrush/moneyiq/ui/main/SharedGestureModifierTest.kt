package org.syalosovetskyi.onemoney.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.syalosovetskyi.onemoney.ui.theme.onemoneyTheme

class SharedGestureModifierTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun horizontalSwipe_ignoresEdgeAndVerticalSwipes() {
        var left = 0
        var right = 0

        compose.setContent {
            onemoneyTheme {
                Box(
                    Modifier
                        .size(width = 320.dp, height = 220.dp)
                        .testTag("surface")
                        .horizontalSwipe(
                            onSwipeLeft = { left++ },
                            onSwipeRight = { right++ }
                        )
                )
            }
        }

        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(width * 0.50f, height * 0.30f),
                end = Offset(width * 0.52f, height * 0.85f)
            )
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(10f, height / 2f),
                end = Offset(width * 0.80f, height / 2f)
            )
        }

        compose.runOnIdle {
            assertEquals(0, left)
            assertEquals(0, right)
        }
    }

    @Test
    fun horizontalSwipe_triggersCentralLeftAndRightSwipes() {
        var left = 0
        var right = 0

        compose.setContent {
            onemoneyTheme {
                Box(
                    Modifier
                        .size(width = 360.dp, height = 220.dp)
                        .testTag("surface")
                        .horizontalSwipe(
                            onSwipeLeft = { left++ },
                            onSwipeRight = { right++ }
                        )
                )
            }
        }

        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(width * 0.72f, height / 2f),
                end = Offset(width * 0.28f, height / 2f)
            )
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(width * 0.28f, height / 2f),
                end = Offset(width * 0.72f, height / 2f)
            )
        }

        compose.runOnIdle {
            assertEquals(1, left)
            assertEquals(1, right)
        }
    }

    @Test
    fun edgeSwipe_triggersOnlyFromEdges() {
        var opened = 0
        var backed = 0

        compose.setContent {
            onemoneyTheme {
                Box(
                    Modifier
                        .size(width = 360.dp, height = 220.dp)
                        .testTag("surface")
                        .edgeSwipe(
                            onLeftEdge = { opened++ },
                            onRightEdge = { backed++ }
                        )
                )
            }
        }

        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(width * 0.50f, height / 2f),
                end = Offset(width * 0.10f, height / 2f)
            )
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(8f, height / 2f),
                end = Offset(width * 0.70f, height / 2f)
            )
        }
        compose.onNodeWithTag("surface").performTouchInput {
            swipe(
                start = Offset(width - 8f, height / 2f),
                end = Offset(width * 0.30f, height / 2f)
            )
        }

        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, backed)
        }
    }
}
