package com.fluxer.client.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

val LocalAccentColor = compositionLocalOf { PhantomRed }

private fun buildDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = TextOnRed,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = TextPrimary,

    secondary = InfoCyan,
    onSecondary = VelvetBlack,
    secondaryContainer = VelvetLight,
    onSecondaryContainer = TextPrimary,

    tertiary = AlertYellow,
    onTertiary = VelvetBlack,
    tertiaryContainer = VelvetLight,
    onTertiaryContainer = TextPrimary,

    background = VelvetBlack,
    onBackground = TextPrimary,

    surface = VelvetDark,
    onSurface = TextPrimary,
    surfaceVariant = VelvetMid,
    onSurfaceVariant = TextSecondary,

    error = DndRed,
    onError = TextPrimary,
    errorContainer = DndRed.copy(alpha = 0.2f),
    onErrorContainer = DndRed,

    outline = BorderSubtle,
    outlineVariant = BorderDark,

    scrim = OverlayDark
)

private val LightColorScheme = lightColorScheme(
    primary = PhantomRed,
    onPrimary = TextOnRed,
    secondary = InfoCyan,
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun FluxerTheme(
    darkTheme: Boolean = true,
    accentColor: Color = PhantomRed,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildDarkColorScheme(accentColor) else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(density = baseDensity.density, fontScale = fontScale)

    CompositionLocalProvider(
        LocalAccentColor provides accentColor,
        LocalFluxerColors provides FluxerColors(
            phantomRed = accentColor,
            phantomRedDark = accentColor.copy(alpha = 0.7f),
            selectedItem = accentColor.copy(alpha = 0.16f),
            glowRed = accentColor.copy(alpha = 0.22f),
            borderSharp = accentColor
        ),
        LocalFluxerSpacing provides FluxerSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FluxerTypography,
            shapes = FluxerShapes
        ) {
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                content()
            }
        }
    }
}

object FluxerTheme {
    val colors: FluxerColors
        @Composable
        get() = LocalFluxerColors.current
}

data class FluxerColors(
    val phantomRed: Color = PhantomRed,
    val phantomRedDark: Color = PhantomRedDark,
    val velvetBlack: Color = VelvetBlack,
    val velvetDark: Color = VelvetDark,
    val velvetMid: Color = VelvetMid,
    val velvetSurface: Color = VelvetSurface,
    val alertYellow: Color = AlertYellow,
    val infoCyan: Color = InfoCyan,
    val successGreen: Color = SuccessGreen,
    val borderSharp: Color = BorderSharp,
    val borderSubtle: Color = BorderSubtle,
    val textMuted: Color = TextMuted,
    val online: Color = OnlineGreen,
    val away: Color = AwayYellow,
    val dnd: Color = DndRed,
    val offline: Color = OfflineGray,
    val glowRed: Color = GlowRed,
    val panelBackground: Color = PanelBackground,
    val selectedItem: Color = SelectedItem,
    val hoverItem: Color = HoverItem
)

val LocalFluxerColors = staticCompositionLocalOf { FluxerColors() }

@Composable
fun ProvideFluxerColors(
    colors: FluxerColors = FluxerColors(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalFluxerColors provides colors,
        LocalFluxerSpacing provides FluxerSpacing(),
        content = content
    )
}
