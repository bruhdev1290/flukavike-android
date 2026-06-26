package com.fluxer.client.data.repository

import com.fluxer.client.data.model.AddFriendRequest
import com.fluxer.client.data.model.Relationship
import com.fluxer.client.data.model.RelationshipTypeRequest
import com.fluxer.client.data.remote.FriendsApiService
import com.fluxer.client.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepository @Inject constructor(
    private val apiService: FriendsApiService
) {
    suspend fun getRelationships(): Result<List<Relationship>> = try {
        val response = apiService.getRelationships()
        if (response.isSuccessful) Result.Success(response.body() ?: emptyList())
        else Result.Error("Failed: ${response.code()}")
    } catch (e: Exception) {
        Result.Error("Network error: ${e.message}")
    }

    suspend fun addFriendByUsername(username: String): Result<Unit> = try {
        val response = apiService.addFriendByUsername(AddFriendRequest(username))
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error("Failed: ${response.code()}")
    } catch (e: Exception) {
        Result.Error("Network error: ${e.message}")
    }

    suspend fun removeRelationship(userId: String): Result<Unit> = try {
        val response = apiService.removeRelationship(userId)
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error("Failed: ${response.code()}")
    } catch (e: Exception) {
        Result.Error("Network error: ${e.message}")
    }

    suspend fun blockUser(userId: String): Result<Unit> = try {
        val response = apiService.updateRelationship(userId, RelationshipTypeRequest(2))
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error("Failed: ${response.code()}")
    } catch (e: Exception) {
        Result.Error("Network error: ${e.message}")
    }

    suspend fun acceptFriendRequest(userId: String): Result<Unit> = try {
        val response = apiService.updateRelationship(userId, RelationshipTypeRequest(1))
        if (response.isSuccessful) Result.Success(Unit)
        else Result.Error("Failed: ${response.code()}")
    } catch (e: Exception) {
        Result.Error("Network error: ${e.message}")
    }
}
