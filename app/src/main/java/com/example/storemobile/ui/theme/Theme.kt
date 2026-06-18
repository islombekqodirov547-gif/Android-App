package com.example.storemobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JeskoColorScheme = darkColorScheme(
    primary = Jesko.Gold,
    onPrimary = Jesko.BgDark,
    primaryContainer = Jesko.GoldDeep,
    onPrimaryContainer = Jesko.TextPrimary,
    secondary = Jesko.Blue,
    onSecondary = Jesko.BgDark,
    tertiary = Jesko.Green,
    onTertiary = Jesko.BgDark,
    background = Jesko.BgDark,
    onBackground = Jesko.TextPrimary,
    surface = Jesko.Card,
    onSurface = Jesko.TextPrimary,
    surfaceVariant = Jesko.Input,
    onSurfaceVariant = Jesko.TextSecondary,
    outline = Jesko.Border,
    outlineVariant = Jesko.InputBorder,
    error = Jesko.Red,
    onError = Jesko.BgDark
)

@Composable
fun JeskoTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // JESKO is always a premium dark theme — matching the desktop product.
    val colorScheme = JeskoColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Jesko.BgDark.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = Jesko.BgDark.luminance() > 0.5f
            controller.isAppearanceLightNavigationBars = Jesko.BgDark.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JeskoTypography,
        shapes = JeskoShapes,
        content = content
    )
}
