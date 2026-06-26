package com.fluxer.client.di

import com.fluxer.client.data.remote.AvatarApiService
import com.fluxer.client.data.remote.FriendsApiService
import com.fluxer.client.data.remote.GuildManagementApiService
import com.fluxer.client.data.remote.GuildMembersApiService
import com.fluxer.client.data.remote.InviteApiService
import com.fluxer.client.data.remote.PinApiService
import com.fluxer.client.data.remote.ReadStateApiService
import com.fluxer.client.data.remote.UploadApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeaturesModule {

    @Provides @Singleton
    fun provideUploadApiService(retrofit: Retrofit): UploadApiService =
        retrofit.create(UploadApiService::class.java)

    @Provides @Singleton
    fun provideFriendsApiService(retrofit: Retrofit): FriendsApiService =
        retrofit.create(FriendsApiService::class.java)

    @Provides @Singleton
    fun providePinApiService(retrofit: Retrofit): PinApiService =
        retrofit.create(PinApiService::class.java)

    @Provides @Singleton
    fun provideGuildMembersApiService(retrofit: Retrofit): GuildMembersApiService =
        retrofit.create(GuildMembersApiService::class.java)

    @Provides @Singleton
    fun provideInviteApiService(retrofit: Retrofit): InviteApiService =
        retrofit.create(InviteApiService::class.java)

    @Provides @Singleton
    fun provideAvatarApiService(retrofit: Retrofit): AvatarApiService =
        retrofit.create(AvatarApiService::class.java)

    @Provides @Singleton
    fun provideGuildManagementApiService(retrofit: Retrofit): GuildManagementApiService =
        retrofit.create(GuildManagementApiService::class.java)

    @Provides @Singleton
    fun provideReadStateApiService(retrofit: Retrofit): ReadStateApiService =
        retrofit.create(ReadStateApiService::class.java)
}
