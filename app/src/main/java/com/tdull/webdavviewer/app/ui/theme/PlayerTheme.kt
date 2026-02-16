package com.tdull.webdavviewer.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 视频播放器专用深色主题
 * 使用纯黑色背景以提供更好的视频观看体验
 */

// 播放器专用颜色
private val PlayerBlack = Color(0xFF000000)
private val PlayerSurface = Color(0xFF121212)
private val PlayerControlBackground = Color(0x80000000) // 半透明黑色
private val PlayerControlText = Color(0xFFFFFFFF)
private val PlayerAccent = Color(0xFFBB86FC) // 紫色强调色

private val PlayerColorScheme = darkColorScheme(
    primary = PlayerAccent,
    onPrimary = Color.White,
    secondary = PlayerAccent,
    onSecondary = Color.White,
    background = PlayerBlack,
    surface = PlayerSurface,
    onBackground = Color.White,
    onSurface = Color.White,
)

/**
 * 播放器主题
 * 强制使用深色主题，并设置状态栏和导航栏为黑色
 */
@Composable
fun PlayerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PlayerColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏和导航栏为黑色
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            // 设置状态栏图标为浅色（因为背景是深色）
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
