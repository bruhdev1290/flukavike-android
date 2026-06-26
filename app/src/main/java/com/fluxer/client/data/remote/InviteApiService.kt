package com.fluxer.client.data.remote

import com.fluxer.client.data.model.InviteInfo
import retrofit2.Response
import retrofit2.http.*

interface InviteApiService {
    @GET("/api/invites/{code}")
    suspend fun getInvite(@Path("code") code: String): Response<InviteInfo>

    @POST("/api/invites/{code}")
    suspend fun joinViaInvite(@Path("code") code: String): Response<InviteInfo>
}
