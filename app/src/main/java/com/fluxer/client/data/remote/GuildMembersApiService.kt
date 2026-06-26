package com.fluxer.client.data.remote

import com.fluxer.client.data.model.GuildMember
import retrofit2.Response
import retrofit2.http.*

interface GuildMembersApiService {
    @GET("/api/guilds/{guildId}/members")
    suspend fun getGuildMembers(
        @Path("guildId") guildId: String,
        @Query("limit") limit: Int = 100
    ): Response<List<GuildMember>>
}
