package com.fluxer.client.data.remote

import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.Server
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class CreateServerRequest(val name: String)

@Serializable
data class CreateChannelRequest(
    val name: String,
    val type: Int = 0
)

interface GuildManagementApiService {
    @POST("/api/guilds")
    suspend fun createGuild(@Body request: CreateServerRequest): Response<Server>

    @POST("/api/guilds/{guildId}/channels")
    suspend fun createChannel(
        @Path("guildId") guildId: String,
        @Body request: CreateChannelRequest
    ): Response<Channel>
}
