package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.preferences.AppThemeEnum

val LocalThemeColors = staticCompositionLocalOf<ThemeColors> {
    error("No ThemeColors provided")
}

@Composable
fun StudentAlarmTheme(
    theme: AppThemeEnum = AppThemeEnum.NEON_GLOW,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val themeProps = ThemeEngine.getThemeColors(theme, isSystemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = themeProps.colorScheme.background.toArgb()
                window.navigationBarColor = themeProps.colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = themeProps.isLight
                insetsController.isAppearanceLightNavigationBars = themeProps.isLight
            }
        }
    }

    CompositionLocalProvider(LocalThemeColors provides themeProps) {
        MaterialTheme(
            colorScheme = themeProps.colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Alias for AppTheme as required by Problem 1
@Composable
fun AppTheme(
    theme: AppThemeEnum = AppThemeEnum.NEON_GLOW,
    content: @Composable () -> Unit
) {
    StudentAlarmTheme(theme = theme, content = content)
}
