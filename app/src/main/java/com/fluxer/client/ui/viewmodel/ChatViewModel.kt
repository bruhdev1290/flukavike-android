package com.fluxer.client.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.GatewayWebSocketManager
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.GuildManagementRepository
import com.fluxer.client.data.repository.HomeStateRepository
import com.fluxer.client.data.repository.ProfileRepository
import com.fluxer.client.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val homeStateRepository: HomeStateRepository,
    private val guildManagementRepository: GuildManagementRepository,
    private val profileRepository: ProfileRepository,
    private val instanceConfigStore: InstanceConfigStore
) : ViewModel() {

    val cdnBaseUrl: String? get() = instanceConfigStore.getCdnBaseUrl()

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

    // Server profile
    private val _serverProfile = MutableStateFlow<com.fluxer.client.data.model.ServerProfile?>(null)
    val serverProfile: StateFlow<com.fluxer.client.data.model.ServerProfile?> = _serverProfile.asStateFlow()

    // Refresh trigger for when messages change
    private val _refreshTrigger = MutableStateFlow(0)
    
    // Messages for selected channel using Paging
    val messages: Flow<PagingData<Message>> = combine(
        _selectedChannel,
        _refreshTrigger
    ) { channel, _ ->
        channel
    }
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

    val favoriteChannelIds: StateFlow<Set<String>> = homeStateRepository.favoriteChannelIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val unreadCountsByChannel: StateFlow<Map<String, Int>> = homeStateRepository.unreadCountsByChannel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    // Pending file attachment
    private val _pendingAttachmentUri = MutableStateFlow<Uri?>(null)
    val pendingAttachmentUri: StateFlow<Uri?> = _pendingAttachmentUri.asStateFlow()

    private val _pendingAttachmentMetadata = MutableStateFlow<AttachmentMetadata?>(null)
    val pendingAttachmentMetadata: StateFlow<AttachmentMetadata?> = _pendingAttachmentMetadata.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    // Typing indicators: channelId → (userId → display name)
    private val _typingUsers = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    private val _typingClearJobs = mutableMapOf<String, Job>()

    val typingUsersInChannel: StateFlow<List<String>> = combine(
        _typingUsers,
        _selectedChannel,
        currentUser
    ) { typingMap, channel, me ->
        val channelId = channel?.id ?: return@combine emptyList()
        typingMap[channelId]
            ?.filter { (userId, _) -> userId != me?.id }
            ?.values
            ?.toList()
            .orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

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
    
    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
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

        // Load server profile when a server is selected
        loadServerProfile(server.id)

        // Check if channels came with the server from Gateway READY event
        // Fluxer sends channels exclusively via Gateway, REST returns empty []
        if (server.channels.isNotEmpty()) {
            Timber.i("✅ Using ${server.channels.size} channels from Gateway READY for ${server.name}")
            _channels.value = server.channels
            if (_selectedChannel.value == null || _selectedChannel.value?.serverId != server.id) {
                _selectedChannel.value = preferredGuildChannel(server.channels)
                Timber.d("🎯 Auto-selected channel: ${_selectedChannel.value?.displayName()}")
            }
        } else {
            val cached = chatRepository.getCachedGuildChannels(server.id)
            if (cached.isNotEmpty()) {
                Timber.i("✅ Using ${cached.size} cached channels for ${server.name}")
                _channels.value = cached
                if (_selectedChannel.value == null || _selectedChannel.value?.serverId != server.id) {
                    _selectedChannel.value = preferredGuildChannel(cached)
                }
            } else {
                Timber.d("📡 No cached channels yet for ${server.name} (waiting for READY)")
                _channels.value = emptyList()
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        viewModelScope.launch {
            homeStateRepository.ackChannelLatest(channel.id, channel.lastMessageId)
        }
    }

    fun toggleFavoriteForSelectedChannel() {
        val channel = _selectedChannel.value ?: return
        viewModelScope.launch {
            homeStateRepository.toggleFavorite(channel.id, channel.serverId)
        }
    }

    fun selectServerById(guildId: String) {
        val server = _guilds.value.firstOrNull { it.id == guildId } ?: return
        selectServer(server)
    }

    fun selectChannelById(channelId: String, guildId: String? = null) {
        guildId?.let { selectServerById(it) }
        val channel = _channels.value.firstOrNull { it.id == channelId }
            ?: _guilds.value.asSequence()
                .flatMap { it.channels.asSequence() }
                .firstOrNull { it.id == channelId }
            ?: chatRepository.getCachedChannel(channelId)
            ?: return
        if (channel.serverId != null && _selectedServer.value?.id != channel.serverId) {
            selectServerById(channel.serverId)
        }
        _selectedChannel.value = channel
    }

    fun sendMessage() {
        val content = _messageInput.value.trim()
        val channelId = _selectedChannel.value?.id ?: return
        val attachment = _pendingAttachmentUri.value

        if (content.isEmpty() && attachment == null) return

        viewModelScope.launch {
            _messageInput.value = ""
            val replyToId = _replyingTo.value?.id
            _replyingTo.value = null
            _pendingAttachmentUri.value = null
            _pendingAttachmentMetadata.value = null
            _isSendingMessage.value = true
            _uploadProgress.value = if (attachment != null) 0.1f else null

            val result = if (attachment != null) {
                _uploadProgress.value = 0.35f
                chatRepository.sendMessageWithAttachment(channelId, content, replyToId, attachment)
            } else {
                chatRepository.sendMessage(channelId, content, replyToId)
            }
            if (attachment != null) _uploadProgress.value = 0.9f

            result
                .onSuccess {
                    _uploadProgress.value = 1f
                    _refreshTrigger.value += 1
                }
                .onError { error ->
                    _error.value = error
                    _messageInput.value = content
                    _pendingAttachmentUri.value = attachment
                    _pendingAttachmentMetadata.value = attachment?.let { chatRepository.getAttachmentMetadata(it) }
                }
            _isSendingMessage.value = false
            _uploadProgress.value = null
        }
    }

    fun setPendingAttachment(uri: Uri?) {
        _pendingAttachmentUri.value = uri
        _pendingAttachmentMetadata.value = uri?.let { chatRepository.getAttachmentMetadata(it) }
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
        if (emoji.isBlank()) return
        val channelId = _selectedChannel.value?.id ?: return
        viewModelScope.launch {
            chatRepository.addReaction(channelId, messageId, emoji)
                .onError { error -> _error.value = error }
        }
    }

    // ==================== MESSAGE EDITING ====================

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    fun startEditMessage(message: Message) {
        _editingMessage.value = message
        _messageInput.value = message.content
    }

    fun cancelEdit() {
        _editingMessage.value = null
        _messageInput.value = ""
    }

    fun submitEdit() {
        val msg = _editingMessage.value ?: return
        val channelId = _selectedChannel.value?.id ?: return
        val newContent = _messageInput.value.trim()
        if (newContent.isBlank() || newContent == msg.content) { cancelEdit(); return }
        viewModelScope.launch {
            chatRepository.editMessage(channelId, msg.id, newContent)
                .onSuccess { _refreshTrigger.value += 1 }
                .onError { error -> _error.value = error }
            cancelEdit()
        }
    }

    // ==================== PINS ====================

    private val _pinnedMessages = MutableStateFlow<List<Message>>(emptyList())
    val pinnedMessages: StateFlow<List<Message>> = _pinnedMessages.asStateFlow()

    private val _showPinnedMessages = MutableStateFlow(false)
    val showPinnedMessages: StateFlow<Boolean> = _showPinnedMessages.asStateFlow()

    fun togglePinnedMessages() { _showPinnedMessages.value = !_showPinnedMessages.value }

    fun loadPinnedMessages() {
        val channelId = _selectedChannel.value?.id ?: return
        viewModelScope.launch {
            chatRepository.getPinnedMessages(channelId)
                .onSuccess { _pinnedMessages.value = it }
                .onError { _error.value = it }
        }
    }

    fun pinMessage(messageId: String) {
        val channelId = _selectedChannel.value?.id ?: return
        viewModelScope.launch {
            chatRepository.pinMessage(channelId, messageId)
                .onError { _error.value = it }
        }
    }

    fun unpinMessage(messageId: String) {
        val channelId = _selectedChannel.value?.id ?: return
        viewModelScope.launch {
            chatRepository.unpinMessage(channelId, messageId)
                .onSuccess { _pinnedMessages.value = _pinnedMessages.value.filter { it.id != messageId } }
                .onError { _error.value = it }
        }
    }

    // ==================== GUILD MEMBERS ====================

    private val _guildMembers = MutableStateFlow<List<com.fluxer.client.data.model.GuildMember>>(emptyList())
    val guildMembers: StateFlow<List<com.fluxer.client.data.model.GuildMember>> = _guildMembers.asStateFlow()

    private val _showMemberList = MutableStateFlow(false)
    val showMemberList: StateFlow<Boolean> = _showMemberList.asStateFlow()

    fun toggleMemberList() {
        _showMemberList.value = !_showMemberList.value
        if (_showMemberList.value) loadGuildMembers()
    }

    private fun loadGuildMembers() {
        val guildId = _selectedServer.value?.id ?: return
        viewModelScope.launch {
            chatRepository.getGuildMembers(guildId)
                .onSuccess { _guildMembers.value = it }
                .onError { _error.value = it }
        }
    }

    // ==================== CUSTOM STATUS ====================

    fun setCustomStatus(status: String?) {
        viewModelScope.launch {
            chatRepository.setCustomStatus(status)
                .onError { _error.value = it }
        }
    }

    // ==================== INVITE / JOIN ====================

    private val _invitePreview = MutableStateFlow<com.fluxer.client.data.model.InviteInfo?>(null)
    val invitePreview: StateFlow<com.fluxer.client.data.model.InviteInfo?> = _invitePreview.asStateFlow()

    fun previewInvite(code: String) {
        viewModelScope.launch {
            chatRepository.previewInvite(code.trim())
                .onSuccess { _invitePreview.value = it }
                .onError { _error.value = it }
        }
    }

    fun joinViaInvite(code: String, onJoined: () -> Unit = {}) {
        viewModelScope.launch {
            chatRepository.joinViaInvite(code.trim())
                .onSuccess {
                    _invitePreview.value = null
                    loadGuilds()
                    onJoined()
                }
                .onError { _error.value = it }
        }
    }

    fun clearInvitePreview() { _invitePreview.value = null }

    fun removeReaction(messageId: String, emoji: String) {
        if (emoji.isBlank()) return
        val channelId = _selectedChannel.value?.id ?: return
        viewModelScope.launch {
            chatRepository.removeReaction(channelId, messageId, emoji)
                .onError { error -> _error.value = error }
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
                        _guilds.value = mergeGuilds(_guilds.value, event.data.guilds)
                        Timber.i("Gateway ready with ${event.data.guilds.size} guilds")
                        if (_guilds.value.isNotEmpty() && _selectedServer.value == null) {
                            selectServer(_guilds.value.first())
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
                    is GatewayWebSocketManager.GatewayEvent.GuildCreate -> {
                        val current = _guilds.value.toMutableList()
                        val existingIndex = current.indexOfFirst { it.id == event.guild.id }
                        if (existingIndex >= 0) {
                            current[existingIndex] = event.guild
                        } else {
                            current.add(event.guild)
                        }
                        _guilds.value = current
                    }
                    is GatewayWebSocketManager.GatewayEvent.GuildUpdate -> {
                        val current = _guilds.value.toMutableList()
                        val index = current.indexOfFirst { it.id == event.guild.id }
                        if (index >= 0) {
                            // Preserve channels if the update doesn't include them
                            val updatedGuild = if (event.guild.channels.isEmpty()) {
                                event.guild.copy(channels = current[index].channels)
                            } else {
                                event.guild
                            }
                            current[index] = updatedGuild
                            _guilds.value = current
                            if (_selectedServer.value?.id == updatedGuild.id) {
                                _selectedServer.value = updatedGuild
                            }
                        }
                    }
                    is GatewayWebSocketManager.GatewayEvent.GuildDelete -> {
                        _guilds.value = _guilds.value.filter { it.id != event.guildId }
                        if (_selectedServer.value?.id == event.guildId) {
                            _selectedServer.value = null
                            _selectedChannel.value = null
                            _channels.value = emptyList()
                        }
                    }
                    is GatewayWebSocketManager.GatewayEvent.TypingStart -> {
                        val typing = event.data
                        val displayName = typing.member?.nick
                            ?: typing.member?.user?.username
                            ?: typing.userId
                        val jobKey = "${typing.channelId}:${typing.userId}"
                        _typingClearJobs[jobKey]?.cancel()
                        _typingUsers.update { map ->
                            val channel = map[typing.channelId]?.toMutableMap() ?: mutableMapOf()
                            channel[typing.userId] = displayName
                            map + (typing.channelId to channel)
                        }
                        _typingClearJobs[jobKey] = viewModelScope.launch {
                            delay(10_000)
                            _typingUsers.update { map ->
                                val channel = map[typing.channelId]?.toMutableMap() ?: return@update map
                                channel.remove(typing.userId)
                                if (channel.isEmpty()) map - typing.channelId
                                else map + (typing.channelId to channel)
                            }
                        }
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
                        _selectedChannel.value = preferredGuildChannel(channels)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadServerProfile(guildId: String?) {
        if (guildId.isNullOrBlank()) {
            _serverProfile.value = null
            return
        }
        viewModelScope.launch {
            profileRepository.getServerProfile(guildId)
                .onSuccess { profile ->
                    _serverProfile.value = profile
                    // Merge profile fields back into the selected server so the UI reflects them
                    _selectedServer.value?.takeIf { it.id == profile.id }?.let { selected ->
                        _selectedServer.value = selected.copy(
                            description = profile.description,
                            bannerUrl = profile.bannerUrl,
                            vanityUrl = profile.vanityUrl,
                            memberCount = if (profile.memberCount != 0) profile.memberCount else selected.memberCount,
                            onlineCount = if (profile.onlineCount != 0) profile.onlineCount else selected.onlineCount
                        )
                    }
                }
                .onError { Timber.w("Failed to load server profile for $guildId: $it") }
        }
    }

    private fun preferredGuildChannel(channels: List<Channel>): Channel? =
        channels.firstOrNull { it.type == ChannelType.TEXT }
            ?: channels.firstOrNull { it.type == ChannelType.VOICE }
            ?: channels.firstOrNull { it.type != ChannelType.CATEGORY && it.type != ChannelType.UNKNOWN }

    private fun mergeGuilds(existing: List<Server>, incoming: List<Server>): List<Server> {
        if (existing.isEmpty()) return incoming
        val merged = existing.associateBy { it.id }.toMutableMap()
        incoming.forEach { guild ->
            val prior = merged[guild.id]
            merged[guild.id] = if (prior == null) {
                guild
            } else {
                guild.copy(
                    channels = if (guild.channels.isNotEmpty()) guild.channels else prior.channels,
                    iconUrl = guild.iconUrl ?: prior.iconUrl,
                    memberCount = if (guild.memberCount != 0) guild.memberCount else prior.memberCount,
                    onlineCount = if (guild.onlineCount != 0) guild.onlineCount else prior.onlineCount
                )
            }
        }
        return merged.values.toList()
    }

    fun createServer(name: String, onCreated: ((Server) -> Unit)? = null) {
        viewModelScope.launch {
            guildManagementRepository.createServer(name)
                .onSuccess { server ->
                    _guilds.value = (_guilds.value + server).distinctBy { it.id }
                    onCreated?.invoke(server)
                }
                .onError { error -> _error.value = error }
        }
    }

    fun createChannel(name: String, isVoice: Boolean, onCreated: ((Channel) -> Unit)? = null) {
        val guildId = _selectedServer.value?.id ?: return
        viewModelScope.launch {
            guildManagementRepository.createChannel(guildId, name, isVoice)
                .onSuccess { channel ->
                    _channels.value = (_channels.value + channel).distinctBy { it.id }
                    onCreated?.invoke(channel)
                }
                .onError { error -> _error.value = error }
        }
    }
}
