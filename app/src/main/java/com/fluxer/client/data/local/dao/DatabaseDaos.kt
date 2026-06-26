package com.fluxer.client.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fluxer.client.data.local.model.ChannelEntity
import com.fluxer.client.data.local.model.AuthSessionEntity
import com.fluxer.client.data.local.model.DmChannelEntity
import com.fluxer.client.data.local.model.FavoriteChannelEntity
import com.fluxer.client.data.local.model.GuildEntity
import com.fluxer.client.data.local.model.GuildLastChannelEntity
import com.fluxer.client.data.local.model.NavigationStateEntity
import com.fluxer.client.data.local.model.NotificationFeedEntity
import com.fluxer.client.data.local.model.ReadStateEntity
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

@Dao
interface AuthSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: AuthSessionEntity)

    @Query("UPDATE auth_sessions SET active = 0")
    suspend fun clearActive()

    @Query("SELECT * FROM auth_sessions WHERE active = 1 LIMIT 1")
    suspend fun getActiveSession(): AuthSessionEntity?

    @Query("SELECT * FROM auth_sessions WHERE userId = :userId")
    suspend fun getSession(userId: String): AuthSessionEntity?

    @Query("SELECT * FROM auth_sessions ORDER BY updatedAt DESC")
    suspend fun getAllSessions(): List<AuthSessionEntity>

    @Query("DELETE FROM auth_sessions WHERE userId = :userId")
    suspend fun delete(userId: String)
}

@Dao
interface NavigationStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: NavigationStateEntity)

    @Query("SELECT * FROM navigation_state WHERE id = 'singleton'")
    suspend fun get(): NavigationStateEntity?
}

@Dao
interface GuildLastChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lastChannel: GuildLastChannelEntity)

    @Query("SELECT channelId FROM guild_last_channels WHERE guildId = :guildId")
    suspend fun getLastChannel(guildId: String): String?
}

@Dao
interface DmChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<DmChannelEntity>)

    @Query("SELECT * FROM dm_channels ORDER BY updatedAt DESC")
    suspend fun getAll(): List<DmChannelEntity>
}

@Dao
interface FavoriteChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(channel: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT * FROM favorite_channels ORDER BY position ASC, createdAt ASC")
    suspend fun getAll(): List<FavoriteChannelEntity>
}

@Dao
interface ReadStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(readState: ReadStateEntity)

    @Query("SELECT * FROM read_states WHERE channelId = :channelId")
    suspend fun get(channelId: String): ReadStateEntity?
}

@Dao
interface NotificationFeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationFeedEntity)

    @Query("SELECT * FROM notification_feed ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<NotificationFeedEntity>

    @Query("UPDATE notification_feed SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notification_feed SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notification_feed")
    suspend fun clearAll()
}
