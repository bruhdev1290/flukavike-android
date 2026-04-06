package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.NotificationSettings
import com.fluxer.client.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = notificationRepository.getNotificationSettings()
            result.onSuccess { settings ->
                _settings.value = settings
            }.onError { error ->
                Timber.e("Failed to load notification settings: $error")
            }
            _isLoading.value = false
        }
    }

    fun updateGlobalEnabled(enabled: Boolean) {
        updateSettings { it.copy(globalEnabled = enabled) }
    }

    fun updateDMNotifications(enabled: Boolean) {
        updateSettings { it.copy(dmNotifications = enabled) }
    }

    fun updateMentionNotifications(enabled: Boolean) {
        updateSettings { it.copy(mentionNotifications = enabled) }
    }

    fun updateCallNotifications(enabled: Boolean) {
        updateSettings { it.copy(callNotifications = enabled) }
    }

    fun updateFriendRequestNotifications(enabled: Boolean) {
        updateSettings { it.copy(friendRequestNotifications = enabled) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        updateSettings { it.copy(soundEnabled = enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        updateSettings { it.copy(vibrationEnabled = enabled) }
    }

    fun updateShowPreview(enabled: Boolean) {
        updateSettings { it.copy(showPreview = enabled) }
    }

    private fun updateSettings(update: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch {
            val newSettings = update(_settings.value)
            _settings.value = newSettings
            
            val result = notificationRepository.updateNotificationSettings(newSettings)
            result.onError { error ->
                Timber.e("Failed to update notification settings: $error")
            }
        }
    }
}
