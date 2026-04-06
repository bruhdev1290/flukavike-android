package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: NotificationData? = null,
    @SerialName("timestamp")
    val timestamp: String,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

@Serializable
data class NotificationData(
    @SerialName("channel_id")
    val channelId: String? = null,
    @SerialName("guild_id")
    val guildId: String? = null,
    @SerialName("message_id")
    val messageId: String? = null,
    @SerialName("sender_id")
    val senderId: String? = null,
    @SerialName("call_id")
    val callId: String? = null,
    val url: String? = null
)

@Serializable
enum class NotificationType {
    MESSAGE,
    MENTION,
    DIRECT_MESSAGE,
    CALL,
    CALL_MISSED,
    FRIEND_REQUEST,
    GUILD_INVITE,
    SYSTEM
}

@Serializable
enum class NotificationPriority {
    HIGH, NORMAL, LOW
}

@Serializable
data class FcmTokenRequest(
    @SerialName("fcm_token")
    val fcmToken: String,
    @SerialName("device_type")
    val deviceType: String = "android",
    @SerialName("device_name")
    val deviceName: String? = null
)

@Serializable
data class NotificationSettings(
    @SerialName("global_enabled")
    val globalEnabled: Boolean = true,
    @SerialName("show_preview")
    val showPreview: Boolean = true,
    @SerialName("sound_enabled")
    val soundEnabled: Boolean = true,
    @SerialName("vibration_enabled")
    val vibrationEnabled: Boolean = true,
    @SerialName("mention_notifications")
    val mentionNotifications: Boolean = true,
    @SerialName("dm_notifications")
    val dmNotifications: Boolean = true,
    @SerialName("call_notifications")
    val callNotifications: Boolean = true,
    @SerialName("friend_request_notifications")
    val friendRequestNotifications: Boolean = true,
    @SerialName("guild_notifications")
    val guildNotifications: Map<String, GuildNotificationSettings> = emptyMap()
)

@Serializable
data class GuildNotificationSettings(
    @SerialName("guild_id")
    val guildId: String,
    val enabled: Boolean = true,
    @SerialName("suppress_everyone")
    val suppressEveryone: Boolean = false,
    @SerialName("suppress_roles")
    val suppressRoles: Boolean = false,
    @SerialName("channel_overrides")
    val channelOverrides: Map<String, ChannelNotificationSettings> = emptyMap()
)

@Serializable
data class ChannelNotificationSettings(
    @SerialName("channel_id")
    val channelId: String,
    val enabled: Boolean = true,
    val muted: Boolean = false,
    @SerialName("mute_until")
    val muteUntil: String? = null
)

@Serializable
data class NotificationChannelInfo(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int
)
