package com.tdull.webdavviewer.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 深色模式配色方案
private val DarkColorScheme = darkColorScheme(
    primary = CloudBlue80,
    secondary = CloudCyan80,
    tertiary = CloudTeal80,
    background = CloudBackground80,
    surface = CloudSurface80,
    onPrimary = Color(0xFF003258),
    onSecondary = Color(0xFF00363A),
    onTertiary = Color(0xFF003730),
    onBackground = Color(0xFFE1E2E4),
    onSurface = Color(0xFFE1E2E4)
)

// 浅色模式配色方案
private val LightColorScheme = lightColorScheme(
    primary = CloudBlue40,
    secondary = CloudCyan40,
    tertiary = CloudTeal40,
    background = CloudBackground40,
    surface = CloudSurface40,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)

@Composable
fun WebDAVViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
