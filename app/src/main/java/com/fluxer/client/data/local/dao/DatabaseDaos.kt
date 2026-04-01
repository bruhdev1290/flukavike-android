package com.fluxer.client.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fluxer.client.data.local.model.ChannelEntity
import com.fluxer.client.data.local.model.GuildEntity
import com.fluxer.client.data.local.model.MessageEntity
import com.fluxer.client.data.local.model.PendingMessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp DESC")
    suspend fun getMessagesForChannel(channelId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun deleteMessagesForChannel(channelId: String)
}

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("SELECT * FROM channels WHERE guildId = :guildId")
    suspend fun getChannelsForGuild(guildId: String): List<ChannelEntity>
}

@Dao
interface GuildDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(guilds: List<GuildEntity>)

    @Query("SELECT * FROM guilds")
    suspend fun getAllGuilds(): List<GuildEntity>
}

@Dao
interface PendingMessageDao {
    @Insert
    suspend fun insert(pendingMessage: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun delete(id: Long)
}
