package com.fluxer.client.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import com.fluxer.client.data.model.MessageAuthor

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    @Embedded(prefix = "author_") val author: MessageAuthor,
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
