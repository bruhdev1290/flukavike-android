package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.User
import com.fluxer.client.data.model.VoiceParticipant
import com.fluxer.client.data.model.VoiceState
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.service.LiveKitVoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VoiceChannelViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val liveKitVoiceManager: LiveKitVoiceManager
) : ViewModel() {

    // Participants from server API
    private val _participants = MutableStateFlow<List<VoiceParticipant>>(emptyList())
    val participants: StateFlow<List<VoiceParticipant>> = _participants.asStateFlow()

    // Voice state from server
    private val _voiceState = MutableStateFlow<VoiceState?>(null)
    val voiceState: StateFlow<VoiceState?> = _voiceState.asStateFlow()

    // Channel info
    private val _channelInfo = MutableStateFlow<Channel?>(null)
    val channelInfo: StateFlow<Channel?> = _channelInfo.asStateFlow()

    // Connection state
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // LiveKit connection state
    val isConnected = liveKitVoiceManager.isConnected
    val isMuted = liveKitVoiceManager.isMuted
    val isDeafened = liveKitVoiceManager.isDeafened
    val livekitParticipants = liveKitVoiceManager.participants
    val speakingParticipants = liveKitVoiceManager.speakingParticipants

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
            
            // Join voice channel via API
            val result = chatRepository.joinVoiceChannel(channelId)
            result.onSuccess { tokenResponse ->
                Timber.d("Joined voice channel, token received")
                
                // Connect to LiveKit
                val livekitUrl = tokenResponse.livekitUrl ?: "wss://livekit.fluxer.app"
                val token = tokenResponse.token
                val roomName = tokenResponse.roomName ?: channelId
                
                val connectResult = liveKitVoiceManager.connect(livekitUrl, token, roomName)
                connectResult.onSuccess {
                    Timber.d("LiveKit connected successfully")
                    // Load participants from server
                    loadParticipants(channelId)
                }.onFailure { error ->
                    Timber.e(error, "Failed to connect to LiveKit")
                    _isConnecting.value = false
                }
                
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
                liveKitVoiceManager.disconnect()
            }
        }
        currentChannelId = null
        _participants.value = emptyList()
        _voiceState.value = null
    }

    fun toggleMute() {
        liveKitVoiceManager.toggleMute()
        
        // Also update server state
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
        liveKitVoiceManager.toggleDeafen()
        
        // Also update server state
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

    fun isParticipantSpeaking(participantSid: String): Boolean {
        return liveKitVoiceManager.isParticipantSpeaking(participantSid)
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

    override fun onCleared() {
        super.onCleared()
        leaveChannel()
        liveKitVoiceManager.cleanup()
    }
}
