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
fun OneMoneyThemeProvider(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors     = if (darkTheme) OneMoneyDarkTokens.colors     else OneMoneyLightTokens.colors
    val typography = if (darkTheme) OneMoneyDarkTokens.typography else OneMoneyLightTokens.typography
    val dimens     = if (darkTheme) OneMoneyDarkTokens.dimens     else OneMoneyLightTokens.dimens
    CompositionLocalProvider(
        LocalOneMoneyColors     provides colors,
        LocalOneMoneyTypography provides typography,
        LocalOneMoneyDimens     provides dimens,
        content = content,
    )
}
