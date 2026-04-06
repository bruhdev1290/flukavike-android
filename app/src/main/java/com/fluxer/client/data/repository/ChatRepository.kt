package com.fluxer.client.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.fluxer.client.data.paging.MessagePagingSource
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.*
import com.fluxer.client.util.Result
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val MESSAGE_PAGE_SIZE = 50

/**
 * Repository for chat-related operations including messages and channels.
 * Bridges REST API and WebSocket Gateway.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val apiService: FluxerApiService,
    private val gatewayManager: GatewayWebSocketManager
) {
    // Cache for messages per channel
    private val messageCache = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    
    // Cache for channels per guild
    private val channelCache = mutableMapOf<String, List<Channel>>()
    
    // Expose Gateway events as repository events
    val gatewayEvents: Flow<GatewayWebSocketManager.GatewayEvent> = gatewayManager.events
    
    // Connection state
    val connectionState: Flow<GatewayWebSocketManager.ConnectionState> = 
        gatewayManager.connectionState

    init {
        // Subscribe to Gateway events to update local cache
        collectGatewayEvents()
    }

    /**
     * Get messages for a channel with pagination.
     */
    fun getMessagesPaginated(channelId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(
                pageSize = MESSAGE_PAGE_SIZE,
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
        return try {
            val response = apiService.getGuildChannels(guildId)
            
            if (response.isSuccessful) {
                val channels = response.body() ?: emptyList()
                channelCache[guildId] = channels
                Result.Success(channels)
            } else {
                Result.Error("Failed to load channels: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

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
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load DM channels: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun createDMChannel(recipientId: String): Result<Channel> {
        return try {
            val request = CreateDMRequest(recipientId = recipientId)
            val response = apiService.createDMChannel(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to create DM: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
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
            val response = apiService.addReaction(channelId, messageId, emoji)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to add reaction: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    private fun collectGatewayEvents() {
        gatewayManager.events.onEach { event ->
            when (event) {
                is GatewayWebSocketManager.GatewayEvent.MessageCreate -> {
                    addMessageToCache(event.message.channelId, event.message)
                }
                is GatewayWebSocketManager.GatewayEvent.MessageUpdate -> {
                    updateMessageInCache(event.message.channelId, event.message)
                }
                is GatewayWebSocketManager.GatewayEvent.MessageDelete -> {
                    removeMessageFromCache(event.channelId, event.messageId)
                }
                else -> { /* Handle other events */ }
            }
        }.launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
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
}
