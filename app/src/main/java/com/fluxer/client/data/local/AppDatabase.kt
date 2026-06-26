package com.fluxer.client.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fluxer.client.data.local.dao.AuthSessionDao
import com.fluxer.client.data.local.dao.ChannelDao
import com.fluxer.client.data.local.dao.DmChannelDao
import com.fluxer.client.data.local.dao.FavoriteChannelDao
import com.fluxer.client.data.local.dao.GuildDao
import com.fluxer.client.data.local.dao.GuildLastChannelDao
import com.fluxer.client.data.local.dao.MessageDao
import com.fluxer.client.data.local.dao.NavigationStateDao
import com.fluxer.client.data.local.dao.NotificationFeedDao
import com.fluxer.client.data.local.dao.PendingMessageDao
import com.fluxer.client.data.local.dao.ReadStateDao
import com.fluxer.client.data.local.dao.UserPreferenceDao
import com.fluxer.client.data.local.model.AuthSessionEntity
import com.fluxer.client.data.local.model.ChannelEntity
import com.fluxer.client.data.local.model.DmChannelEntity
import com.fluxer.client.data.local.model.FavoriteChannelEntity
import com.fluxer.client.data.local.model.GuildEntity
import com.fluxer.client.data.local.model.GuildLastChannelEntity
import com.fluxer.client.data.local.model.MemberEntity
import com.fluxer.client.data.local.model.MessageEntity
import com.fluxer.client.data.local.model.NavigationStateEntity
import com.fluxer.client.data.local.model.NotificationFeedEntity
import com.fluxer.client.data.local.model.PendingMessageEntity
import com.fluxer.client.data.local.model.ReadStateEntity
import com.fluxer.client.data.local.model.RoleEntity
import com.fluxer.client.data.local.model.UserEntity
import com.fluxer.client.data.local.model.UserPreferenceEntity
import com.fluxer.client.data.local.model.VoiceSessionEntity

@Database(
    entities = [
        MessageEntity::class,
        ChannelEntity::class,
        GuildEntity::class,
        PendingMessageEntity::class,
        AuthSessionEntity::class,
        DmChannelEntity::class,
        GuildLastChannelEntity::class,
        FavoriteChannelEntity::class,
        ReadStateEntity::class,
        NotificationFeedEntity::class,
        UserPreferenceEntity::class,
        UserEntity::class,
        MemberEntity::class,
        RoleEntity::class,
        VoiceSessionEntity::class,
        NavigationStateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun channelDao(): ChannelDao
    abstract fun guildDao(): GuildDao
    abstract fun pendingMessageDao(): PendingMessageDao
    abstract fun authSessionDao(): AuthSessionDao
    abstract fun navigationStateDao(): NavigationStateDao
    abstract fun guildLastChannelDao(): GuildLastChannelDao
    abstract fun dmChannelDao(): DmChannelDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun readStateDao(): ReadStateDao
    abstract fun notificationFeedDao(): NotificationFeedDao
    abstract fun userPreferenceDao(): UserPreferenceDao
}
