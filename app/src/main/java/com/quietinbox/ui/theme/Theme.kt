package com.quietinbox.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = QuietTokens.Dark.primary,
    onPrimary = QuietTokens.Dark.background,
    primaryContainer = QuietTokens.Dark.surfaceVariant,
    onPrimaryContainer = QuietTokens.Dark.primary,
    surface = QuietTokens.Dark.surface,
    onSurface = QuietTokens.Dark.onSurface,
    surfaceVariant = QuietTokens.Dark.surfaceVariant,
    onSurfaceVariant = QuietTokens.Dark.onSurfaceVariant,
    background = QuietTokens.Dark.background,
    onBackground = QuietTokens.Dark.onSurface,
    outline = QuietTokens.Dark.outline,
    error = QuietTokens.Dark.danger,
    onError = QuietTokens.Dark.background
)

private val LightColorScheme = lightColorScheme(
    primary = QuietTokens.Light.primary,
    onPrimary = QuietTokens.Light.surface,
    primaryContainer = QuietTokens.Light.surfaceVariant,
    onPrimaryContainer = QuietTokens.Light.primary,
    surface = QuietTokens.Light.surface,
    onSurface = QuietTokens.Light.onSurface,
    surfaceVariant = QuietTokens.Light.surfaceVariant,
    onSurfaceVariant = QuietTokens.Light.onSurfaceVariant,
    background = QuietTokens.Light.background,
    onBackground = QuietTokens.Light.onSurface,
    outline = QuietTokens.Light.outline,
    error = QuietTokens.Light.danger,
    onError = QuietTokens.Light.surface
)

object QuietTheme {
    val colors: QuietColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: QuietTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val shapes: QuietShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShapes.current

    val tokens: QuietTokens = QuietTokens
}

@Composable
fun QuietInboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val appColors = if (darkTheme) DarkQuietColors else LightQuietColors
    val appTypography = QuietTypography()
    val appShapes = QuietShapes()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = appColors.background.toArgb()
                window.navigationBarColor = appColors.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography,
        LocalAppShapes provides appShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content
        )
    }
}
