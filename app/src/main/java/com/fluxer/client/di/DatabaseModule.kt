package com.fluxer.client.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxer.client.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS auth_sessions (userId TEXT NOT NULL PRIMARY KEY, active INTEGER NOT NULL, username TEXT, displayName TEXT, avatarUrl TEXT, instanceSnapshotJson TEXT, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS dm_channels (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, lastMessageId TEXT, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS guild_last_channels (guildId TEXT NOT NULL PRIMARY KEY, channelId TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS favorite_channels (channelId TEXT NOT NULL PRIMARY KEY, guildId TEXT, position INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS read_states (channelId TEXT NOT NULL PRIMARY KEY, lastReadMessageId TEXT, mentionCount INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS notification_feed (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, guildId TEXT, channelId TEXT, messageId TEXT, createdAt INTEGER NOT NULL, read INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS user_preferences (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS users (id TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, displayName TEXT, avatarUrl TEXT, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS members (guildId TEXT NOT NULL, userId TEXT NOT NULL, nickname TEXT, roleIds TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(guildId, userId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS roles (guildId TEXT NOT NULL, roleId TEXT NOT NULL, name TEXT NOT NULL, color INTEGER, position INTEGER NOT NULL, PRIMARY KEY(guildId, roleId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS voice_sessions (channelId TEXT NOT NULL PRIMARY KEY, guildId TEXT, active INTEGER NOT NULL, muted INTEGER NOT NULL, deafened INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS navigation_state (id TEXT NOT NULL PRIMARY KEY, activePath TEXT NOT NULL, homePath TEXT NOT NULL, notificationsPath TEXT NOT NULL, youPath TEXT NOT NULL, preReconnectPath TEXT, pendingPath TEXT, updatedAt INTEGER NOT NULL)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dm_channels ADD COLUMN type INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE dm_channels ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE dm_channels ADD COLUMN recipientId TEXT")
            db.execSQL("ALTER TABLE dm_channels ADD COLUMN recipientUsername TEXT")
            db.execSQL("ALTER TABLE dm_channels ADD COLUMN recipientDisplayName TEXT")
            db.execSQL("ALTER TABLE dm_channels ADD COLUMN recipientAvatarUrl TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fluxer_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    @Provides
    fun provideMessageDao(appDatabase: AppDatabase): MessageDao {
        return appDatabase.messageDao()
    }

    @Provides
    fun provideChannelDao(appDatabase: AppDatabase): ChannelDao {
        return appDatabase.channelDao()
    }

    @Provides
    fun provideGuildDao(appDatabase: AppDatabase): GuildDao {
        return appDatabase.guildDao()
    }

    @Provides
    fun providePendingMessageDao(appDatabase: AppDatabase): PendingMessageDao {
        return appDatabase.pendingMessageDao()
    }

    @Provides
    fun provideAuthSessionDao(appDatabase: AppDatabase): AuthSessionDao =
        appDatabase.authSessionDao()

    @Provides
    fun provideNavigationStateDao(appDatabase: AppDatabase): NavigationStateDao =
        appDatabase.navigationStateDao()

    @Provides
    fun provideGuildLastChannelDao(appDatabase: AppDatabase): GuildLastChannelDao =
        appDatabase.guildLastChannelDao()

    @Provides
    fun provideDmChannelDao(appDatabase: AppDatabase): DmChannelDao =
        appDatabase.dmChannelDao()

    @Provides
    fun provideFavoriteChannelDao(appDatabase: AppDatabase): FavoriteChannelDao =
        appDatabase.favoriteChannelDao()

    @Provides
    fun provideReadStateDao(appDatabase: AppDatabase): ReadStateDao =
        appDatabase.readStateDao()

    @Provides
    fun provideNotificationFeedDao(appDatabase: AppDatabase): NotificationFeedDao =
        appDatabase.notificationFeedDao()

    @Provides
    fun provideUserPreferenceDao(appDatabase: AppDatabase): UserPreferenceDao =
        appDatabase.userPreferenceDao()
}
