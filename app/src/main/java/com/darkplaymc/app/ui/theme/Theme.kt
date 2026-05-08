package com.darkplaymc.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary               = OnDarkPrimary,
    onPrimary             = DarkBackground,
    primaryContainer      = DarkSurface2,
    onPrimaryContainer    = OnDarkPrimary,
    secondary             = OnDarkSecondary,
    onSecondary           = DarkBackground,
    secondaryContainer    = DarkSurface,
    onSecondaryContainer  = OnDarkPrimary,
    tertiary              = OnDarkSecondary,
    background            = DarkBackground,
    surface               = DarkSurface,
    surfaceVariant        = DarkSurfaceVariant,
    onBackground          = OnDarkPrimary,
    onSurface             = OnDarkPrimary,
    onSurfaceVariant      = OnDarkSecondary,
    outline               = OnDarkDisabled
)

@Composable
fun DarkPlayMCTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
