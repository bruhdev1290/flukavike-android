package com.fluxer.client.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class MessageAckRequest(
    @SerialName("mention_count")
    val mentionCount: Int? = null,
    val manual: Boolean? = null
)

interface ReadStateApiService {
    @POST("/api/channels/{channelId}/messages/{messageId}/ack")
    suspend fun acknowledgeMessage(
        @Path("channelId") channelId: String,
        @Path("messageId") messageId: String,
        @Body request: MessageAckRequest = MessageAckRequest()
    ): Response<Unit>
}
