package com.fluxer.client.data.remote

import com.fluxer.client.data.model.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AvatarApiService {
    @Multipart
    @PATCH("/api/users/@me")
    suspend fun updateAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<User>
}
