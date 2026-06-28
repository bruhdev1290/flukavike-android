package com.fluxer.client.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CdnClient

@Module
@InstallIn(SingletonComponent::class)
object CdnModule {

    /**
     * A separate OkHttpClient for Coil image loading that does NOT include
     * BaseUrlOverrideInterceptor. The base-URL interceptor rewrites every request's
     * host to the API host (web.fluxer.app), which breaks CDN image URLs like
     * https://fluxerusercontent.com/avatars/... → 404.
     */
    @CdnClient
    @Provides
    @Singleton
    fun provideCdnOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()
    }
}
