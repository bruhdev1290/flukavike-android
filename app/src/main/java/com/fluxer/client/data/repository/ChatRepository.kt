package com.fluxer.client.data.repository

import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.*
import com.fluxer.client.util.Result
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
     * Get messages for a channel
     */
    suspend fun getMessages(
        channelId: String,
        limit: Int = 50,
        before: String? = null
    ): Result<List<Message>> {
        return try {
            val response = apiService.getMessages(channelId, limit, before)
            
            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                
                // Update cache
                updateMessageCache(channelId, messages, before == null)
                
                Result.Success(messages)
            } else {
                Result.Error("Failed to load messages: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
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
