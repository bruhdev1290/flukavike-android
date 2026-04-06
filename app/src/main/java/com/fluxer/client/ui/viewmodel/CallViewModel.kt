package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Call
import com.fluxer.client.data.model.CallParticipant
import com.fluxer.client.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _call = MutableStateFlow<Call?>(null)
    val call: StateFlow<Call?> = _call.asStateFlow()

    private val _participants = MutableStateFlow<List<CallParticipant>>(emptyList())
    val participants: StateFlow<List<CallParticipant>> = _participants.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isVideoOn = MutableStateFlow(false)
    val isVideoOn: StateFlow<Boolean> = _isVideoOn.asStateFlow()

    private var callStartTime: Long? = null

    fun joinCall(callId: String) {
        viewModelScope.launch {
            val result = chatRepository.joinCall(callId, "")
            result.onSuccess { response ->
                _call.value = response.call
                _participants.value = response.call.participants
                callStartTime = System.currentTimeMillis()
                startDurationTimer()
            }.onError { error ->
                Timber.e("Failed to join call: $error")
            }
        }
    }

    fun endCall() {
        _call.value?.let { call ->
            viewModelScope.launch {
                chatRepository.leaveCall(call.id)
            }
        }
        _call.value = null
        _participants.value = emptyList()
        callStartTime = null
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        // TODO: Apply mute to WebRTC audio track
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        // TODO: Toggle audio output between speaker and earpiece
    }

    fun toggleVideo() {
        _isVideoOn.value = !_isVideoOn.value
        // TODO: Enable/disable video track
    }

    private fun startDurationTimer() {
        viewModelScope.launch {
            while (callStartTime != null) {
                val elapsed = (System.currentTimeMillis() - (callStartTime ?: 0)) / 1000
                _callDuration.value = elapsed
                delay(1000)
            }
        }
    }

    fun initiateCall(recipientId: String? = null, channelId: String? = null) {
        viewModelScope.launch {
            val result = chatRepository.initiateCall(recipientId, channelId)
            result.onSuccess { response ->
                _call.value = response.call
                _participants.value = response.call.participants
                callStartTime = System.currentTimeMillis()
                startDurationTimer()
            }.onError { error ->
                Timber.e("Failed to initiate call: $error")
            }
        }
    }

    fun declineCall(callId: String) {
        viewModelScope.launch {
            chatRepository.declineCall(callId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        endCall()
    }
}
