package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Call(
    val id: String,
    @SerialName("channel_id")
    val channelId: String? = null,
    @SerialName("guild_id")
    val guildId: String? = null,
    @SerialName("initiator_id")
    val initiatorId: String,
    val participants: List<CallParticipant> = emptyList(),
    val status: CallStatus = CallStatus.RINGING,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("ended_at")
    val endedAt: String? = null,
    val type: CallType = CallType.VOICE
)

@Serializable
data class CallParticipant(
    val user: User,
    @SerialName("joined_at")
    val joinedAt: String? = null,
    @SerialName("left_at")
    val leftAt: String? = null,
    val status: ParticipantStatus = ParticipantStatus.PENDING
)

@Serializable
enum class CallStatus {
    RINGING, CONNECTED, ENDED, MISSED, DECLINED
}

@Serializable
enum class CallType {
    VOICE, VIDEO
}

@Serializable
enum class ParticipantStatus {
    PENDING, CONNECTED, DISCONNECTED
}

@Serializable
data class InitiateCallRequest(
    @SerialName("recipient_id")
    val recipientId: String? = null,
    @SerialName("channel_id")
    val channelId: String? = null,
    val type: CallType = CallType.VOICE
)

@Serializable
data class JoinCallRequest(
    @SerialName("call_id")
    val callId: String,
    @SerialName("token")
    val token: String
)

@Serializable
data class CallResponse(
    val call: Call,
    val token: String? = null,
    val servers: List<IceServer> = emptyList()
)

@Serializable
data class IceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

@Serializable
data class IncomingCallData(
    @SerialName("call_id")
    val callId: String,
    val caller: User,
    val type: CallType = CallType.VOICE,
    @SerialName("channel_name")
    val channelName: String? = null
)
