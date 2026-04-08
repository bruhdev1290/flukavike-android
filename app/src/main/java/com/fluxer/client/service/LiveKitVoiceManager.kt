package com.fluxer.client.service

import android.content.Context
import io.livekit.android.AudioType
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.AudioTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.TrackPublication
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

/**
 * LiveKit Voice Manager for handling voice channel connections
 */
@Singleton
class LiveKitVoiceManager @Inject constructor(
    context: Context
) {
    private var room: Room? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val appContext = context.applicationContext
    
    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Local participant state
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    private val _isDeafened = MutableStateFlow(false)
    val isDeafened: StateFlow<Boolean> = _isDeafened.asStateFlow()
    
    // Remote participants
    private val _participants = MutableStateFlow<List<RemoteParticipant>>(emptyList())
    val participants: StateFlow<List<RemoteParticipant>> = _participants.asStateFlow()
    
    // Speaking participants
    private val _speakingParticipants = MutableStateFlow<Set<String>>(emptySet())
    val speakingParticipants: StateFlow<Set<String>> = _speakingParticipants.asStateFlow()
    
    // Current room name/channel ID
    private var currentRoomName: String? = null
    
    // Audio handler for managing audio routing
    private val audioHandler = AudioSwitchHandler(appContext)
    
    /**
     * Connect to a LiveKit room
     */
    suspend fun connect(url: String, token: String, roomName: String): Result<Unit> {
        return try {
            if (room != null) {
                disconnect()
            }
            
            currentRoomName = roomName
            
            // Create room with options
            val roomOptions = RoomOptions(
                adaptiveStream = true,
                dynacast = true
            )
            
            val newRoom = LiveKit.create(
                appContext,
                options = roomOptions
            )
            
            // Note: Audio handler setup may vary by LiveKit version
            // newRoom.audioHandler = audioHandler
            
            room = newRoom
            
            // Collect room events
            coroutineScope.launch {
                newRoom.events.collect { event ->
                    handleRoomEvent(event)
                }
            }
            
            // Connect to room
            newRoom.connect(url, token)
            
            // Enable microphone
            newRoom.localParticipant.setMicrophoneEnabled(true)
            
            _isConnected.value = true
            _isMuted.value = false
            
            // Update participants list
            updateParticipants(newRoom)
            
            Timber.d("Connected to LiveKit room: $roomName")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to LiveKit room")
            Result.failure(e)
        }
    }
    
    /**
     * Disconnect from the current room
     */
    fun disconnect() {
        try {
            room?.disconnect()
            room = null
            _isConnected.value = false
            _isMuted.value = false
            _isDeafened.value = false
            _participants.value = emptyList()
            _speakingParticipants.value = emptySet()
            currentRoomName = null
            Timber.d("Disconnected from LiveKit room")
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from room")
        }
    }
    
    /**
     * Toggle microphone mute
     */
    fun toggleMute() {
        val room = this.room ?: return
        val newMuteState = !_isMuted.value
        
        coroutineScope.launch {
            try {
                room.localParticipant.setMicrophoneEnabled(!newMuteState)
                _isMuted.value = newMuteState
                Timber.d("Microphone ${if (newMuteState) "muted" else "unmuted"}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle mute")
            }
        }
    }
    
    /**
     * Toggle deafen (mute microphone only - audio output control varies by device)
     */
    fun toggleDeafen() {
        val newDeafenState = !_isDeafened.value
        _isDeafened.value = newDeafenState
        
        // Mute microphone when deafened
        if (newDeafenState && !_isMuted.value) {
            toggleMute()
        }
        // Unmute when undeafening (if we were muted by deafen)
        else if (!newDeafenState && _isMuted.value) {
            toggleMute()
        }
        
        Timber.d("${if (newDeafenState) "Deafened" else "Undeafened"}")
    }
    
    /**
     * Check if a participant is currently speaking
     */
    fun isParticipantSpeaking(participantSid: String): Boolean {
        return _speakingParticipants.value.contains(participantSid)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }
    
    /**
     * Handle room events
     */
    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.ParticipantConnected -> {
                Timber.d("Participant connected: ${event.participant.identity}")
                room?.let { updateParticipants(it) }
            }
            
            is RoomEvent.ParticipantDisconnected -> {
                Timber.d("Participant disconnected: ${event.participant.identity}")
                room?.let { updateParticipants(it) }
                _speakingParticipants.value = _speakingParticipants.value - event.participant.sid.value
            }
            
            is RoomEvent.ActiveSpeakersChanged -> {
                val speakingSids = event.speakers.map { it.sid.value }.toSet()
                _speakingParticipants.value = speakingSids
                Timber.d("Active speakers: ${event.speakers.map { it.identity }}")
            }
            
            is RoomEvent.TrackSubscribed -> {
                Timber.d("Track subscribed: ${event.track.name}")
            }
            
            is RoomEvent.TrackUnsubscribed -> {
                Timber.d("Track unsubscribed: ${event.track.name}")
            }
            
            is RoomEvent.Disconnected -> {
                Timber.d("Disconnected from room")
                _isConnected.value = false
            }
            
            is RoomEvent.FailedToConnect -> {
                Timber.e(event.error, "Failed to connect to room")
                _isConnected.value = false
            }
            
            else -> {
                // Handle other events if needed
            }
        }
    }
    
    /**
     * Update participants list
     */
    private fun updateParticipants(room: Room) {
        val remoteParticipants = room.remoteParticipants.values.toList()
        _participants.value = remoteParticipants
        Timber.d("Updated participants: ${remoteParticipants.size} remote participants")
    }
}

/**
 * Data class for voice participant info
 */
data class LiveKitParticipant(
    val sid: String,
    val identity: String,
    val name: String?,
    val isSpeaking: Boolean,
    val isMuted: Boolean,
    val avatarUrl: String? = null
)
