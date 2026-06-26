package com.fluxer.client.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.fluxer.client.data.local.dao.DmChannelDao
import com.fluxer.client.data.local.model.DmChannelEntity
import com.fluxer.client.data.paging.MessagePagingSource
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.*
import com.fluxer.client.data.model.InviteInfo
import com.fluxer.client.data.model.UpdateProfileRequest
import com.fluxer.client.data.model.GuildMember
import com.fluxer.client.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

private const val MESSAGE_PAGE_SIZE = 25
private const val MAX_ATTACHMENT_BYTES = 25L * 1024L * 1024L

/**
 * Repository for chat-related operations including messages and channels.
 * Bridges REST API and WebSocket Gateway.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val apiService: FluxerApiService,
    private val uploadApiService: UploadApiService,
    private val pinApiService: PinApiService,
    private val guildMembersApiService: GuildMembersApiService,
    private val inviteApiService: InviteApiService,
    private val avatarApiService: AvatarApiService,
    private val gatewayManager: GatewayWebSocketManager,
    private val dmChannelDao: DmChannelDao,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Cache for messages per channel
    private val messageCache = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    
    // Cache for channels per guild
    private val channelCache = mutableMapOf<String, List<Channel>>()
    private val _channelCacheFlow =
        MutableStateFlow<Map<String, List<Channel>>>(emptyMap())
    val channelCacheFlow: StateFlow<Map<String, List<Channel>>> =
        _channelCacheFlow.asStateFlow()

    private val _dmChannelsFlow = MutableStateFlow<List<Channel>>(emptyList())
    val dmChannelsFlow: StateFlow<List<Channel>> = _dmChannelsFlow.asStateFlow()
    
    // Voice state cache per channel
    private val voiceStateCache = mutableMapOf<String, MutableStateFlow<List<com.fluxer.client.data.model.VoiceStateUpdateEvent>>>()
    private val _voiceServerUpdates = MutableSharedFlow<com.fluxer.client.data.model.VoiceServerUpdateEvent>(extraBufferCapacity = 1)
    val voiceServerUpdates: Flow<com.fluxer.client.data.model.VoiceServerUpdateEvent> = _voiceServerUpdates.asSharedFlow()
    
    // Call state cache per channel
    private val callStateCache = mutableMapOf<String, MutableStateFlow<com.fluxer.client.data.model.CallEvent?>>()
    private val _callUpdates = MutableSharedFlow<com.fluxer.client.data.model.CallEvent>(extraBufferCapacity = 1)
    val callUpdates: Flow<com.fluxer.client.data.model.CallEvent> = _callUpdates.asSharedFlow()
    private val _callDeletes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val callDeletes: Flow<String> = _callDeletes.asSharedFlow()
    
    // Expose Gateway events as repository events
    val gatewayEvents: Flow<GatewayWebSocketManager.GatewayEvent> = gatewayManager.events
    
    // Connection state
    val connectionState: Flow<GatewayWebSocketManager.ConnectionState> = 
        gatewayManager.connectionState

    init {
        // Subscribe to Gateway events to update local cache
        collectGatewayEvents()
        restoreDmChannels()
    }

    /**
     * Get messages for a channel with pagination.
     */
    fun getMessagesPaginated(channelId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(
                pageSize = MESSAGE_PAGE_SIZE,
                initialLoadSize = MESSAGE_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { MessagePagingSource(apiService, channelId) }
        ).flow
    }

    /**
     * Search messages in a channel.
     */
    suspend fun searchMessages(channelId: String, query: String): Result<List<Message>> {
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        return try {
            val response = apiService.searchMessages(channelId, query)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Search failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error during search: ${e.message}")
        }
    }

    /**
     * Get messages flow for a channel (observable)
     */
    fun getMessagesFlow(channelId: String): StateFlow<List<Message>> {
        return messageCache.getOrPut(channelId) { 
            MutableStateFlow(emptyList()) 
        }
    }

    /**
     * Send a message to a channel
     */
    suspend fun sendMessage(
        channelId: String,
        content: String,
        replyToId: String? = null
    ): Result<Message> {
        return try {
            val request = SendMessageRequest(
                content = content,
                replyToId = replyToId
            )
            
            val response = apiService.sendMessage(channelId, request)
            
            if (response.isSuccessful) {
                val message = response.body()
                message?.let {
                    // Optimistically add to cache
                    addMessageToCache(channelId, it)
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to send message: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    /**
     * Edit a message
     */
    suspend fun editMessage(
        channelId: String,
        messageId: String,
        newContent: String
    ): Result<Message> {
        return try {
            val request = SendMessageRequest(content = newContent)
            val response = apiService.editMessage(channelId, messageId, request)
            
            if (response.isSuccessful) {
                val message = response.body()
                message?.let {
                    updateMessageInCache(channelId, it)
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to edit message: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    /**
     * Delete a message
     */
    suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit> {
        return try {
            val response = apiService.deleteMessage(channelId, messageId)
            
            if (response.isSuccessful) {
                removeMessageFromCache(channelId, messageId)
                Result.Success(Unit)
            } else {
                Result.Error("Failed to delete message: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    /**
     * Get user's guilds/servers
     */
    suspend fun getUserGuilds(): Result<List<Server>> {
        return try {
            val response = apiService.getUserGuilds()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load servers: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    /**
     * Get channels for a guild/server
     */
    suspend fun getGuildChannels(guildId: String): Result<List<Channel>> {
        val cached = channelCache[guildId].orEmpty()
        return if (cached.isNotEmpty()) {
            Result.Success(cached)
        } else {
            Result.Error(
                "Guild channels are provided by the Gateway READY payload on Fluxer. " +
                    "Connect the gateway and wait for READY before requesting channels."
            )
        }
    }

    /**
     * Get cached channels for a guild/server (non-suspending).
     */
    fun getCachedGuildChannels(guildId: String): List<Channel> =
        channelCache[guildId].orEmpty()

    fun getCachedChannel(channelId: String): Channel? =
        _dmChannelsFlow.value.firstOrNull { it.id == channelId }
            ?: channelCache.values.asSequence()
                .flatMap { it.asSequence() }
                .firstOrNull { it.id == channelId }

    /**
     * Get a specific channel
     */
    suspend fun getChannel(channelId: String): Result<Channel> {
        return try {
            val response = apiService.getChannel(channelId)
            
            if (response.isSuccessful) {
                val channel = response.body()
                channel?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to load channel: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    /**
     * Update presence status
     */
    fun updatePresence(status: UserStatus, customStatus: String? = null) {
        gatewayManager.updatePresence(status, customStatus)
    }

    /**
     * Connect to Gateway
     */
    fun connectGateway() {
        gatewayManager.connect()
    }

    /**
     * Disconnect from Gateway
     */
    fun disconnectGateway() {
        gatewayManager.disconnect()
    }

    // ==================== DIRECT MESSAGES ====================

    suspend fun getDMChannels(): Result<List<Channel>> {
        return try {
            val response = apiService.getDMChannels()
            if (response.isSuccessful) {
                val channels = response.body() ?: emptyList()
                cacheDmChannels(channels)
                Result.Success(channels)
            } else {
                Result.Error("Failed to load DM channels: ${response.code()}")
            }
        } catch (e: Exception) {
            val cached = _dmChannelsFlow.value
            if (cached.isNotEmpty()) {
                Result.Success(cached)
            } else {
                Result.Error("Network error: ${e.message}")
            }
        }
    }

    suspend fun createDMChannel(recipientId: String): Result<Channel> {
        return try {
            val request = CreateDMRequest(recipientId = recipientId)
            val response = apiService.createDMChannel(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    cacheDmChannels(listOf(it), replace = false)
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to create DM: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    fun ensurePersonalNotesChannel(user: User): Channel {
        val channel = Channel(
            id = user.id,
            name = "Personal Notes",
            type = ChannelType.DM,
            serverId = null,
            recipients = listOf(user),
            ownerId = user.id
        )
        cacheDmChannels(listOf(channel), replace = false)
        return channel
    }

    // ==================== VOICE CHANNELS ====================

    suspend fun joinVoiceChannel(channelId: String): Result<VoiceTokenResponse> {
        return try {
            val request = JoinVoiceChannelRequest(channelId = channelId)
            val response = apiService.joinVoiceChannel(channelId, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to join voice: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun leaveVoiceChannel(channelId: String): Result<Unit> {
        return try {
            val response = apiService.leaveVoiceChannel(channelId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to leave voice: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun getVoiceParticipants(channelId: String): Result<List<VoiceParticipant>> {
        return try {
            val response = apiService.getVoiceParticipants(channelId)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load participants: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun updateVoiceState(
        channelId: String,
        selfMute: Boolean? = null,
        selfDeaf: Boolean? = null,
        selfVideo: Boolean? = null
    ): Result<VoiceState> {
        return try {
            val request = UpdateVoiceStateRequest(
                selfMute = selfMute,
                selfDeaf = selfDeaf,
                selfVideo = selfVideo
            )
            val response = apiService.updateVoiceState(channelId, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to update voice state: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    // ==================== CALLS ====================

    suspend fun initiateCall(
        recipientId: String? = null,
        channelId: String? = null,
        type: CallType = CallType.VOICE
    ): Result<CallResponse> {
        return try {
            val request = InitiateCallRequest(
                recipientId = recipientId,
                channelId = channelId,
                type = type
            )
            val response = apiService.initiateCall(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to initiate call: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun joinCall(callId: String, token: String): Result<CallResponse> {
        return try {
            val request = JoinCallRequest(callId = callId, token = token)
            val response = apiService.joinCall(callId, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to join call: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun leaveCall(callId: String): Result<Unit> {
        return try {
            val response = apiService.leaveCall(callId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to leave call: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun declineCall(callId: String): Result<Unit> {
        return try {
            val response = apiService.declineCall(callId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to decline call: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    // ==================== REACTIONS ====================

    suspend fun addReaction(
        channelId: String,
        messageId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            // URL-encode the emoji for the API path
            val encodedEmoji = URLEncoder.encode(emoji, "UTF-8")
            val response = apiService.addReaction(channelId, messageId, encodedEmoji)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to add reaction: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun removeReaction(
        channelId: String,
        messageId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val encodedEmoji = URLEncoder.encode(emoji, "UTF-8")
            val response = apiService.removeReaction(channelId, messageId, encodedEmoji)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to remove reaction: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun sendMessageWithAttachment(
        channelId: String,
        content: String,
        replyToId: String?,
        fileUri: Uri
    ): Result<Message> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
            val fileName = getDisplayName(fileUri) ?: fileUri.lastPathSegment ?: "attachment"
            val declaredSize = getFileSize(fileUri)
            if (declaredSize != null && declaredSize > MAX_ATTACHMENT_BYTES) {
                return Result.Error("Attachment is too large. Max size is 25 MB.")
            }
            val bytes = contentResolver.openInputStream(fileUri)?.readBytes()
                ?: return Result.Error("Could not read file")
            if (bytes.size > MAX_ATTACHMENT_BYTES) {
                return Result.Error("Attachment is too large. Max size is 25 MB.")
            }

            val jsonPayload = buildString {
                append("{\"content\":\"")
                append(content.replace("\"", "\\\""))
                append("\"")
                if (!replyToId.isNullOrBlank()) {
                    append(",\"reply_to_id\":\"$replyToId\"")
                }
                append("}")
            }

            val jsonBody = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "files[0]", fileName,
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )

            val response = uploadApiService.sendMessageWithAttachment(channelId, jsonBody, filePart)
            if (response.isSuccessful) {
                val message = response.body()
                message?.let {
                    addMessageToCache(channelId, it)
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Upload failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Upload error: ${e.message}")
        }
    }

    fun getAttachmentMetadata(fileUri: Uri): AttachmentMetadata {
        val contentResolver = context.contentResolver
        return AttachmentMetadata(
            displayName = getDisplayName(fileUri) ?: fileUri.lastPathSegment ?: "attachment",
            mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream",
            sizeBytes = getFileSize(fileUri)
        )
    }

    private fun getDisplayName(fileUri: Uri): String? {
        return context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }

    private fun getFileSize(fileUri: Uri): Long? {
        return context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
    }

    // ==================== PINS ====================

    suspend fun getPinnedMessages(channelId: String): Result<List<Message>> = try {
        val r = pinApiService.getPinnedMessages(channelId)
        if (r.isSuccessful) Result.Success(r.body() ?: emptyList())
        else Result.Error("Failed: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun pinMessage(channelId: String, messageId: String): Result<Unit> = try {
        val r = pinApiService.pinMessage(channelId, messageId)
        if (r.isSuccessful) Result.Success(Unit) else Result.Error("Failed: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun unpinMessage(channelId: String, messageId: String): Result<Unit> = try {
        val r = pinApiService.unpinMessage(channelId, messageId)
        if (r.isSuccessful) Result.Success(Unit) else Result.Error("Failed: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    // ==================== GUILD MEMBERS ====================

    suspend fun getGuildMembers(guildId: String): Result<List<GuildMember>> = try {
        val r = guildMembersApiService.getGuildMembers(guildId)
        if (r.isSuccessful) Result.Success(r.body() ?: emptyList())
        else Result.Error("Failed: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    // ==================== INVITES ====================

    suspend fun previewInvite(code: String): Result<InviteInfo> = try {
        val r = inviteApiService.getInvite(code)
        if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
        else Result.Error("Invalid invite: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun joinViaInvite(code: String): Result<InviteInfo> = try {
        val r = inviteApiService.joinViaInvite(code)
        if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
        else Result.Error("Could not join: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    // ==================== AVATAR UPLOAD ====================

    suspend fun updateAvatar(fileUri: Uri): Result<com.fluxer.client.data.model.User> {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(fileUri) ?: "image/jpeg"
        val bytes = contentResolver.openInputStream(fileUri)?.readBytes()
            ?: return Result.Error("Could not read file")
        return try {
            val part = MultipartBody.Part.createFormData(
                "avatar", "avatar.jpg",
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            val r = avatarApiService.updateAvatar(part)
            if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
            else Result.Error("Upload failed: ${r.code()}")
        } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
    }

    // ==================== CUSTOM STATUS ====================

    suspend fun setCustomStatus(status: String?): Result<com.fluxer.client.data.model.UserProfile> = try {
        val r = apiService.updateProfile(UpdateProfileRequest(customStatus = status))
        if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
        else Result.Error("Failed: ${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    private fun collectGatewayEvents() {
        gatewayManager.events.onEach { event ->
            when (event) {
                is GatewayWebSocketManager.GatewayEvent.Ready -> {
                    cacheChannelsFromReady(event.data)
                    cacheDmChannels(event.data.privateChannels)
                }
                is GatewayWebSocketManager.GatewayEvent.MessageCreate -> {
                    addMessageToCache(event.message.channelId, event.message)
                }
                is GatewayWebSocketManager.GatewayEvent.MessageUpdate -> {
                    updateMessageInCache(event.message.channelId, event.message)
                }
                is GatewayWebSocketManager.GatewayEvent.MessageDelete -> {
                    removeMessageFromCache(event.channelId, event.messageId)
                }
                is GatewayWebSocketManager.GatewayEvent.ReactionAdd -> {
                    addReactionToCache(event.data)
                }
                is GatewayWebSocketManager.GatewayEvent.ReactionRemove -> {
                    removeReactionFromCache(event.data)
                }
                is GatewayWebSocketManager.GatewayEvent.ChannelCreate -> {
                    if (event.channel.serverId == null) {
                        cacheDmChannels(listOf(event.channel), replace = false)
                    } else {
                        addChannelToCache(event.channel)
                    }
                }
                is GatewayWebSocketManager.GatewayEvent.ChannelUpdate -> {
                    if (event.channel.serverId == null) {
                        cacheDmChannels(listOf(event.channel), replace = false)
                    } else {
                        updateChannelInCache(event.channel)
                    }
                }
                is GatewayWebSocketManager.GatewayEvent.ChannelDelete -> {
                    if (event.channel.serverId == null) {
                        removeDmChannelFromCache(event.channel.id)
                    } else {
                        removeChannelFromCache(event.channel)
                    }
                }
                is GatewayWebSocketManager.GatewayEvent.GuildCreate -> {
                    if (event.guild.channels.isNotEmpty()) {
                        channelCache[event.guild.id] = event.guild.channels
                        _channelCacheFlow.value = channelCache.toMap()
                    }
                }
                is GatewayWebSocketManager.GatewayEvent.GuildUpdate -> {
                    // Channels may not be present in guild update, only update if they are
                    if (event.guild.channels.isNotEmpty()) {
                        channelCache[event.guild.id] = event.guild.channels
                        _channelCacheFlow.value = channelCache.toMap()
                    }
                }
                is GatewayWebSocketManager.GatewayEvent.GuildDelete -> {
                    channelCache.remove(event.guildId)
                    _channelCacheFlow.value = channelCache.toMap()
                }
                is GatewayWebSocketManager.GatewayEvent.VoiceStateUpdate -> {
                    updateVoiceStateCache(event.data)
                }
                is GatewayWebSocketManager.GatewayEvent.VoiceServerUpdate -> {
                    _voiceServerUpdates.tryEmit(event.data)
                }
                is GatewayWebSocketManager.GatewayEvent.CallCreate -> {
                    updateCallCache(event.data)
                    _callUpdates.tryEmit(event.data)
                }
                is GatewayWebSocketManager.GatewayEvent.CallUpdate -> {
                    updateCallCache(event.data)
                    _callUpdates.tryEmit(event.data)
                }
                is GatewayWebSocketManager.GatewayEvent.CallDelete -> {
                    callStateCache.remove(event.data.channelId)
                    _callDeletes.tryEmit(event.data.channelId)
                }
                else -> { /* Handle other events */ }
            }
        }.launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
    }

    private fun cacheChannelsFromReady(ready: ReadyEvent) {
        ready.guilds.forEach { guild ->
            if (guild.channels.isNotEmpty()) {
                channelCache[guild.id] = guild.channels
            }
        }
        _channelCacheFlow.value = channelCache.toMap()
        Timber.i("Cached channels for ${channelCache.size} guilds from READY")
    }

    private fun restoreDmChannels() {
        scope.launch {
            val cached = dmChannelDao.getAll().map { it.toDomain() }
            if (cached.isNotEmpty()) {
                _dmChannelsFlow.value = cached
            }
        }
    }

    private fun cacheDmChannels(channels: List<Channel>, replace: Boolean = true) {
        if (channels.isEmpty()) {
            if (replace) {
                _dmChannelsFlow.value = emptyList()
            }
            return
        }
        val dmCandidates = channels.filter {
            it.type == ChannelType.DM && it.serverId == null && it.recipients.isNotEmpty()
        }
        if (dmCandidates.isEmpty()) return

        val merged = if (replace) {
            dmCandidates
        } else {
            (_dmChannelsFlow.value + dmCandidates).associateBy { it.id }.values.toList()
        }.sortedByDescending { it.lastMessageId ?: it.id }

        _dmChannelsFlow.value = merged
        scope.launch {
            dmChannelDao.upsertAll(merged.map { it.toEntity() })
        }
    }

    private fun removeDmChannelFromCache(channelId: String) {
        _dmChannelsFlow.value = _dmChannelsFlow.value.filter { it.id != channelId }
    }

    private fun addChannelToCache(channel: Channel) {
        val guildId = channel.serverId ?: return
        val current = channelCache[guildId].orEmpty()
        if (current.none { it.id == channel.id }) {
            channelCache[guildId] = current + channel
            _channelCacheFlow.value = channelCache.toMap()
            Timber.d("Added channel ${channel.name} to cache for guild $guildId")
        }
    }

    private fun updateChannelInCache(channel: Channel) {
        val guildId = channel.serverId ?: return
        val current = channelCache[guildId].orEmpty()
        channelCache[guildId] = current.map { if (it.id == channel.id) channel else it }
        _channelCacheFlow.value = channelCache.toMap()
        Timber.d("Updated channel ${channel.name} in cache for guild $guildId")
    }

    private fun removeChannelFromCache(channel: Channel) {
        val guildId = channel.serverId ?: return
        val current = channelCache[guildId].orEmpty()
        channelCache[guildId] = current.filter { it.id != channel.id }
        _channelCacheFlow.value = channelCache.toMap()
        Timber.d("Removed channel ${channel.name} from cache for guild $guildId")
    }

    private fun updateMessageCache(channelId: String, messages: List<Message>, replace: Boolean) {
        val flow = messageCache.getOrPut(channelId) { MutableStateFlow(emptyList()) }
        val current = if (replace) emptyList() else flow.value
        
        // Merge and sort by timestamp
        val merged = (current + messages).distinctBy { it.id }
            .sortedBy { it.createdAt }
        
        flow.value = merged
    }

    private fun addMessageToCache(channelId: String, message: Message) {
        val flow = messageCache.getOrPut(channelId) { MutableStateFlow(emptyList()) }
        val current = flow.value
        
        // Check if already exists (optimistic update)
        if (current.none { it.id == message.id }) {
            flow.value = (current + message).sortedBy { it.createdAt }
        } else {
            // Update existing
            flow.value = current.map { if (it.id == message.id) message else it }
        }
    }

    private fun updateMessageInCache(channelId: String, message: Message) {
        val flow = messageCache[channelId] ?: return
        flow.value = flow.value.map { 
            if (it.id == message.id) message else it 
        }
    }

    private fun removeMessageFromCache(channelId: String, messageId: String) {
        val flow = messageCache[channelId] ?: return
        flow.value = flow.value.filter { it.id != messageId }
    }
    
    private fun addReactionToCache(event: ReactionEvent) {
        val flow = messageCache[event.channelId] ?: return
        flow.value = flow.value.map { message ->
            if (message.id == event.messageId) {
                // Check if this reaction already exists
                val existingReaction = message.reactions.find { it.emoji.name == event.emoji.name }
                val updatedReactions = if (existingReaction != null) {
                    // Increment count
                    message.reactions.map { reaction ->
                        if (reaction.emoji.name == event.emoji.name) {
                            reaction.copy(count = reaction.count + 1, userReacted = true)
                        } else reaction
                    }
                } else {
                    // Add new reaction
                    message.reactions + Reaction(
                        emoji = event.emoji,
                        count = 1,
                        userReacted = true
                    )
                }
                message.copy(reactions = updatedReactions)
            } else message
        }
    }
    
    private fun removeReactionFromCache(event: ReactionEvent) {
        val flow = messageCache[event.channelId] ?: return
        flow.value = flow.value.map { message ->
            if (message.id == event.messageId) {
                val updatedReactions = message.reactions.mapNotNull { reaction ->
                    if (reaction.emoji.name == event.emoji.name) {
                        if (reaction.count > 1) {
                            reaction.copy(count = reaction.count - 1, userReacted = false)
                        } else null // Remove if count would be 0
                    } else reaction
                }
                message.copy(reactions = updatedReactions)
            } else message
        }
    }

    // ==================== VOICE STATE CACHE ====================

    fun getVoiceStatesFlow(channelId: String): StateFlow<List<com.fluxer.client.data.model.VoiceStateUpdateEvent>> {
        return voiceStateCache.getOrPut(channelId) {
            MutableStateFlow(emptyList())
        }
    }

    private fun updateVoiceStateCache(voiceState: com.fluxer.client.data.model.VoiceStateUpdateEvent) {
        val channelId = voiceState.channelId
        
        if (channelId == null) {
            // User disconnected - search all cached channels and remove them
            voiceStateCache.forEach { (_, flow) ->
                flow.value = flow.value.filter { it.userId != voiceState.userId }
            }
            return
        }
        
        val flow = voiceStateCache.getOrPut(channelId) { MutableStateFlow(emptyList()) }
        val current = flow.value

        // Update or add voice state
        val existing = current.find { it.userId == voiceState.userId }
        flow.value = if (existing != null) {
            current.map { if (it.userId == voiceState.userId) voiceState else it }
        } else {
            current + voiceState
        }
    }

    // ==================== CALL CACHE ====================

    fun getCallStateFlow(channelId: String): StateFlow<com.fluxer.client.data.model.CallEvent?> {
        return callStateCache.getOrPut(channelId) {
            MutableStateFlow(null)
        }
    }

    private fun updateCallCache(call: com.fluxer.client.data.model.CallEvent) {
        val flow = callStateCache.getOrPut(call.channelId) { MutableStateFlow(null) }
        flow.value = call
    }

    private fun Channel.toEntity(): DmChannelEntity =
        DmChannelEntity(
            id = id,
            name = displayName(),
            lastMessageId = lastMessageId,
            type = type.value,
            serverId = serverId,
            recipientId = recipients.firstOrNull()?.id,
            recipientUsername = recipients.firstOrNull()?.username,
            recipientDisplayName = recipients.firstOrNull()?.displayName,
            recipientAvatarUrl = recipients.firstOrNull()?.avatarUrl,
            updatedAt = System.currentTimeMillis()
        )

    private fun DmChannelEntity.toDomain(): Channel {
        val recipient = if (recipientId != null && recipientUsername != null) {
            User(
                id = recipientId,
                username = recipientUsername,
                displayName = recipientDisplayName,
                avatarUrl = recipientAvatarUrl
            )
        } else {
            null
        }
        return Channel(
            id = id,
            name = name,
            type = ChannelType.entries.firstOrNull { it.value == type } ?: ChannelType.UNKNOWN,
            serverId = serverId,
            lastMessageId = lastMessageId,
            recipients = listOfNotNull(recipient)
        )
    }
}
