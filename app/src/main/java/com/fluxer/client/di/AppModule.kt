package com.fluxer.client.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.fluxer.client.data.local.dataStore
import com.fluxer.client.service.LiveKitVoiceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideTimberTree(): Timber.Tree {
        return if (true) { // BuildConfig.DEBUG
            Timber.DebugTree()
        } else {
            // Production tree - could log to Crashlytics
            Timber.DebugTree()
        }
    }

    @Provides
    @Singleton
    fun provideLiveKitVoiceManager(
        @ApplicationContext context: Context
    ): LiveKitVoiceManager = LiveKitVoiceManager(context)
}
