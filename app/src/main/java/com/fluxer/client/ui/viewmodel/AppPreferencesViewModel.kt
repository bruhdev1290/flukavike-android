package com.fluxer.client.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.fluxer.client.data.local.AppPreferencesStore
import com.fluxer.client.data.local.FontScale
import com.fluxer.client.data.local.GestureSensitivity
import com.fluxer.client.data.local.ServerRailMode
import com.fluxer.client.data.local.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppPreferencesViewModel @Inject constructor(
    private val store: AppPreferencesStore
) : ViewModel() {

    val accentColor = store.accentColor
    val fontScale = store.fontScale
    val themePreset = store.themePreset
    val biometricLockEnabled = store.biometricLockEnabled
    val gesturesEnabled = store.gesturesEnabled
    val gestureSensitivity = store.gestureSensitivity
    val serverRailMode = store.serverRailMode

    fun setAccentColor(color: Color) = store.setAccentColor(color)
    fun setFontScale(scale: FontScale) = store.setFontScale(scale)
    fun setThemePreset(preset: ThemePreset) = store.setThemePreset(preset)
    fun setBiometricLock(enabled: Boolean) = store.setBiometricLock(enabled)
    fun setGesturesEnabled(enabled: Boolean) = store.setGesturesEnabled(enabled)
    fun setGestureSensitivity(sensitivity: GestureSensitivity) = store.setGestureSensitivity(sensitivity)
    fun setServerRailMode(mode: ServerRailMode) = store.setServerRailMode(mode)
}
