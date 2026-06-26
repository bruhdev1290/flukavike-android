package com.fluxer.client.service

import android.content.Context
import android.content.Intent
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveKitVoiceManager @Inject constructor(
    context: Context
) {
    private var room: Room? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val appContext = context.applicationContext

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isDeafened = MutableStateFlow(false)
    val isDeafened: StateFlow<Boolean> = _isDeafened.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(false)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isScreenSharing = MutableStateFlow(false)
    val isScreenSharing: StateFlow<Boolean> = _isScreenSharing.asStateFlow()

    private val _participants = MutableStateFlow<List<RemoteParticipant>>(emptyList())
    val participants: StateFlow<List<RemoteParticipant>> = _participants.asStateFlow()

    private val _speakingParticipants = MutableStateFlow<Set<String>>(emptySet())
    val speakingParticipants: StateFlow<Set<String>> = _speakingParticipants.asStateFlow()

    // Local camera video track exposed for preview rendering
    private val _localVideoTrack = MutableStateFlow<LocalVideoTrack?>(null)
    val localVideoTrack: StateFlow<LocalVideoTrack?> = _localVideoTrack.asStateFlow()

    private var currentRoomName: String? = null

    suspend fun connect(url: String, token: String, roomName: String): Result<Unit> {
        return try {
            if (room != null) disconnect()

            currentRoomName = roomName
            val newRoom = LiveKit.create(appContext, options = RoomOptions(adaptiveStream = true, dynacast = true))
            room = newRoom

            coroutineScope.launch {
                newRoom.events.collect { event -> handleRoomEvent(event) }
            }

            newRoom.connect(url, token)
            newRoom.localParticipant.setMicrophoneEnabled(true)

            _isConnected.value = true
            _isMuted.value = false
            updateParticipants(newRoom)

            Timber.d("Connected to LiveKit room: $roomName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to LiveKit room")
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            room?.disconnect()
            room = null
            _isConnected.value = false
            _isMuted.value = false
            _isDeafened.value = false
            _isCameraEnabled.value = false
            _isScreenSharing.value = false
            _participants.value = emptyList()
            _speakingParticipants.value = emptySet()
            _localVideoTrack.value = null
            currentRoomName = null
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from room")
        }
    }

    fun toggleMute() {
        val room = this.room ?: return
        val newMuteState = !_isMuted.value
        coroutineScope.launch {
            try {
                room.localParticipant.setMicrophoneEnabled(!newMuteState)
                _isMuted.value = newMuteState
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle mute")
            }
        }
    }

    fun toggleDeafen() {
        val newDeafenState = !_isDeafened.value
        _isDeafened.value = newDeafenState
        if (newDeafenState && !_isMuted.value) toggleMute()
        else if (!newDeafenState && _isMuted.value) toggleMute()
    }

    fun toggleCamera() {
        val room = this.room ?: return
        val newState = !_isCameraEnabled.value
        coroutineScope.launch {
            try {
                room.localParticipant.setCameraEnabled(newState)
                _isCameraEnabled.value = newState
                // Update local video track reference
                if (newState) {
                    val pub = room.localParticipant.getTrackPublication(io.livekit.android.room.track.Track.Source.CAMERA)
                    _localVideoTrack.value = pub?.track as? LocalVideoTrack
                } else {
                    _localVideoTrack.value = null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle camera")
            }
        }
    }

    fun startScreenShare(mediaProjectionPermissionResultData: Intent) {
        val room = this.room ?: return
        coroutineScope.launch {
            try {
                room.localParticipant.setScreenShareEnabled(true, mediaProjectionPermissionResultData)
                _isScreenSharing.value = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to start screen share")
            }
        }
    }

    fun stopScreenShare() {
        val room = this.room ?: return
        coroutineScope.launch {
            try {
                room.localParticipant.setScreenShareEnabled(false, null)
                _isScreenSharing.value = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop screen share")
            }
        }
    }

    fun getRemoteVideoTrack(participant: RemoteParticipant): VideoTrack? {
        return participant.getTrackPublication(io.livekit.android.room.track.Track.Source.CAMERA)
            ?.track as? VideoTrack
    }

    fun isParticipantSpeaking(participantSid: String): Boolean = _speakingParticipants.value.contains(participantSid)

    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.ParticipantConnected -> {
                Timber.d("Participant connected: ${event.participant.identity}")
                room?.let { updateParticipants(it) }
            }
            is RoomEvent.ParticipantDisconnected -> {
                Timber.d("Participant disconnected: ${event.participant.identity}")
                room?.let { updateParticipants(it) }
                _speakingParticipants.value -= event.participant.sid.value
            }
            is RoomEvent.ActiveSpeakersChanged -> {
                _speakingParticipants.value = event.speakers.map { it.sid.value }.toSet()
            }
            is RoomEvent.TrackSubscribed -> {
                Timber.d("Track subscribed: ${event.track.name}")
                room?.let { updateParticipants(it) }
            }
            is RoomEvent.TrackUnsubscribed -> {
                Timber.d("Track unsubscribed: ${event.track.name}")
                room?.let { updateParticipants(it) }
            }
            is RoomEvent.Disconnected -> {
                _isConnected.value = false
            }
            is RoomEvent.FailedToConnect -> {
                Timber.e(event.error, "Failed to connect to room")
                _isConnected.value = false
            }
            else -> {}
        }
    }

    private fun updateParticipants(room: Room) {
        _participants.value = room.remoteParticipants.values.toList()
    }
}

data class LiveKitParticipant(
    val sid: String,
    val identity: String,
    val name: String?,
    val isSpeaking: Boolean,
    val isMuted: Boolean,
    val avatarUrl: String? = null
)
