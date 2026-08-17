package com.storm.lyricraft.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.storm.lyricraft.utils.ThemeManager

// 深色模式保持不变
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Color(0xFF2D2D6E),
    onPrimaryContainer = Color(0xFFD0D4F0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2D2D3A),
    onSurfaceVariant = Color(0xFFC5C6D0)
)

// 浅色模式 — 干净、清爽、层次分明
private val LightColorScheme = lightColorScheme(
    // 主色 — 干净紫色，不脏
    primary = Color(0xFF7C5CBF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF3D1F7A),

    secondary = Color(0xFF9A7BCB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE3FF),
    onSecondaryContainer = Color(0xFF452A7A),

    // 背景 — 干净暖白，不灰
    background = Color(0xFFF8F6FA),
    onBackground = Color(0xFF1C1A2A),

    // 表面 — 纯白卡片
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1A2A),

    // 底栏 — 干净浅紫灰，不脏
    surfaceVariant = Color(0xFFE8E4F0),
    onSurfaceVariant = Color(0xFF4A3D5E),

    outline = Color(0xFFD5CEE0),
    outlineVariant = Color(0xFFE3DDEA)
)

@Composable
fun AppTheme(
    themeMode: String,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeManager.THEME_LIGHT -> false
        ThemeManager.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MaterialTheme.shapes,
        content = {
            val view = LocalView.current

            SideEffect {
                if (!view.isInEditMode) {
                    val window = (view.context as androidx.activity.ComponentActivity).window
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.surfaceVariant.toArgb()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !darkTheme
                        insetsController.isAppearanceLightNavigationBars = !darkTheme
                    }
                }
            }

            LaunchedEffect(themeMode) {
                if (!view.isInEditMode) {
                    val window = (view.context as androidx.activity.ComponentActivity).window
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.surfaceVariant.toArgb()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !darkTheme
                        insetsController.isAppearanceLightNavigationBars = !darkTheme
                    }
                }
            }

            content()
        }
    )
}

fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (this.alpha * 255).toInt(),
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt()
    )
}