package com.starkindustries.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    JARVIS, FRIDAY, SAFETY, RED_ALERT
}

data class JarvisColors(
    val bright: Color,
    val glow: Color,
    val dim: Color,
    val dark: Color,
    val background: Color = BackgroundDark,
    val surface: Color = SurfaceDark,
    val textPrimary: Color = TextLight,
    val textSecondary: Color = TextDim
)

val LocalJarvisColors = staticCompositionLocalOf {
    JarvisColors(
        bright = CyanBright,
        glow = CyanGlow,
        dim = CyanDim,
        dark = CyanDark
    )
}

object JarvisTheme {
    val colors: JarvisColors
        @Composable
        get() = LocalJarvisColors.current
}

@Composable
fun JarvisTheme(
    mode: ThemeMode = ThemeMode.JARVIS,
    content: @Composable () -> Unit
) {
    val jarvisColors = when (mode) {
        ThemeMode.JARVIS -> JarvisColors(
            bright = CyanBright,
            glow = CyanGlow,
            dim = CyanDim,
            dark = CyanDark
        )
        ThemeMode.FRIDAY -> JarvisColors(
            bright = OrangeBright,
            glow = OrangeGlow,
            dim = OrangeDim,
            dark = OrangeDark
        )
        ThemeMode.SAFETY -> JarvisColors(
            bright = GreenBright,
            glow = GreenGlow,
            dim = GreenDim,
            dark = GreenDark
        )
        ThemeMode.RED_ALERT -> JarvisColors(
            bright = RedBright,
            glow = RedGlow,
            dim = RedDim,
            dark = RedDark
        )
    }

    val colorScheme = darkColorScheme(
        primary = jarvisColors.bright,
        background = jarvisColors.background,
        surface = jarvisColors.surface,
        onPrimary = Color.Black,
        onBackground = jarvisColors.textPrimary,
        onSurface = jarvisColors.textPrimary
    )

    CompositionLocalProvider(LocalJarvisColors provides jarvisColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
