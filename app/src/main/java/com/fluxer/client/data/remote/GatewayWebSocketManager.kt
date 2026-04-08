package com.fluxer.client.data.remote

import com.fluxer.client.data.local.AuthTokenStorage
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.local.SecureCookieStorage
import com.fluxer.client.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.*
import okio.IOException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages WebSocket connection to Fluxer Gateway for real-time events.
 * Handles auto-reconnection, heartbeat, and session resumption.
 */
@Singleton
class GatewayWebSocketManager @Inject constructor(
    private val cookieStorage: SecureCookieStorage,
    private val authTokenStorage: AuthTokenStorage,
    private val json: Json,
    private val instanceConfigStore: InstanceConfigStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Gateway events flow
    private val _events = Channel<GatewayEvent>(Channel.BUFFERED)
    val events: Flow<GatewayEvent> = _events.receiveAsFlow()
    
    // Session data for resumption
    private var sessionId: String? = null
    private var sequenceNumber: Int? = null
    private var heartbeatInterval: Long = 45000 // Default fallback
    
    private var reconnectAttempt = 0
    private val maxReconnectDelay = 300000L // 5 minutes

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Timber.i("🟢 WebSocket connected")
            _connectionState.value = ConnectionState.Connected
            reconnectAttempt = 0
        }

        override fun onMessage(ws: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Timber.w("🟡 WebSocket closing: $code - $reason")
            _connectionState.value = ConnectionState.Disconnecting
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Timber.w("🔴 WebSocket closed: $code - $reason")
            cleanupConnection()
            
            // Auto-reconnect on unexpected close
            if (code != 1000 && code != 1001) {
                scheduleReconnect()
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Timber.e(t, "🔴 WebSocket failure")
            _connectionState.value = ConnectionState.Error(t.message ?: "Unknown error")
            cleanupConnection()
            scheduleReconnect()
        }
    }

    /**
     * Connect to the Gateway
     */
    fun connect(gatewayUrl: String = instanceConfigStore.getActiveWebSocketUrl()) {
        if (_connectionState.value is ConnectionState.Connecting ||
            _connectionState.value is ConnectionState.Connected) {
            Timber.d("Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .cookieJar(cookieStorage as CookieJar)
            .build()

        val wsUrl = if (gatewayUrl.contains("?")) gatewayUrl else "$gatewayUrl?v=1"

        val request = Request.Builder()
            .url(wsUrl)
            .header("Accept", "application/json")
            .header("User-Agent", "FluxerAndroid/1.0")
            .build()

        Timber.i("🔌 Connecting to Gateway: $gatewayUrl")
        webSocket = client.newWebSocket(request, webSocketListener)
    }

    /**
     * Disconnect from Gateway
     */
    fun disconnect() {
        Timber.i("👋 Disconnecting from Gateway")
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        cleanupConnection()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Send a message through the WebSocket
     */
    fun send(payload: GatewayPayload): Boolean {
        val jsonStr = json.encodeToString(payload)
        Timber.v("⬆️ Sending: $jsonStr")
        return webSocket?.send(jsonStr) ?: false
    }

    /**
     * Update presence status
     */
    fun updatePresence(status: UserStatus, customStatus: String? = null) {
        val payload = GatewayPayload(
            op = GatewayOpCodes.PRESENCE_UPDATE,
            data = json.encodeToJsonElement(
                mapOf(
                    "status" to status.name.lowercase(),
                    "custom_status" to customStatus
                )
            )
        )
        send(payload)
    }

    private fun handleMessage(text: String) {
        try {
            Timber.v("⬇️ Received: $text")
            val payload = json.decodeFromString<GatewayPayload>(text)
            
            // Update sequence number
            payload.sequence?.let { sequenceNumber = it }
            
            when (payload.op) {
                GatewayOpCodes.DISPATCH -> handleDispatch(payload)
                GatewayOpCodes.HELLO -> handleHello(payload)
                GatewayOpCodes.HEARTBEAT_ACK -> Timber.v("💓 Heartbeat ACK")
                GatewayOpCodes.HEARTBEAT -> sendHeartbeat()
                GatewayOpCodes.RECONNECT -> handleReconnect()
                GatewayOpCodes.INVALID_SESSION -> handleInvalidSession()
                else -> Timber.d("Unhandled opcode: ${payload.op}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle WebSocket message")
        }
    }

    private fun handleHello(payload: GatewayPayload) {
        val hello = payload.data?.let { json.decodeFromJsonElement<GatewayHello>(it) }
        hello?.let {
            heartbeatInterval = it.heartbeatInterval
            Timber.i("👋 Gateway Hello received, heartbeat: ${heartbeatInterval}ms")
            startHeartbeat()
        }
        
        // Send IDENTIFY or RESUME
        if (sessionId != null && sequenceNumber != null) {
            sendResume()
        } else {
            sendIdentify()
        }
    }

    private fun handleDispatch(payload: GatewayPayload) {
        val eventType = payload.type ?: return
        val eventData = payload.data ?: return
        
        when (eventType) {
            GatewayEventTypes.READY -> {
                val ready = json.decodeFromJsonElement<ReadyEvent>(eventData)
                sessionId = ready.sessionId
                Timber.i("✅ Gateway Ready, session: ${sessionId?.take(8)}...")
                _events.trySend(GatewayEvent.Ready(ready))
            }
            GatewayEventTypes.RESUMED -> {
                Timber.i("✅ Gateway Resumed")
                _events.trySend(GatewayEvent.Resumed)
            }
            GatewayEventTypes.MESSAGE_CREATE -> {
                val message = json.decodeFromJsonElement<Message>(eventData)
                _events.trySend(GatewayEvent.MessageCreate(message))
            }
            GatewayEventTypes.MESSAGE_UPDATE -> {
                val message = json.decodeFromJsonElement<Message>(eventData)
                _events.trySend(GatewayEvent.MessageUpdate(message))
            }
            GatewayEventTypes.MESSAGE_DELETE -> {
                val data = json.decodeFromJsonElement<MessageDeleteData>(eventData)
                _events.trySend(GatewayEvent.MessageDelete(data.id, data.channelId))
            }
            GatewayEventTypes.PRESENCE_UPDATE -> {
                val update = json.decodeFromJsonElement<PresenceUpdateEvent>(eventData)
                _events.trySend(GatewayEvent.PresenceUpdate(update))
            }
            GatewayEventTypes.TYPING_START -> {
                val typing = json.decodeFromJsonElement<TypingEvent>(eventData)
                _events.trySend(GatewayEvent.TypingStart(typing))
            }
            GatewayEventTypes.MESSAGE_REACTION_ADD -> {
                val reaction = json.decodeFromJsonElement<ReactionEvent>(eventData)
                _events.trySend(GatewayEvent.ReactionAdd(reaction))
            }
            GatewayEventTypes.MESSAGE_REACTION_REMOVE -> {
                val reaction = json.decodeFromJsonElement<ReactionEvent>(eventData)
                _events.trySend(GatewayEvent.ReactionRemove(reaction))
            }
            else -> {
                Timber.d("Unhandled event type: $eventType")
            }
        }
    }

    private fun handleReconnect() {
        Timber.w("🔄 Gateway requested reconnect")
        webSocket?.close(1001, "Server requested reconnect")
        scheduleReconnect()
    }

    private fun handleInvalidSession() {
        Timber.e("🚫 Invalid session, clearing session data")
        sessionId = null
        sequenceNumber = null
        // Will trigger fresh IDENTIFY on reconnect
    }

    private fun sendIdentify() {
        // Try auth token first (from login), then fall back to session cookie
        val token = authTokenStorage.token ?: cookieStorage.getSessionToken()
        if (token == null) {
            Timber.e("No auth token or session token for IDENTIFY - will retry on reconnect")
            return
        }
        
        val identify = GatewayIdentify(
            token = token,
            properties = ConnectionProperties()
        )
        
        val payload = GatewayPayload(
            op = GatewayOpCodes.IDENTIFY,
            data = json.encodeToJsonElement(identify)
        )
        
        send(payload)
        Timber.d("📤 Sent IDENTIFY")
    }

    private fun sendResume() {
        val token = cookieStorage.getSessionToken() ?: return
        val session = sessionId ?: return
        val seq = sequenceNumber ?: return
        
        val resume = GatewayResume(token, session, seq)
        val payload = GatewayPayload(
            op = GatewayOpCodes.RESUME,
            data = json.encodeToJsonElement(resume)
        )
        
        send(payload)
        Timber.d("📤 Sent RESUME for session ${session.take(8)}...")
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(heartbeatInterval)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val payload = GatewayPayload(
            op = GatewayOpCodes.HEARTBEAT,
            data = json.encodeToJsonElement(sequenceNumber)
        )
        send(payload)
        Timber.v("💓 Heartbeat sent, seq: $sequenceNumber")
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        
        reconnectJob = scope.launch {
            val delayMs = calculateReconnectDelay()
            Timber.i("⏳ Scheduling reconnect in ${delayMs}ms (attempt $reconnectAttempt)")
            delay(delayMs)
            connect()
        }
    }

    private fun calculateReconnectDelay(): Long {
        reconnectAttempt++
        // Exponential backoff with jitter: min(2^attempt * 1000 + random, max)
        val baseDelay = kotlin.math.min(
            (1 shl kotlin.math.min(reconnectAttempt, 10)) * 1000L,
            maxReconnectDelay
        )
        val jitter = (Math.random() * 1000).toLong()
        return baseDelay + jitter
    }

    private fun cleanupConnection() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket = null
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Disconnecting : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    sealed class GatewayEvent {
        data class Ready(val data: ReadyEvent) : GatewayEvent()
        object Resumed : GatewayEvent()
        data class MessageCreate(val message: Message) : GatewayEvent()
        data class MessageUpdate(val message: Message) : GatewayEvent()
        data class MessageDelete(val messageId: String, val channelId: String) : GatewayEvent()
        data class PresenceUpdate(val data: PresenceUpdateEvent) : GatewayEvent()
        data class TypingStart(val data: TypingEvent) : GatewayEvent()
        data class ReactionAdd(val data: ReactionEvent) : GatewayEvent()
        data class ReactionRemove(val data: ReactionEvent) : GatewayEvent()
    }

    @kotlinx.serialization.Serializable
    private data class MessageDeleteData(
        val id: String,
        @SerialName("channel_id")
        val channelId: String
    )

}
