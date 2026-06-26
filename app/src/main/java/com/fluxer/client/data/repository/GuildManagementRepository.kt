package com.fluxer.client.data.repository

import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.Server
import com.fluxer.client.data.remote.CreateChannelRequest
import com.fluxer.client.data.remote.CreateServerRequest
import com.fluxer.client.data.remote.GuildManagementApiService
import com.fluxer.client.util.Result
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuildManagementRepository @Inject constructor(
    private val api: GuildManagementApiService
) {
    suspend fun createServer(name: String): Result<Server> {
        return try {
            val response = api.createGuild(CreateServerRequest(name))
            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to create server: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "createServer failed")
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createChannel(guildId: String, name: String, isVoice: Boolean): Result<Channel> {
        return try {
            val type = if (isVoice) 2 else 0
            val response = api.createChannel(guildId, CreateChannelRequest(name, type))
            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to create channel: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "createChannel failed")
            Result.Error(e.message ?: "Network error")
        }
    }
}
