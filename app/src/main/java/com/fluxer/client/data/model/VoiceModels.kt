package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceChannel(
    val id: String,
    val name: String,
    @SerialName("server_id")
    val serverId: String,
    val position: Int = 0,
    @SerialName("parent_id")
    val parentId: String? = null,
    @SerialName("bitrate")
    val bitrate: Int = 64000,
    @SerialName("user_limit")
    val userLimit: Int = 0, // 0 = unlimited
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class VoiceState(
    @SerialName("user_id")
    val userId: String,
    @SerialName("channel_id")
    val channelId: String,
    val sessionId: String,
    val deaf: Boolean = false,
    val mute: Boolean = false,
    @SerialName("self_deaf")
    val selfDeaf: Boolean = false,
    @SerialName("self_mute")
    val selfMute: Boolean = false,
    @SerialName("self_video")
    val selfVideo: Boolean = false,
    @SerialName("suppress")
    val suppress: Boolean = false,
    @SerialName("speaking")
    val speaking: Boolean = false,
    @SerialName("voice_activity")
    val voiceActivity: Boolean = false
)

@Serializable
data class VoiceParticipant(
    val user: User,
    @SerialName("voice_state")
    val voiceState: VoiceState,
    @SerialName("joined_at")
    val joinedAt: String,
    @SerialName("speaking_duration")
    val speakingDuration: Long = 0
)

@Serializable
data class JoinVoiceChannelRequest(
    @SerialName("channel_id")
    val channelId: String,
    @SerialName("self_mute")
    val selfMute: Boolean = false,
    @SerialName("self_deaf")
    val selfDeaf: Boolean = false
)

@Serializable
data class LeaveVoiceChannelRequest(
    @SerialName("channel_id")
    val channelId: String
)

@Serializable
data class UpdateVoiceStateRequest(
    @SerialName("self_mute")
    val selfMute: Boolean? = null,
    @SerialName("self_deaf")
    val selfDeaf: Boolean? = null,
    @SerialName("self_video")
    val selfVideo: Boolean? = null
)

@Serializable
data class VoiceTokenResponse(
    val token: String,
    @SerialName("server_id")
    val serverId: String? = null,
    val endpoint: String? = null
)

@Serializable
data class VoiceRegion(
    val id: String,
    val name: String,
    @SerialName("vip_only")
    val vipOnly: Boolean = false,
    val optimal: Boolean = false,
    val deprecated: Boolean = false,
    val custom: Boolean = false
)
