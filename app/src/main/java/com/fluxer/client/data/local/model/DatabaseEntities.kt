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
