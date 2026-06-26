package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.local.model.NotificationFeedEntity
import com.fluxer.client.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationFeedEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationFeedEntity>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepository.getRecentNotifications()
                .onSuccess {
                    _notifications.value = it
                    _error.value = null
                }
                .onError {
                    _error.value = it
                }
            _isLoading.value = false
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markNotificationRead(id)
            _notifications.value = _notifications.value.map {
                if (it.id == id) it.copy(read = true) else it
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            notificationRepository.markAllNotificationsRead()
            _notifications.value = _notifications.value.map { it.copy(read = true) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepository.clearNotificationFeed()
            _notifications.value = emptyList()
        }
    }
}
