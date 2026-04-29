package com.asyria.security.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    STANDARD, ZEN
}

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = NeonBlue,
    tertiary = NeuralPurple,
    background = VoidBlack,
    surface = SurfaceDark,
    onPrimary = VoidBlack,
    onSecondary = VoidBlack,
    onTertiary = VoidBlack,
    onBackground = OffWhite,
    onSurface = OffWhite
)

private val ZenColorScheme = lightColorScheme(
    primary = AmberZen,
    secondary = WarmWhite,
    tertiary = ZenBlack,
    background = ZenBlack,
    surface = ZenBlack,
    onPrimary = ZenBlack,
    onSecondary = ZenBlack,
    onTertiary = AmberZen,
    onBackground = WarmWhite,
    onSurface = WarmWhite
)

@Composable
fun SentinelTheme(
    mode: ThemeMode = ThemeMode.STANDARD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (mode) {
        ThemeMode.STANDARD -> DarkColorScheme
        ThemeMode.ZEN -> ZenColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
