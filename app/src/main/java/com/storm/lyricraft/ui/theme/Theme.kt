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

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2979FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = Color(0xFF31313C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF002984),
    primaryContainer = Color(0xFF153660),
    onPrimaryContainer = Color(0xFFD0E4FF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2D2D3A),
    onSurfaceVariant = Color(0xFFC5C6D0)
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