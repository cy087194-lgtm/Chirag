package com.Lia.assistant.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NovaColorScheme(
    val background: Color,
    val backgroundGradientTop: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val surfaceBorder: Color,
    val surfaceRaised: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val userBubble: Color,
    val assistantBubble: Color,
    val depthShadow: Color,
    val rimLight: Color,
    val lagoon: Color,
    val lotus: Color
)

val DarkNovaColors = NovaColorScheme(
    background = Color(0xFF06050E),
    backgroundGradientTop = Color(0xFF15113A),
    surface = Color(0xFF17143A),
    surfaceGlass = Color(0x3317143A),
    surfaceBorder = Color(0x2E6C63FF),
    surfaceRaised = Color(0xFF221E4D),
    accent = Color(0xFFFFB547),
    accentSecondary = Color(0xFFFF8A3D),
    accentGlow = Color(0x4DFFB547),
    onAccent = Color(0xFF06050E),
    textPrimary = Color(0xFFEEE9FF),
    textSecondary = Color(0xFFA7A0CC),
    textTertiary = Color(0xFF6F6A99),
    success = Color(0xFF3FE0D0),
    warning = Color(0xFFFFB547),
    error = Color(0xFFFF5252),
    userBubble = Color(0xFF2A214E),
    assistantBubble = Color(0xFF17143A),
    depthShadow = Color(0x80000000),
    rimLight = Color(0x26FFFFFF),
    lagoon = Color(0xFF3FE0D0),
    lotus = Color(0xFFFF6FAE)
)

val LightNovaColors = NovaColorScheme(
    background = Color(0xFFF3F0FB),
    backgroundGradientTop = Color(0xFFE5DEFB),
    surface = Color(0xFFFFFFFF),
    surfaceGlass = Color(0xCCFFFFFF),
    surfaceBorder = Color(0x1F2B2653),
    surfaceRaised = Color(0xFFFBF9FF),
    accent = Color(0xFFB86A00),
    accentSecondary = Color(0xFFD47C00),
    accentGlow = Color(0x33B86A00),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF15112E),
    textSecondary = Color(0xFF585273),
    textTertiary = Color(0xFF8F88A8),
    success = Color(0xFF00897B),
    warning = Color(0xFFB86A00),
    error = Color(0xFFD32F2F),
    userBubble = Color(0xFFEFE8FF),
    assistantBubble = Color(0xFFFFFFFF),
    depthShadow = Color(0x1F000000),
    rimLight = Color(0x40FFFFFF),
    lagoon = Color(0xFF00B4D8),
    lotus = Color(0xFFE91E63)
)

val LocalNovaColors = staticCompositionLocalOf { DarkNovaColors }
val LocalNovaReducedMotion = staticCompositionLocalOf { false }
