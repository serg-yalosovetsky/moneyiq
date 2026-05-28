package org.pixelrush.moneyiq.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Semantic colors ──────────────────────────────────────────────────────────
val IncomeGreen      = Color(0xFF4CAF50)
val ExpenseRed       = Color(0xFFF44336)
val TransferBlue     = Color(0xFF2196F3)
val DebtOrange       = Color(0xFFFF9800)
val BudgetExpenseColor = Color(0xFFD81B60)
val BudgetIncomeColor  = Color(0xFF26A69A)

// ── 1Money colour palette (Material 3) ───────────────────────────────────────
// Light
private val md_light_primary             = Color(0xFF4D5C92)
private val md_light_onPrimary           = Color(0xFFFFFFFF)
private val md_light_primaryContainer    = Color(0xFFDCE1FF)
private val md_light_onPrimaryContainer  = Color(0xFF04164B)
private val md_light_secondary           = Color(0xFF595D72)
private val md_light_onSecondary         = Color(0xFFFFFFFF)
private val md_light_secondaryContainer  = Color(0xFFDEE1F9)
private val md_light_onSecondaryContainer= Color(0xFF161B2C)
private val md_light_tertiary            = Color(0xFF755470)
private val md_light_onTertiary          = Color(0xFFFFFFFF)
private val md_light_tertiaryContainer   = Color(0xFFFFD6F6)
private val md_light_onTertiaryContainer = Color(0xFF2D1029)
private val md_light_error              = Color(0xFFBA1A1A)
private val md_light_onError            = Color(0xFFFFFFFF)
private val md_light_errorContainer     = Color(0xFFFFDAD6)
private val md_light_onErrorContainer   = Color(0xFF410002)
private val md_light_background         = Color(0xFFFAF8FF)
private val md_light_onBackground       = Color(0xFF1A1B21)
private val md_light_surface            = Color(0xFFFAF8FF)
private val md_light_onSurface          = Color(0xFF1A1B21)
private val md_light_surfaceVariant     = Color(0xFFE2E1EC)
private val md_light_onSurfaceVariant   = Color(0xFF45464F)
private val md_light_outline            = Color(0xFF767680)
private val md_light_outlineVariant     = Color(0xFFC6C5D0)

// Dark
private val md_dark_primary             = Color(0xFFB6C4FF)
private val md_dark_onPrimary           = Color(0xFF1E2D60)
private val md_dark_primaryContainer    = Color(0xFF354378)
private val md_dark_onPrimaryContainer  = Color(0xFFDCE1FF)
private val md_dark_secondary           = Color(0xFFC2C5DD)
private val md_dark_onSecondary         = Color(0xFF2C2F42)
private val md_dark_secondaryContainer  = Color(0xFF414459)
private val md_dark_onSecondaryContainer= Color(0xFFDEE1F9)
private val md_dark_tertiary            = Color(0xFFE4B8DC)
private val md_dark_onTertiary          = Color(0xFF44243F)
private val md_dark_tertiaryContainer   = Color(0xFF5C3B57)
private val md_dark_onTertiaryContainer = Color(0xFFFFD6F6)
private val md_dark_error              = Color(0xFFFFB4AB)
private val md_dark_onError            = Color(0xFF690005)
private val md_dark_errorContainer     = Color(0xFF93000A)
private val md_dark_onErrorContainer   = Color(0xFFFFDAD6)
private val md_dark_background         = Color(0xFF121318)
private val md_dark_onBackground       = Color(0xFFE3E1EA)
private val md_dark_surface            = Color(0xFF121318)
private val md_dark_onSurface          = Color(0xFFE3E1EA)
private val md_dark_surfaceVariant     = Color(0xFF45464F)
private val md_dark_onSurfaceVariant   = Color(0xFFC6C5D0)
private val md_dark_outline            = Color(0xFF8F909A)
private val md_dark_outlineVariant     = Color(0xFF45464F)

private val LightColors = lightColorScheme(
    primary             = md_light_primary,
    onPrimary           = md_light_onPrimary,
    primaryContainer    = md_light_primaryContainer,
    onPrimaryContainer  = md_light_onPrimaryContainer,
    secondary           = md_light_secondary,
    onSecondary         = md_light_onSecondary,
    secondaryContainer  = md_light_secondaryContainer,
    onSecondaryContainer= md_light_onSecondaryContainer,
    tertiary            = md_light_tertiary,
    onTertiary          = md_light_onTertiary,
    tertiaryContainer   = md_light_tertiaryContainer,
    onTertiaryContainer = md_light_onTertiaryContainer,
    error               = md_light_error,
    onError             = md_light_onError,
    errorContainer      = md_light_errorContainer,
    onErrorContainer    = md_light_onErrorContainer,
    background          = md_light_background,
    onBackground        = md_light_onBackground,
    surface             = md_light_surface,
    onSurface           = md_light_onSurface,
    surfaceVariant      = md_light_surfaceVariant,
    onSurfaceVariant    = md_light_onSurfaceVariant,
    outline             = md_light_outline,
    outlineVariant      = md_light_outlineVariant,
)

private val DarkColors = darkColorScheme(
    primary             = md_dark_primary,
    onPrimary           = md_dark_onPrimary,
    primaryContainer    = md_dark_primaryContainer,
    onPrimaryContainer  = md_dark_onPrimaryContainer,
    secondary           = md_dark_secondary,
    onSecondary         = md_dark_onSecondary,
    secondaryContainer  = md_dark_secondaryContainer,
    onSecondaryContainer= md_dark_onSecondaryContainer,
    tertiary            = md_dark_tertiary,
    onTertiary          = md_dark_onTertiary,
    tertiaryContainer   = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    error               = md_dark_error,
    onError             = md_dark_onError,
    errorContainer      = md_dark_errorContainer,
    onErrorContainer    = md_dark_onErrorContainer,
    background          = md_dark_background,
    onBackground        = md_dark_onBackground,
    surface             = md_dark_surface,
    onSurface           = md_dark_onSurface,
    surfaceVariant      = md_dark_surfaceVariant,
    onSurfaceVariant    = md_dark_onSurfaceVariant,
    outline             = md_dark_outline,
    outlineVariant      = md_dark_outlineVariant,
)

private fun Color.lighten(factor: Float) = Color(
    red   = (red   + (1f - red)   * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue  = (blue  + (1f - blue)  * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.darken(factor: Float) = Color(
    red   = (red   * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue  = (blue  * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
)

private fun onColorFor(bg: Color): Color =
    if (bg.luminance() > 0.4f) Color.Black else Color.White

private fun applyAccent(base: ColorScheme, accent: Color, dark: Boolean): ColorScheme {
    val container   = if (dark) accent.darken(0.35f) else accent.lighten(0.72f)
    val onContainer = onColorFor(container)
    return base.copy(
        primary            = accent,
        onPrimary          = onColorFor(accent),
        primaryContainer   = container,
        onPrimaryContainer = onContainer,
        inversePrimary     = if (dark) accent.lighten(0.4f) else accent.darken(0.3f)
    )
}

@Composable
fun MoneyIQTheme(
    darkTheme:    Boolean = isSystemInDarkTheme(),
    accentColor:  Color?  = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val colorScheme = if (accentColor != null) applyAccent(base, accentColor, darkTheme) else base

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
