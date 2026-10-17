package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = EmeraldDark,
    onSecondary = OnEmeraldPrimary,
    secondaryContainer = EmeraldSoft,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = AmberAccent,
    onTertiary = OnEmeraldPrimary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = BgGray,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextMuted,
    outline = SurfaceBorder,
    error = ErrorRed,
    onErrorContainer = ErrorRed,
    errorContainer = ErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = OnEmeraldContainer,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = EmeraldContainer,
    secondary = EmeraldPrimary,
    onSecondary = OnEmeraldContainer,
    secondaryContainer = EmeraldDark,
    onSecondaryContainer = EmeraldContainer,
    tertiary = AmberLight,
    onTertiary = OnAmberContainer,
    tertiaryContainer = AmberDark,
    onTertiaryContainer = AmberContainer,
    background = TextDark,
    onBackground = SurfaceWhite,
    surface = Color(0xFF1F2937),
    onSurface = SurfaceWhite,
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563),
    error = ErrorRed
)

@Composable
fun SaleSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
