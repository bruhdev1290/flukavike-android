package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.*
import com.fluxer.client.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            settingsRepository.getSettings().collect { settings ->
                _settings.value = settings
                _isLoading.value = false
            }
        }
    }

    fun updateTheme(theme: ThemeMode) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(theme = theme)
            saveSettings()
        }
    }

    fun updateMessageDisplay(mode: MessageDisplayMode) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(messageDisplay = mode)
            saveSettings()
        }
    }

    fun updateFontSize(size: FontSize) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(fontSize = size)
            saveSettings()
        }
    }

    fun updateCompactMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(compactMode = enabled)
            saveSettings()
        }
    }

    fun updateShowAnimations(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(showAnimations = enabled)
            saveSettings()
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(soundEnabled = enabled)
            saveSettings()
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(notificationsEnabled = enabled)
            saveSettings()
        }
    }

    fun updateMentionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(mentionNotifications = enabled)
            saveSettings()
        }
    }

    fun updateDMNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(dmNotifications = enabled)
            saveSettings()
        }
    }

    fun updateCallNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            _settings.value = current.copy(callNotifications = enabled)
            saveSettings()
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val result = settingsRepository.saveSettings(_settings.value)
            result.onError { error ->
                Timber.e("Failed to save settings: $error")
            }
        }
    }
}
