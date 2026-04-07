package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.GatewayWebSocketManager
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
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

    // Selected server/guild
    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    // Messages for selected channel using Paging
    val messages: Flow<PagingData<Message>> = _selectedChannel
        .flatMapLatest { channel ->
            channel?.let { 
                chatRepository.getMessagesPaginated(it.id)
            } ?: flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<Message>>(emptyList())
    val searchResults: StateFlow<List<Message>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Guilds/Servers
    private val _guilds = MutableStateFlow<List<Server>>(emptyList())
    val guilds: StateFlow<List<Server>> = _guilds.asStateFlow()

    // Channels for selected guild
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    // Loading states
    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _isLoadingServers = MutableStateFlow(false)
    val isLoadingServers: StateFlow<Boolean> = _isLoadingServers.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Message input
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    // Reply state
    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    init {
        // Connect to Gateway when ViewModel is created
        chatRepository.connectGateway()
        
        // Collect Gateway events
        collectGatewayEvents()
        collectChannelCache()

        // Load servers/guilds from REST API (Gateway READY will populate channels)
        loadGuilds()

        // Debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collectLatest { query ->
                    if (query.length > 2) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
        
        // Reconnect Gateway when auth state becomes authenticated
        // This ensures the session token is available for IDENTIFY
        viewModelScope.launch {
            authRepository.authState
                .filter { it is AuthRepository.AuthState.Authenticated }
                .take(1) // Only act on first authentication
                .collect {
                    Timber.d("🔐 Auth state is Authenticated, ensuring Gateway connection")
                    if (connectionState.value == GatewayWebSocketManager.ConnectionState.Disconnected) {
                        chatRepository.connectGateway()
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        val channelId = _selectedChannel.value?.id ?: return
        viewModelScope.launch {
            _isSearching.value = true
            val result = chatRepository.searchMessages(channelId, query)
            if (result is com.fluxer.client.util.Result.Success) {
                _searchResults.value = result.data
            }
            _isSearching.value = false
        }
    }

    // !! DO NOT USE server.channels HERE !!
    // Guild objects from /api/users/@me/guilds always have channels = emptyList().
    // Channels must be populated from Gateway READY on Fluxer. See CLAUDE.md.
        fun selectServer(server: Server) {
        Timber.i("🖱️ selectServer called: ${server.name} (${server.id})")
        _selectedServer.value = server

        // Check if channels came with the server from Gateway READY event
        // Fluxer sends channels exclusively via Gateway, REST returns empty []
        if (server.channels.isNotEmpty()) {
            Timber.i("✅ Using ${server.channels.size} channels from Gateway READY for ${server.name}")
            _channels.value = server.channels
            if (_selectedChannel.value == null || _selectedChannel.value?.serverId != server.id) {
                _selectedChannel.value = server.channels.firstOrNull { it.type == ChannelType.TEXT }
                Timber.d("🎯 Auto-selected channel: ${_selectedChannel.value?.name}")
            }
        } else {
            val cached = chatRepository.getCachedGuildChannels(server.id)
            if (cached.isNotEmpty()) {
                Timber.i("✅ Using ${cached.size} cached channels for ${server.name}")
                _channels.value = cached
                if (_selectedChannel.value == null || _selectedChannel.value?.serverId != server.id) {
                    _selectedChannel.value = cached.firstOrNull { it.type == ChannelType.TEXT }
                }
            } else {
                Timber.d("📡 No cached channels yet for ${server.name} (waiting for READY)")
                _channels.value = emptyList()
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
    }

    fun sendMessage() {
        val content = _messageInput.value.trim()
        val channelId = _selectedChannel.value?.id ?: return
        
        if (content.isEmpty()) return

        viewModelScope.launch {
            // Clear input immediately for better UX
            _messageInput.value = ""
            val replyToId = _replyingTo.value?.id
            _replyingTo.value = null
            
            chatRepository.sendMessage(channelId, content, replyToId)
                .onError { error ->
                    _error.value = error
                    // Restore input on error
                    _messageInput.value = content
                }
        }
    }
    
    fun sendReply(replyToMessageId: String) {
        val content = _messageInput.value.trim()
        val channelId = _selectedChannel.value?.id ?: return
        
        if (content.isEmpty()) return

        viewModelScope.launch {
            _messageInput.value = ""
            _replyingTo.value = null
            
            chatRepository.sendMessage(channelId, content, replyToMessageId)
                .onError { error ->
                    _error.value = error
                    _messageInput.value = content
                }
        }
    }
    
    fun startReply(message: Message) {
        _replyingTo.value = message
    }
    
    fun cancelReply() {
        _replyingTo.value = null
    }
    
    fun jumpToMessage(messageId: String) {
        // TODO: Implement scroll to specific message
        Timber.d("Jump to message: $messageId")
    }
    
    fun addReaction(messageId: String, emoji: String) {
        val channelId = _selectedChannel.value?.id ?: return
        
        viewModelScope.launch {
            chatRepository.addReaction(channelId, messageId, emoji)
                .onError { error ->
                    _error.value = error
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

    private fun loadGuilds() {
        viewModelScope.launch {
            _isLoadingServers.value = true
            val result = chatRepository.getUserGuilds()
            result.onSuccess { servers ->
                _guilds.value = servers
                Timber.i("Loaded ${servers.size} guilds from REST API")
                if (servers.isNotEmpty() && _selectedServer.value == null) {
                    selectServer(servers.first())
                }
            }.onError { error ->
                Timber.e("Failed to load guilds: $error")
            }
            _isLoadingServers.value = false
        }
    }

    private fun collectGatewayEvents() {
        chatRepository.gatewayEvents
            .onEach { event ->
                when (event) {
                    is GatewayWebSocketManager.GatewayEvent.Ready -> {
                        _guilds.value = event.data.guilds
                        Timber.i("Gateway ready with ${event.data.guilds.size} guilds")
                        if (event.data.guilds.isNotEmpty() && _selectedServer.value == null) {
                            selectServer(event.data.guilds.first())
                        }
                    }
                    is GatewayWebSocketManager.GatewayEvent.MessageCreate -> {
                        // Message handled by repository cache
                    }
                    is GatewayWebSocketManager.GatewayEvent.MessageUpdate -> {
                        // Message handled by repository cache
                    }
                    is GatewayWebSocketManager.GatewayEvent.MessageDelete -> {
                        // Message handled by repository cache
                    }
                    else -> { /* Handle other events if needed */ }
                }
            }
            .launchIn(viewModelScope)
    }
    private fun collectChannelCache() {
        chatRepository.channelCacheFlow
            .onEach { cache ->
                val selected = _selectedServer.value ?: return@onEach
                val channels = cache[selected.id].orEmpty()
                if (channels.isNotEmpty()) {
                    _channels.value = channels
                    if (_selectedChannel.value == null || _selectedChannel.value?.serverId != selected.id) {
                        _selectedChannel.value = channels.firstOrNull { it.type == ChannelType.TEXT }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}


