package com.fluxer.client.data.remote

import com.fluxer.client.data.model.Message
import retrofit2.Response
import retrofit2.http.*

interface PinApiService {
    @GET("/api/channels/{channelId}/pins")
    suspend fun getPinnedMessages(
        @Path("channelId") channelId: String
    ): Response<List<Message>>

    @PUT("/api/channels/{channelId}/pins/{messageId}")
    suspend fun pinMessage(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String
    ): Response<Unit>

    @DELETE("/api/channels/{channelId}/pins/{messageId}")
    suspend fun unpinMessage(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String
    ): Response<Unit>
}
