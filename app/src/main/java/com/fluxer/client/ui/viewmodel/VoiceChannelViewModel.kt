package com.fluxer.client.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.User
import com.fluxer.client.data.model.VoiceParticipant
import com.fluxer.client.data.model.VoiceState
import com.fluxer.client.data.model.VoiceStateUpdateEvent
import com.fluxer.client.data.repository.AuthRepository
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
    private val authRepository: AuthRepository,
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
    val isCameraEnabled = liveKitVoiceManager.isCameraEnabled
    val isScreenSharing = liveKitVoiceManager.isScreenSharing
    val localVideoTrack = liveKitVoiceManager.localVideoTrack
    val livekitParticipants = liveKitVoiceManager.participants
    val speakingParticipants = liveKitVoiceManager.speakingParticipants

    private var currentChannelId: String? = null
    private var currentGuildId: String? = null

    init {
        // Subscribe to gateway voice state updates for real-time participant changes
        viewModelScope.launch {
            chatRepository.gatewayEvents.collect { event ->
                when (event) {
                    is com.fluxer.client.data.remote.GatewayWebSocketManager.GatewayEvent.VoiceStateUpdate -> {
                        handleGatewayVoiceStateUpdate(event.data)
                    }
                    is com.fluxer.client.data.remote.GatewayWebSocketManager.GatewayEvent.VoiceServerUpdate -> {
                        handleGatewayVoiceServerUpdate(event.data)
                    }
                    else -> { }
                }
            }
        }
    }

    fun joinChannel(channelId: String) {
        if (currentChannelId == channelId) return

        viewModelScope.launch {
            _isConnecting.value = true
            currentChannelId = channelId

            // Get channel info
            val channelResult = chatRepository.getChannel(channelId)
            channelResult.onSuccess { channel ->
                _channelInfo.value = channel
                currentGuildId = channel.serverId
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
        currentGuildId = null
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

    fun isParticipantSpeaking(participantSid: String): Boolean = liveKitVoiceManager.isParticipantSpeaking(participantSid)

    fun toggleCamera() {
        liveKitVoiceManager.toggleCamera()
    }

    fun startScreenShare(intent: Intent) {
        liveKitVoiceManager.startScreenShare(intent)
    }

    fun stopScreenShare() {
        liveKitVoiceManager.stopScreenShare()
    }

    fun getRemoteVideoTrack(participant: io.livekit.android.room.participant.RemoteParticipant) =
        liveKitVoiceManager.getRemoteVideoTrack(participant)

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

    private fun handleGatewayVoiceStateUpdate(voiceState: VoiceStateUpdateEvent) {
        val channelId = currentChannelId ?: return
        val guildId = currentGuildId

        // Only handle updates for our current channel/guild context
        val isRelevant = when {
            voiceState.channelId == channelId -> true
            voiceState.guildId == guildId && voiceState.channelId == null -> {
                // User left a channel in our guild - check if they were in our channel
                _participants.value.any { it.voiceState.userId == voiceState.userId }
            }
            voiceState.guildId != null && voiceState.guildId != guildId -> false
            voiceState.channelId != null && voiceState.channelId != channelId -> false
            else -> true
        }

        if (!isRelevant) return

        // Refresh participants from server to get full user objects
        // In a more optimized implementation, we could merge the gateway update
        // with existing participant data without a full server fetch
        viewModelScope.launch {
            val result = chatRepository.getVoiceParticipants(channelId)
            result.onSuccess { participants ->
                _participants.value = participants
            }.onError { error ->
                Timber.e("Failed to refresh participants: $error")
            }
        }

        val currentUser = (authRepository.authState.value as? AuthRepository.AuthState.Authenticated)?.user
        if (voiceState.userId == currentUser?.id) {
            _voiceState.value = _voiceState.value?.copy(
                selfMute = voiceState.selfMute,
                selfDeaf = voiceState.selfDeaf
            )
        }
    }

    private fun handleGatewayVoiceServerUpdate(voiceServer: com.fluxer.client.data.model.VoiceServerUpdateEvent) {
        val guildId = currentGuildId ?: return
        val channelId = currentChannelId ?: return

        // Only handle if it's for our current guild or call
        if (voiceServer.guildId != guildId && voiceServer.channelId != channelId) return

        Timber.d("Voice server update received: ${voiceServer.endpoint}")

        // Reconnect to LiveKit with new server info if needed
        // The backend may provide a new token/endpoint during a voice session
        viewModelScope.launch {
            liveKitVoiceManager.disconnect()
            _isConnecting.value = true

            val livekitUrl = "wss://${voiceServer.endpoint}"
            val result = liveKitVoiceManager.connect(livekitUrl, voiceServer.token, channelId)
            result.onSuccess {
                Timber.d("LiveKit reconnected to new voice server")
                _isConnecting.value = false
            }.onFailure { error ->
                Timber.e(error, "Failed to reconnect to new voice server")
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
