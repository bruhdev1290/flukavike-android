package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * WebSocket Gateway Events - Fluxer Real-time Protocol
 */

@Serializable
data class GatewayPayload(
    val op: Int,                    // Opcode
    @SerialName("d")
    val data: JsonElement? = null,  // Event data
    @SerialName("s")
    val sequence: Int? = null,      // Sequence number
    @SerialName("t")
    val type: String? = null        // Event type
)

@Serializable
data class GatewayHello(
    @SerialName("heartbeat_interval")
    val heartbeatInterval: Long
)

@Serializable
data class GatewayIdentify(
    val token: String,
    val properties: ConnectionProperties
)

@Serializable
data class ConnectionProperties(
    @SerialName("os")
    val os: String = "android",
    @SerialName("browser")
    val browser: String = "FluxerAndroid",
    @SerialName("device")
    val device: String = "Android Device"
)

@Serializable
data class GatewayResume(
    val token: String,
    @SerialName("session_id")
    val sessionId: String,
    val seq: Int
)

// Gateway Opcodes
object GatewayOpCodes {
    const val DISPATCH = 0              // Receive
    const val HEARTBEAT = 1             // Send/Receive
    const val IDENTIFY = 2              // Send
    const val PRESENCE_UPDATE = 3       // Send
    const val VOICE_STATE_UPDATE = 4    // Send
    const val RESUME = 6                // Send
    const val RECONNECT = 7             // Receive
    const val REQUEST_GUILD_MEMBERS = 8 // Send
    const val INVALID_SESSION = 9       // Receive
    const val HELLO = 10                // Receive
    const val HEARTBEAT_ACK = 11        // Receive
}

// Gateway Event Types
object GatewayEventTypes {
    const val READY = "READY"
    const val RESUMED = "RESUMED"
    const val MESSAGE_CREATE = "MESSAGE_CREATE"
    const val MESSAGE_UPDATE = "MESSAGE_UPDATE"
    const val MESSAGE_DELETE = "MESSAGE_DELETE"
    const val CHANNEL_CREATE = "CHANNEL_CREATE"
    const val CHANNEL_UPDATE = "CHANNEL_UPDATE"
    const val CHANNEL_DELETE = "CHANNEL_DELETE"
    const val GUILD_CREATE = "GUILD_CREATE"
    const val GUILD_UPDATE = "GUILD_UPDATE"
    const val GUILD_DELETE = "GUILD_DELETE"
    const val PRESENCE_UPDATE = "PRESENCE_UPDATE"
    const val TYPING_START = "TYPING_START"
    const val USER_UPDATE = "USER_UPDATE"
}

@Serializable
data class ReadyEvent(
    @SerialName("v")
    val version: Int,
    val user: User,
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("resume_gateway_url")
    val resumeGatewayUrl: String? = null,
    val guilds: List<Server> = emptyList(),
    @SerialName("private_channels")
    val privateChannels: List<Channel> = emptyList()
)

@Serializable
data class TypingEvent(
    @SerialName("channel_id")
    val channelId: String,
    @SerialName("user_id")
    val userId: String,
    val timestamp: Long
)

@Serializable
data class PresenceUpdateEvent(
    val user: User,
    val status: UserStatus,
    @SerialName("guild_id")
    val guildId: String? = null
)
