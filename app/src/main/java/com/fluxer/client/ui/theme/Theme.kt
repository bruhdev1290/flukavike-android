package com.fluxer.client.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Fluxer Dark Theme - Persona 5 Inspired
 * Sharp, high-contrast gaming aesthetic
 */

private val DarkColorScheme = darkColorScheme(
    primary = PhantomRed,
    onPrimary = TextOnRed,
    primaryContainer = PhantomRedDark,
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
    // We primarily use dark theme, but define light for completeness
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
    darkTheme: Boolean = true, // Default to dark theme
    dynamicColor: Boolean = false, // Disable dynamic colors for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FluxerTypography,
        shapes = FluxerShapes,
        content = content
    )
}

/**
 * Custom theme extension for accessing custom colors
 */
object FluxerTheme {
    val colors: FluxerColors
        @Composable
        get() = LocalFluxerColors.current
}

/**
 * Custom colors that extend beyond Material3 color scheme
 */
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
        content = content
    )
}
