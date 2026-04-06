// =============================================================================
// !! DO NOT TOUCH ENDPOINT PATHS !!
// All paths are verified against the live Fluxer API:
// - Current user: /api/users/@me (NOT /api/auth/me — that returns 404)
// - Guild channels: /api/guilds/{guildId}/channels (guilds from /api/users/@me/guilds do NOT embed channels)
// See CLAUDE.md for full details.
// =============================================================================
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

    @GET("/api/users/@me")
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

    // ==================== PROFILE & SETTINGS ====================
    
    @GET("/api/users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<UserProfile>
    
    @GET("/api/users/@me/profile")
    suspend fun getCurrentUserProfile(): Response<UserProfile>
    
    @PATCH("/api/users/@me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserProfile>
    
    @GET("/api/users/@me/settings")
    suspend fun getUserSettings(): Response<UserSettings>
    
    @PATCH("/api/users/@me/settings")
    suspend fun updateUserSettings(@Body request: UpdateSettingsRequest): Response<UserSettings>
    
    // ==================== DIRECT MESSAGES ====================
    
    @GET("/api/users/@me/channels")
    suspend fun getDMChannels(): Response<List<Channel>>
    
    @POST("/api/users/@me/channels")
    suspend fun createDMChannel(@Body request: CreateDMRequest): Response<Channel>
    
    @GET("/api/channels/{channelId}/recipients")
    suspend fun getDMRecipients(@Path("channelId") channelId: String): Response<List<User>>
    
    // ==================== VOICE CHANNELS ====================
    
    @POST("/api/channels/{channelId}/voice/join")
    suspend fun joinVoiceChannel(
        @Path("channelId") channelId: String,
        @Body request: JoinVoiceChannelRequest
    ): Response<VoiceTokenResponse>
    
    @POST("/api/channels/{channelId}/voice/leave")
    suspend fun leaveVoiceChannel(
        @Path("channelId") channelId: String
    ): Response<Unit>
    
    @GET("/api/channels/{channelId}/voice/participants")
    suspend fun getVoiceParticipants(@Path("channelId") channelId: String): Response<List<VoiceParticipant>>
    
    @PATCH("/api/channels/{channelId}/voice/state")
    suspend fun updateVoiceState(
        @Path("channelId") channelId: String,
        @Body request: UpdateVoiceStateRequest
    ): Response<VoiceState>
    
    @GET("/api/voice/regions")
    suspend fun getVoiceRegions(): Response<List<VoiceRegion>>
    
    // ==================== CALLS ====================
    
    @POST("/api/calls")
    suspend fun initiateCall(@Body request: InitiateCallRequest): Response<CallResponse>
    
    @POST("/api/calls/{callId}/join")
    suspend fun joinCall(
        @Path("callId") callId: String,
        @Body request: JoinCallRequest
    ): Response<CallResponse>
    
    @POST("/api/calls/{callId}/leave")
    suspend fun leaveCall(@Path("callId") callId: String): Response<Unit>
    
    @POST("/api/calls/{callId}/decline")
    suspend fun declineCall(@Path("callId") callId: String): Response<Unit>
    
    @GET("/api/calls/{callId}")
    suspend fun getCall(@Path("callId") callId: String): Response<Call>
    
    // ==================== NOTIFICATIONS ====================
    
    @POST("/api/users/@me/notifications/token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<Unit>
    
    @DELETE("/api/users/@me/notifications/token")
    suspend fun unregisterFcmToken(): Response<Unit>
    
    @GET("/api/users/@me/notifications/settings")
    suspend fun getNotificationSettings(): Response<NotificationSettings>
    
    @PATCH("/api/users/@me/notifications/settings")
    suspend fun updateNotificationSettings(@Body settings: NotificationSettings): Response<NotificationSettings>
    
    // ==================== GLOBAL SEARCH ====================
    
    @GET("/api/search")
    suspend fun globalSearch(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<GlobalSearchResults>
    
    @GET("/api/search/suggestions")
    suspend fun getSearchSuggestions(@Query("q") query: String): Response<List<SearchSuggestion>>
    
    // ==================== REACTIONS ====================
    
    @PUT("/api/channels/{channelId}/messages/{messageId}/reactions/{emoji}/@me")
    suspend fun addReaction(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String,
        @Path("emoji") emoji: String
    ): Response<Unit>
    
    @DELETE("/api/channels/{channelId}/messages/{messageId}/reactions/{emoji}/@me")
    suspend fun removeReaction(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String,
        @Path("emoji") emoji: String
    ): Response<Unit>
}

@kotlinx.serialization.Serializable
data class CreateDMRequest(
    @kotlinx.serialization.SerialName("recipient_id")
    val recipientId: String
)

@kotlinx.serialization.Serializable
data class GlobalSearchResults(
    val messages: List<Message> = emptyList(),
    val users: List<User> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val guilds: List<Server> = emptyList()
)

@kotlinx.serialization.Serializable
data class SearchSuggestion(
    val type: String,
    val value: String,
    val label: String
)
