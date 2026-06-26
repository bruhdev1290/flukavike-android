package com.fluxer.client.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded

// Local entity for embedded author to avoid cross-package issues with Room KSP
@Entity
class AuthorEntity(
    val author_id: String,
    val author_username: String,
    val author_displayName: String?,
    val author_avatarUrl: String?
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    @Embedded val author: AuthorEntity,
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val guildId: String,
    val name: String,
    val type: String
)

@Entity(tableName = "guilds")
data class GuildEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconUrl: String?
)

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "auth_sessions")
data class AuthSessionEntity(
    @PrimaryKey val userId: String,
    val active: Boolean,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val instanceSnapshotJson: String?,
    val updatedAt: Long
)

@Entity(tableName = "dm_channels")
data class DmChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastMessageId: String?,
    val type: Int,
    val serverId: String?,
    val recipientId: String?,
    val recipientUsername: String?,
    val recipientDisplayName: String?,
    val recipientAvatarUrl: String?,
    val updatedAt: Long
)

@Entity(tableName = "guild_last_channels")
data class GuildLastChannelEntity(
    @PrimaryKey val guildId: String,
    val channelId: String,
    val updatedAt: Long
)

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val channelId: String,
    val guildId: String?,
    val position: Int,
    val createdAt: Long
)

@Entity(tableName = "read_states")
data class ReadStateEntity(
    @PrimaryKey val channelId: String,
    val lastReadMessageId: String?,
    val mentionCount: Int,
    val updatedAt: Long
)

@Entity(tableName = "notification_feed")
data class NotificationFeedEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val body: String,
    val guildId: String?,
    val channelId: String?,
    val messageId: String?,
    val createdAt: Long,
    val read: Boolean
)

data class ChannelUnreadCount(
    val channelId: String,
    val unreadCount: Int
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val updatedAt: Long
)

@Entity(tableName = "members", primaryKeys = ["guildId", "userId"])
data class MemberEntity(
    val guildId: String,
    val userId: String,
    val nickname: String?,
    val roleIds: String,
    val updatedAt: Long
)

@Entity(tableName = "roles", primaryKeys = ["guildId", "roleId"])
data class RoleEntity(
    val guildId: String,
    val roleId: String,
    val name: String,
    val color: Int?,
    val position: Int
)

@Entity(tableName = "voice_sessions")
data class VoiceSessionEntity(
    @PrimaryKey val channelId: String,
    val guildId: String?,
    val active: Boolean,
    val muted: Boolean,
    val deafened: Boolean,
    val updatedAt: Long
)

@Entity(tableName = "navigation_state")
data class NavigationStateEntity(
    @PrimaryKey val id: String = "singleton",
    val activePath: String,
    val homePath: String,
    val notificationsPath: String,
    val youPath: String,
    val preReconnectPath: String?,
    val pendingPath: String?,
    val updatedAt: Long
)
