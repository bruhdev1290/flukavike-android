package com.fluxer.client.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.NotificationSettings
import com.fluxer.client.data.repository.NotificationRepository
import com.fluxer.client.util.UnifiedPushManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedProvider = MutableStateFlow(UnifiedPushManager.PushProvider.FCM)
    val selectedProvider: StateFlow<UnifiedPushManager.PushProvider> = _selectedProvider.asStateFlow()

    private val _availableDistributors = MutableStateFlow<List<String>>(emptyList())
    val availableDistributors: StateFlow<List<String>> = _availableDistributors.asStateFlow()

    init {
        viewModelScope.launch {
            UnifiedPushManager.getSelectedProvider(context).collect { provider ->
                _selectedProvider.value = provider
            }
        }
    }

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

    fun selectPushProvider(provider: UnifiedPushManager.PushProvider) {
        viewModelScope.launch {
            UnifiedPushManager.setSelectedProvider(context, provider)
            _selectedProvider.value = provider

            when (provider) {
                UnifiedPushManager.PushProvider.FCM -> {
                    UnifiedPushManager.unregister(context)
                    // FCM token registration is handled automatically by FirebaseMessagingService
                }
                UnifiedPushManager.PushProvider.UNIFIEDPUSH -> {
                    val distributors = UnifiedPushManager.getDistributors(context)
                    if (distributors.size == 1) {
                        UnifiedPushManager.saveDistributor(context, distributors.first())
                    } else if (distributors.isEmpty()) {
                        Timber.w("No UnifiedPush distributors available")
                    } else {
                        _availableDistributors.value = distributors
                    }
                }
            }
        }
    }

    fun selectDistributor(distributor: String) {
        viewModelScope.launch {
            UnifiedPushManager.saveDistributor(context, distributor)
            _availableDistributors.value = emptyList()
        }
    }

    fun loadDistributors() {
        _availableDistributors.value = UnifiedPushManager.getDistributors(context)
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
