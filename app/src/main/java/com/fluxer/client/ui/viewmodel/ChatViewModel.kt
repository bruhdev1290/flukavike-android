package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.GatewayWebSocketManager
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Current user
    val currentUser: StateFlow<com.fluxer.client.data.model.User?> = authRepository.authState
        .map { state ->
            (state as? AuthRepository.AuthState.Authenticated)?.user
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    // Connection state
    val connectionState: StateFlow<GatewayWebSocketManager.ConnectionState> = 
        chatRepository.connectionState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 
                GatewayWebSocketManager.ConnectionState.Disconnected)

    // Selected channel
    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    // Messages for selected channel
    val messages: StateFlow<List<Message>> = _selectedChannel
        .flatMapLatest { channel ->
            channel?.let { 
                chatRepository.getMessagesFlow(it.id)
            } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Guilds/Servers
    private val _guilds = MutableStateFlow<List<Server>>(emptyList())
    val guilds: StateFlow<List<Server>> = _guilds.asStateFlow()

    // Channels for selected guild
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    // Loading states
    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Message input
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    init {
        // Connect to Gateway when ViewModel is created
        chatRepository.connectGateway()
        
        // Collect Gateway events
        collectGatewayEvents()
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        loadMessages(channel.id)
    }

    fun loadMessages(channelId: String, before: String? = null) {
        viewModelScope.launch {
            _isLoadingMessages.value = true
            
            chatRepository.getMessages(channelId, before = before)
                .onSuccess { messages ->
                    Timber.d("Loaded ${messages.size} messages")
                }
                .onError { error ->
                    _error.value = error
                }
            
            _isLoadingMessages.value = false
        }
    }

    fun sendMessage() {
        val content = _messageInput.value.trim()
        val channelId = _selectedChannel.value?.id ?: return
        
        if (content.isEmpty()) return

        viewModelScope.launch {
            // Clear input immediately for better UX
            _messageInput.value = ""
            
            chatRepository.sendMessage(channelId, content)
                .onError { error ->
                    _error.value = error
                    // Restore input on error
                    _messageInput.value = content
                }
        }
    }

    fun deleteMessage(messageId: String) {
        val channelId = _selectedChannel.value?.id ?: return
        
        viewModelScope.launch {
            chatRepository.deleteMessage(channelId, messageId)
                .onError { error ->
                    _error.value = error
                }
        }
    }

    fun updateMessageInput(text: String) {
        _messageInput.value = text
    }

    fun updatePresence(status: UserStatus) {
        chatRepository.updatePresence(status)
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnectGateway()
    }

    private fun collectGatewayEvents() {
        chatRepository.gatewayEvents
            .onEach { event ->
                when (event) {
                    is GatewayWebSocketManager.GatewayEvent.Ready -> {
                        _guilds.value = event.data.guilds
                        Timber.i("Gateway ready with ${event.data.guilds.size} guilds")
                    }
                    else -> { /* Handle other events if needed */ }
                }
            }
            .launchIn(viewModelScope)
    }
}
