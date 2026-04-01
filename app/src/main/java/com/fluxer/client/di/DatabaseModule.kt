package com.fluxer.client.di

import android.content.Context
import androidx.room.Room
import com.fluxer.client.data.local.AppDatabase
import com.fluxer.client.data.local.dao.ChannelDao
import com.fluxer.client.data.local.dao.GuildDao
import com.fluxer.client.data.local.dao.MessageDao
import com.fluxer.client.data.local.dao.PendingMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fluxer_database"
        ).build()
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
}
