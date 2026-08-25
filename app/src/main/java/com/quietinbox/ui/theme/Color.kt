package com.quietinbox.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class QuietColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val positive: Color,
    val muted: Color,
    val warning: Color,
    val danger: Color,
    val heatmapRamp: List<Color>,
)

val DarkQuietColors = QuietColors(
    background = QuietTokens.Dark.background,
    surface = QuietTokens.Dark.surface,
    surfaceVariant = QuietTokens.Dark.surfaceVariant,
    outline = QuietTokens.Dark.outline,
    onSurface = QuietTokens.Dark.onSurface,
    onSurfaceVariant = QuietTokens.Dark.onSurfaceVariant,
    primary = QuietTokens.Dark.primary,
    positive = QuietTokens.Dark.positive,
    muted = QuietTokens.Dark.muted,
    warning = QuietTokens.Dark.warning,
    danger = QuietTokens.Dark.danger,
    heatmapRamp = QuietTokens.Dark.heatmapRamp,
)

val LightQuietColors = QuietColors(
    background = QuietTokens.Light.background,
    surface = QuietTokens.Light.surface,
    surfaceVariant = QuietTokens.Light.surfaceVariant,
    outline = QuietTokens.Light.outline,
    onSurface = QuietTokens.Light.onSurface,
    onSurfaceVariant = QuietTokens.Light.onSurfaceVariant,
    primary = QuietTokens.Light.primary,
    positive = QuietTokens.Light.positive,
    muted = QuietTokens.Light.muted,
    warning = QuietTokens.Light.warning,
    danger = QuietTokens.Light.danger,
    heatmapRamp = QuietTokens.Light.heatmapRamp,
)

val LocalAppColors = staticCompositionLocalOf { DarkQuietColors }
