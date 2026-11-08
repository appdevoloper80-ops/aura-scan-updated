package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// AuraScan Neon Dark Theme Palette (Matched to Showcase Design)
val NeonDarkBackground = Color(0xFF090D16)
val NeonDarkSurface = Color(0xFF0F172A)
val NeonDarkSurfaceVariant = Color(0xFF1E293B)
val NeonDarkCardBackground = Color(0xFF131D33)
val NeonDarkBorder = Color(0xFF1F2F4E)

// Vibrant Neon Accents
val NeonCyan = Color(0xFF00E5FF)
val NeonCyanGlow = Color(0xFF18FFFF)
val NeonBlue = Color(0xFF2979FF)
val NeonBlueDark = Color(0xFF1565C0)
val NeonIndigo = Color(0xFF7C4DFF)
val NeonPurple = Color(0xFF9C27B0)
val NeonTeal = Color(0xFF00BFA5)
val NeonGreen = Color(0xFF00E676)
val NeonGold = Color(0xFFFFD700)
val NeonAmber = Color(0xFFFF9100)
val NeonCoral = Color(0xFFFF6D00)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)
val TextDark = Color(0xFF0F172A)

// Card Gradients
val HeroCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF10284E), Color(0xFF0D1B36))
)

val ScanTileGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00B0FF), Color(0xFF0077D4))
)

val GalleryTileGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF7C4DFF), Color(0xFF5326B8))
)

val AiTileGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF00897B))
)

val LibraryTileGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFF9100), Color(0xFFD84315))
)

val GoldCardGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6B4800), Color(0xFF4A3200))
)

val CyanButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
)

val DarkButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
)
