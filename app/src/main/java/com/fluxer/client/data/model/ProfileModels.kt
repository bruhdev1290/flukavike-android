package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val discriminator: String = "0001",
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("banner_url")
    val bannerUrl: String? = null,
    val bio: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    @SerialName("custom_status")
    val customStatus: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val stats: UserStats? = null,
    @SerialName("is_premium")
    val isPremium: Boolean = false,
    val badges: List<UserBadge> = emptyList()
)

@Serializable
data class UserStats(
    @SerialName("message_count")
    val messageCount: Long = 0,
    @SerialName("joined_guilds")
    val joinedGuilds: Int = 0,
    @SerialName("friends_count")
    val friendsCount: Int = 0,
    @SerialName("voice_time_minutes")
    val voiceTimeMinutes: Long = 0
)

@Serializable
data class UserBadge(
    val id: String,
    val name: String,
    val icon: String,
    val description: String
)

@Serializable
data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    @SerialName("message_display")
    val messageDisplay: MessageDisplayMode = MessageDisplayMode.COMFORTABLE,
    @SerialName("font_size")
    val fontSize: FontSize = FontSize.MEDIUM,
    @SerialName("compact_mode")
    val compactMode: Boolean = false,
    @SerialName("show_animations")
    val showAnimations: Boolean = true,
    @SerialName("reduced_motion")
    val reducedMotion: Boolean = false,
    @SerialName("sound_enabled")
    val soundEnabled: Boolean = true,
    @SerialName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
    @SerialName("mention_notifications")
    val mentionNotifications: Boolean = true,
    @SerialName("dm_notifications")
    val dmNotifications: Boolean = true,
    @SerialName("call_notifications")
    val callNotifications: Boolean = true
)

@Serializable
enum class ThemeMode {
    LIGHT, DARK, SYSTEM, AMOLED
}

@Serializable
enum class MessageDisplayMode {
    COMFORTABLE, COMPACT
}

@Serializable
enum class FontSize {
    SMALL, MEDIUM, LARGE
}

@Serializable
data class UpdateProfileRequest(
    val bio: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("custom_status")
    val customStatus: String? = null
)

@Serializable
data class UpdateSettingsRequest(
    val theme: ThemeMode? = null,
    @SerialName("message_display")
    val messageDisplay: MessageDisplayMode? = null,
    @SerialName("font_size")
    val fontSize: FontSize? = null,
    @SerialName("compact_mode")
    val compactMode: Boolean? = null,
    @SerialName("show_animations")
    val showAnimations: Boolean? = null,
    @SerialName("notifications_enabled")
    val notificationsEnabled: Boolean? = null
)
