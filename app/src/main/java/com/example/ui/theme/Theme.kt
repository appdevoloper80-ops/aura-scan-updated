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

private val AuraScanNeonColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002029),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = NeonCyanGlow,
    secondary = NeonBlue,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF152A50),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = NeonTeal,
    onTertiary = Color(0xFF00201A),
    background = NeonDarkBackground,
    onBackground = TextWhite,
    surface = NeonDarkSurface,
    onSurface = TextWhite,
    surfaceVariant = NeonDarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = NeonDarkBorder,
    error = Color(0xFFFF5252),
    onError = Color(0xFFFFFFFF)
)

// Clean high contrast light fallback if user switches system theme, but optimized with same cyan accents
private val AuraScanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF1976D2),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun AuraScanTheme(
    darkTheme: Boolean = true, // Neon dark theme as showcased
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AuraScanNeonColorScheme else AuraScanLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AuraScanTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
