package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary High-Contrast Monochrome Palette
val ObsidianBlack = Color(0xFF121214)
val PureBlack = Color(0xFF000000)
val DarkCharcoal = Color(0xFF1C1C1E)
val MediumGray = Color(0xFF8E8E93)
val CoolGray = Color(0xFF6B7280)
val LightBorderGray = Color(0xFFE5E7EB)
val SoftSurfaceGray = Color(0xFFF2F3F5)
val ActivePillBg = Color(0xFFF3F4F6)
val CanvasBg = Color(0xFFF8F9FA)
val PureWhite = Color(0xFFFFFFFF)

// Accent Colors
val AccentRed = Color(0xFFFF2D55)
val MusicPrimary = ObsidianBlack
val MusicPrimaryDark = PureBlack
val MusicSecondary = Color(0xFF27272A)
val MusicAccent = ObsidianBlack
val MusicEmerald = Color(0xFF10B981)

val DarkBackground = Color(0xFF121214)
val DarkSurface = Color(0xFF18181B)
val DarkSurfaceVariant = Color(0xFF27272A)
val DarkSurfaceElevated = Color(0xFF3F3F46)

val LightTextPrimary = Color(0xFF121214)
val LightTextSecondary = Color(0xFF71717A)
val LightTextTertiary = Color(0xFFA1A1AA)

val LiveRed = Color(0xFFFF2D55)
val LiveBadgeBg = Color(0xFFE11D48)
val TubeRed = Color(0xFFFF2D55)

object MusicGradients {
    val HeroButton = Brush.linearGradient(
        colors = listOf(Color(0xFF121214), Color(0xFF27272A))
    )
    val PlayerCardGlow = Brush.radialGradient(
        colors = listOf(Color(0x1A000000), Color(0x00000000))
    )
    val MiniPlayerBg = Brush.horizontalGradient(
        colors = listOf(Color(0xFF18181B), Color(0xFF121214))
    )
    val GenrePop = Brush.linearGradient(listOf(Color(0xFF1E1E24), Color(0xFF2B2B36)))
    val GenreHipHop = Brush.linearGradient(listOf(Color(0xFF27272A), Color(0xFF3F3F46)))
    val GenreLofi = Brush.linearGradient(listOf(Color(0xFF3F3F46), Color(0xFF52525B)))
    val GenreRock = Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF27272A)))
    val GenreElectronic = Brush.linearGradient(listOf(Color(0xFF27272A), Color(0xFF1E1E24)))
    val GenreChill = Brush.linearGradient(listOf(Color(0xFF3F3F46), Color(0xFF27272A)))
}


