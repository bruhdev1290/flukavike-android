package com.fluxer.client.data.remote

import com.fluxer.client.data.model.Message
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface UploadApiService {
    @Multipart
    @POST("/api/channels/{channelId}/messages")
    suspend fun sendMessageWithAttachment(
        @Path("channelId") channelId: String,
        @Part("payload_json; type=application/json") json: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Message>
}
