package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _dmChannels = MutableStateFlow<List<Channel>>(emptyList())
    val dmChannels: StateFlow<List<Channel>> = _dmChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collectLatest { query ->
                    filterChannels(query)
                }
        }
    }

    fun loadDMChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.getDMChannels()
            result.onSuccess { channels ->
                _dmChannels.value = channels
                _filteredChannels.value = channels
            }.onError { error ->
                Timber.e("Failed to load DM channels: $error")
            }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun filterChannels(query: String) {
        val channels = _dmChannels.value
        _filteredChannels.value = if (query.isBlank()) {
            channels
        } else {
            channels.filter { channel ->
                channel.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun createDMChannel(recipientId: String) {
        viewModelScope.launch {
            val result = chatRepository.createDMChannel(recipientId)
            result.onSuccess { channel ->
                val current = _dmChannels.value.toMutableList()
                current.add(0, channel)
                _dmChannels.value = current
            }.onError { error ->
                Timber.e("Failed to create DM channel: $error")
            }
        }
    }
    
    fun closeDM(channelId: String) {
        viewModelScope.launch {
            // Remove from local list immediately for UX
            val current = _dmChannels.value.toMutableList()
            current.removeAll { it.id == channelId }
            _dmChannels.value = current
            
            // TODO: Call API to close/hide DM
            Timber.d("Closing DM: $channelId")
        }
    }
    
    fun muteDM(channelId: String, duration: com.fluxer.client.ui.screens.MuteDuration) {
        viewModelScope.launch {
            // TODO: Call API to mute DM
            Timber.d("Muting DM: $channelId for $duration")
        }
    }
}
