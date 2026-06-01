package org.syalosovetskyi.onemoney.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalOneMoneyColors     = staticCompositionLocalOf { OneMoneyLightTokens.colors }
val LocalOneMoneyTypography = staticCompositionLocalOf { OneMoneyLightTokens.typography }
val LocalOneMoneyDimens     = staticCompositionLocalOf { OneMoneyLightTokens.dimens }

object OneMoneyTheme {
    val colors: OneMoneyColors
        @Composable get() = LocalOneMoneyColors.current

    val typography: OneMoneyTypography
        @Composable get() = LocalOneMoneyTypography.current

    val dimens: OneMoneyDimens
        @Composable get() = LocalOneMoneyDimens.current
}

@Composable
fun OneMoneyThemeProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOneMoneyColors     provides OneMoneyLightTokens.colors,
        LocalOneMoneyTypography provides OneMoneyLightTokens.typography,
        LocalOneMoneyDimens     provides OneMoneyLightTokens.dimens,
        content = content,
    )
}
