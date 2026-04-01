package com.fluxer.client.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fluxer.client.data.local.dao.ChannelDao
import com.fluxer.client.data.local.dao.GuildDao
import com.fluxer.client.data.local.dao.MessageDao
import com.fluxer.client.data.local.dao.PendingMessageDao
import com.fluxer.client.data.local.model.ChannelEntity
import com.fluxer.client.data.local.model.GuildEntity
import com.fluxer.client.data.local.model.MessageEntity
import com.fluxer.client.data.local.model.PendingMessageEntity

@Database(
    entities = [
        MessageEntity::class,
        ChannelEntity::class,
        GuildEntity::class,
        PendingMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun channelDao(): ChannelDao
    abstract fun guildDao(): GuildDao
    abstract fun pendingMessageDao(): PendingMessageDao
}
