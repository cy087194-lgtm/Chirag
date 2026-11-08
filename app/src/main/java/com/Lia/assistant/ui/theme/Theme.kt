package com.Lia.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun NovaTheme(
    mode: String = "DARK",
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (mode.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val novaColors = if (isDark) DarkNovaColors else LightNovaColors

    val materialColors = if (isDark) {
        darkColorScheme(
            primary = novaColors.accent,
            onPrimary = novaColors.onAccent,
            primaryContainer = novaColors.surfaceRaised,
            onPrimaryContainer = novaColors.textPrimary,
            secondary = novaColors.accentSecondary,
            background = novaColors.background,
            onBackground = novaColors.textPrimary,
            surface = novaColors.surface,
            onSurface = novaColors.textPrimary,
            error = novaColors.error
        )
    } else {
        lightColorScheme(
            primary = novaColors.accent,
            onPrimary = novaColors.onAccent,
            primaryContainer = novaColors.surfaceRaised,
            onPrimaryContainer = novaColors.textPrimary,
            secondary = novaColors.accentSecondary,
            background = novaColors.background,
            onBackground = novaColors.textPrimary,
            surface = novaColors.surface,
            onSurface = novaColors.textPrimary,
            error = novaColors.error
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = novaColors.background.toArgb()
            window.navigationBarColor = novaColors.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalNovaColors provides novaColors,
        LocalNovaReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = NovaTypography,
            content = content
        )
    }
}
