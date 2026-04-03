package com.fluxer.client.di

import android.content.Context
import com.fluxer.client.BuildConfig
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.local.SecureCookieStorage
import com.fluxer.client.data.local.AuthTokenStorage
import com.fluxer.client.data.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dagger Hilt module providing network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = BuildConfig.DEBUG
    }

    @Provides
    @Singleton
    fun provideSecureCookieStorage(
        @ApplicationContext context: Context
    ): SecureCookieStorage = SecureCookieStorage(context)

    @Provides
    @Singleton
    fun provideCsrfInterceptor(
        cookieStorage: SecureCookieStorage
    ): CsrfInterceptor = CsrfInterceptor(cookieStorage)

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        authTokenStorage: AuthTokenStorage
    ): AuthInterceptor = AuthInterceptor(authTokenStorage)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieStorage: SecureCookieStorage,
        csrfInterceptor: CsrfInterceptor,
        baseUrlOverrideInterceptor: BaseUrlOverrideInterceptor,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        authAuthenticator: AuthAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Cookie handling - CRITICAL for HttpOnly session cookies
            .cookieJar(cookieStorage as CookieJar)
            // Runtime instance switching for all API calls
            .addInterceptor(baseUrlOverrideInterceptor)
            // Auth token injection
            .addInterceptor(authInterceptor)
            // CSRF protection
            .addInterceptor(csrfInterceptor)
            // Logging
            .addInterceptor(loggingInterceptor)
            // Auth handling for 401s
            .authenticator(authAuthenticator)
            // Timeouts
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Retry on connection failure
            .retryOnConnectionFailure(true)
            // Follow redirects (important for auth flows)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl(BuildConfig.FLUXER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideFluxerApiService(retrofit: Retrofit): FluxerApiService {
        return retrofit.create(FluxerApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGatewayWebSocketManager(
        cookieStorage: SecureCookieStorage,
        json: Json,
        instanceConfigStore: InstanceConfigStore
    ): GatewayWebSocketManager {
        return GatewayWebSocketManager(cookieStorage, json, instanceConfigStore)
    }
}
