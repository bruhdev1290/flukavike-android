package com.fluxer.client.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemePreset(val displayName: String, val accentArgb: Int, val description: String) {
    VELVET_DARK("Velvet Dark", 0xFFE15463.toInt(), "Classic red — the default"),
    MIDNIGHT("Midnight", 0xFF79B8FF.toInt(), "Cool blue accent"),
    EMERALD("Emerald", 0xFF53C28B.toInt(), "Forest green vibes"),
    AMETHYST("Amethyst", 0xFF9B59B6.toInt(), "Rich purple tones"),
    SUNFIRE("Sunfire", 0xFFFFA76B.toInt(), "Warm orange glow"),
    ARCTIC("Arctic", 0xFF64DFDF.toInt(), "Icy cool tones"),
}

enum class FontScale(val displayName: String, val scale: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra Large", 1.3f),
}

// Swipe threshold in dp — lower = easier to trigger
enum class GestureSensitivity(val displayName: String, val description: String, val thresholdDp: Float) {
    LOW("Low", "Swipe more to trigger", 90f),
    MEDIUM("Medium", "Balanced feel", 55f),
    HIGH("High", "Light swipe triggers", 28f),
}

enum class ServerRailMode(val displayName: String, val description: String) {
    RAIL("Rail", "Always-visible server icons on the left"),
    DRAWER("Drawer", "Server list hidden behind a left-edge drawer")
}

@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    private val _accentColor = MutableStateFlow(
        Color(prefs.getInt(KEY_ACCENT_COLOR, 0xFFE15463.toInt()))
    )
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _fontScale = MutableStateFlow(
        FontScale.entries.firstOrNull { it.name == prefs.getString(KEY_FONT_SCALE, FontScale.MEDIUM.name) }
            ?: FontScale.MEDIUM
    )
    val fontScale: StateFlow<FontScale> = _fontScale.asStateFlow()

    private val _themePreset = MutableStateFlow(
        ThemePreset.entries.firstOrNull { it.name == prefs.getString(KEY_THEME_PRESET, ThemePreset.VELVET_DARK.name) }
            ?: ThemePreset.VELVET_DARK
    )
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _biometricLockEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    )
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    private val _gesturesEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GESTURES_ENABLED, true)
    )
    val gesturesEnabled: StateFlow<Boolean> = _gesturesEnabled.asStateFlow()

    private val _gestureSensitivity = MutableStateFlow(
        GestureSensitivity.entries.firstOrNull { it.name == prefs.getString(KEY_GESTURE_SENSITIVITY, GestureSensitivity.MEDIUM.name) }
            ?: GestureSensitivity.MEDIUM
    )
    val gestureSensitivity: StateFlow<GestureSensitivity> = _gestureSensitivity.asStateFlow()

    private val _serverRailMode = MutableStateFlow(
        ServerRailMode.entries.firstOrNull { it.name == prefs.getString(KEY_SERVER_RAIL_MODE, ServerRailMode.RAIL.name) }
            ?: ServerRailMode.RAIL
    )
    val serverRailMode: StateFlow<ServerRailMode> = _serverRailMode.asStateFlow()

    fun setAccentColor(color: Color) {
        _accentColor.value = color
        prefs.edit().putInt(KEY_ACCENT_COLOR, color.toArgb()).apply()
    }

    fun setFontScale(scale: FontScale) {
        _fontScale.value = scale
        prefs.edit().putString(KEY_FONT_SCALE, scale.name).apply()
    }

    fun setThemePreset(preset: ThemePreset) {
        _themePreset.value = preset
        val color = Color(preset.accentArgb)
        setAccentColor(color)
        prefs.edit().putString(KEY_THEME_PRESET, preset.name).apply()
    }

    fun setBiometricLock(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    fun setGesturesEnabled(enabled: Boolean) {
        _gesturesEnabled.value = enabled
        prefs.edit().putBoolean(KEY_GESTURES_ENABLED, enabled).apply()
    }

    fun setGestureSensitivity(sensitivity: GestureSensitivity) {
        _gestureSensitivity.value = sensitivity
        prefs.edit().putString(KEY_GESTURE_SENSITIVITY, sensitivity.name).apply()
    }

    fun setServerRailMode(mode: ServerRailMode) {
        _serverRailMode.value = mode
        prefs.edit().putString(KEY_SERVER_RAIL_MODE, mode.name).apply()
    }

    companion object {
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        private const val KEY_GESTURES_ENABLED = "gestures_enabled"
        private const val KEY_GESTURE_SENSITIVITY = "gesture_sensitivity"
        private const val KEY_SERVER_RAIL_MODE = "server_rail_mode"
    }
}
