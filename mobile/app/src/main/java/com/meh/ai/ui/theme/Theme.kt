package com.meh.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val MehLightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = MistStrong,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Paper,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = Amber,
    onTertiary = Paper,
    tertiaryContainer = AmberWash,
    onTertiaryContainer = AmberInk,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = FieldLight,
    surfaceContainerLow = Paper,
    surfaceContainer = Mist,
    surfaceContainerHigh = MistStrong,
    surfaceContainerHighest = LineLight,
    outline = LineLight,
    outlineVariant = MistStrong,
    error = Red,
    onError = Paper,
    errorContainer = RedWash,
    onErrorContainer = RedInk,
    inverseSurface = Night,
    inverseOnSurface = Snow,
    inversePrimary = Snow,
    scrim = Night,
)

private val MehDarkColors = darkColorScheme(
    primary = Snow,
    onPrimary = Night,
    primaryContainer = NightHigh,
    onPrimaryContainer = Snow,
    secondary = SnowMuted,
    onSecondary = Night,
    secondaryContainer = NightRaised,
    onSecondaryContainer = Snow,
    tertiary = AmberNight,
    onTertiary = Night,
    tertiaryContainer = AmberNightWash,
    onTertiaryContainer = AmberNight,
    background = Night,
    onBackground = Snow,
    surface = Night,
    onSurface = Snow,
    surfaceVariant = NightRaised,
    onSurfaceVariant = SnowMuted,
    surfaceContainerLowest = Color(0xFF141416),
    surfaceContainerLow = Night,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
    outline = NightLine,
    outlineVariant = NightHigh,
    error = RedNight,
    onError = Night,
    errorContainer = RedNightWash,
    onErrorContainer = RedNight,
    inverseSurface = Snow,
    inverseOnSurface = Night,
    inversePrimary = Night,
    scrim = Color(0xFF09090B),
)

@Suppress("DEPRECATION")
private fun quietRipple(color: Color) = RippleConfiguration(
    color = color,
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.08f,
        focusedAlpha = 0.08f,
        draggedAlpha = 0.08f,
        hoveredAlpha = 0.08f,
    ),
)

private val LocalMehMonoTypography = staticCompositionLocalOf { MonoTypography }

object MehTheme {
    val mono: MehMonoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMehMonoTypography.current
}

@Composable
fun MehAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) MehDarkColors else MehLightColors
    CompositionLocalProvider(LocalMehMonoTypography provides MonoTypography) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
        ) {
            CompositionLocalProvider(LocalRippleConfiguration provides quietRipple(scheme.onSurface)) {
                content()
            }
        }
    }
}
