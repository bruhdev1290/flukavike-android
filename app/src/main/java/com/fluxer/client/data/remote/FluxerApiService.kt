package com.fluxer.client.data.remote

import com.fluxer.client.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Fluxer REST API Service
 * All endpoints return Response<T> to handle HTTP status codes properly
 */
interface FluxerApiService {

    // ==================== DISCOVERY ====================

    @GET("/.well-known/fluxer")
    suspend fun discoverInstance(): Response<InstanceConfig>

    // ==================== AUTH ====================

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest,
        @Header("X-Captcha-Token") captchaToken: String? = null,
        @Header("X-Captcha-Type") captchaType: String? = null
    ): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("/api/auth/refresh")
    suspend fun refreshToken(): Response<AuthResponse>

    @GET("/api/auth/csrf")
    suspend fun getCsrfToken(): Response<CsrfResponse>

    @GET("/api/auth/me")
    suspend fun getCurrentUser(@Header("Authorization") authToken: String? = null): Response<User>
    
    // ==================== USERS ====================
    
    @GET("/api/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<User>
    
    @PATCH("/api/users/@me")
    suspend fun updateCurrentUser(@Body updates: Map<String, String>): Response<User>
    
    @GET("/api/users/@me/guilds")
    suspend fun getUserGuilds(): Response<List<Server>>
    
    // ==================== MESSAGES ====================
    
    @GET("/api/channels/{channelId}/messages")
    suspend fun getMessages(
        @Path("channelId") channelId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null
    ): Response<List<Message>>
    
    @POST("/api/channels/{channelId}/messages")
    suspend fun sendMessage(
        @Path("channelId") channelId: String,
        @Body request: SendMessageRequest
    ): Response<Message>
    
    @PATCH("/api/channels/{channelId}/messages/{messageId}")
    suspend fun editMessage(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String,
        @Body request: SendMessageRequest
    ): Response<Message>
    
    @DELETE("/api/channels/{channelId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String
    ): Response<Unit>

    @GET("/api/channels/{channelId}/messages/search")
    suspend fun searchMessages(
        @Path("channelId") channelId: String,
        @Query("q") query: String
    ): Response<List<Message>>
    
    // ==================== CHANNELS ====================
    
    @GET("/api/channels/{channelId}")
    suspend fun getChannel(@Path("channelId") channelId: String): Response<Channel>
    
    @GET("/api/guilds/{guildId}/channels")
    suspend fun getGuildChannels(@Path("guildId") guildId: String): Response<List<Channel>>
    
    // ==================== SERVERS/GUILDS ====================
    
    @GET("/api/guilds/{guildId}")
    suspend fun getGuild(@Path("guildId") guildId: String): Response<Server>
    
    @POST("/api/guilds/{guildId}/members/{userId}/typing")
    suspend fun sendTypingIndicator(
        @Path("guildId") guildId: String,
        @Path("userId") userId: String
    ): Response<Unit>
}
