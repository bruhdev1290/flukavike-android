package com.fluxer.client.data.remote

import com.fluxer.client.data.model.Relationship
import com.fluxer.client.data.model.AddFriendRequest
import com.fluxer.client.data.model.RelationshipTypeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface FriendsApiService {
    @GET("/api/users/@me/relationships")
    suspend fun getRelationships(): Response<List<Relationship>>

    @POST("/api/users/@me/relationships")
    suspend fun addFriendByUsername(@Body request: AddFriendRequest): Response<Unit>

    @PUT("/api/users/@me/relationships/{userId}")
    suspend fun updateRelationship(
        @Path("userId") userId: String,
        @Body request: RelationshipTypeRequest
    ): Response<Unit>

    @DELETE("/api/users/@me/relationships/{userId}")
    suspend fun removeRelationship(@Path("userId") userId: String): Response<Unit>
}
