package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.VoiceParticipant
import com.fluxer.client.data.model.VoiceState
import com.fluxer.client.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VoiceChannelViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _participants = MutableStateFlow<List<VoiceParticipant>>(emptyList())
    val participants: StateFlow<List<VoiceParticipant>> = _participants.asStateFlow()

    private val _voiceState = MutableStateFlow<VoiceState?>(null)
    val voiceState: StateFlow<VoiceState?> = _voiceState.asStateFlow()

    private val _channelInfo = MutableStateFlow<Channel?>(null)
    val channelInfo: StateFlow<Channel?> = _channelInfo.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private var currentChannelId: String? = null

    fun joinChannel(channelId: String) {
        if (currentChannelId == channelId) return
        
        viewModelScope.launch {
            _isConnecting.value = true
            currentChannelId = channelId
            
            // Get channel info
            val channelResult = chatRepository.getChannel(channelId)
            channelResult.onSuccess { channel ->
                _channelInfo.value = channel
            }
            
            // Join voice channel
            val result = chatRepository.joinVoiceChannel(channelId)
            result.onSuccess { tokenResponse ->
                Timber.d("Joined voice channel, token received")
                // Start WebRTC connection here with token
                startVoiceConnection(tokenResponse.token)
                
                // Load participants
                loadParticipants(channelId)
            }.onError { error ->
                Timber.e("Failed to join voice channel: $error")
                _isConnecting.value = false
            }
        }
    }

    fun leaveChannel() {
        currentChannelId?.let { channelId ->
            viewModelScope.launch {
                chatRepository.leaveVoiceChannel(channelId)
                stopVoiceConnection()
            }
        }
        currentChannelId = null
        _participants.value = emptyList()
        _voiceState.value = null
    }

    fun toggleMute() {
        val channelId = currentChannelId ?: return
        val currentMute = _voiceState.value?.selfMute ?: false
        
        viewModelScope.launch {
            val result = chatRepository.updateVoiceState(
                channelId = channelId,
                selfMute = !currentMute
            )
            result.onSuccess { state ->
                _voiceState.value = state
            }
        }
    }

    fun toggleDeafen() {
        val channelId = currentChannelId ?: return
        val currentDeaf = _voiceState.value?.selfDeaf ?: false
        
        viewModelScope.launch {
            val result = chatRepository.updateVoiceState(
                channelId = channelId,
                selfDeaf = !currentDeaf
            )
            result.onSuccess { state ->
                _voiceState.value = state
            }
        }
    }

    private fun loadParticipants(channelId: String) {
        viewModelScope.launch {
            val result = chatRepository.getVoiceParticipants(channelId)
            result.onSuccess { participants ->
                _participants.value = participants
                _isConnecting.value = false
            }.onError { error ->
                Timber.e("Failed to load participants: $error")
                _isConnecting.value = false
            }
        }
    }

    private fun startVoiceConnection(token: String) {
        // TODO: Initialize WebRTC connection
        // This would set up the actual voice connection using WebRTC
    }

    private fun stopVoiceConnection() {
        // TODO: Clean up WebRTC connection
    }

    override fun onCleared() {
        super.onCleared()
        leaveChannel()
    }
}
