package com.fluxer.client.data.repository

import com.fluxer.client.data.model.UpdateProfileRequest
import com.fluxer.client.data.model.UserProfile
import com.fluxer.client.data.remote.FluxerApiService
import com.fluxer.client.util.Result
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: FluxerApiService
) {
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to load profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user profile")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return try {
            val response = apiService.getCurrentUserProfile()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to load profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load current user profile")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<UserProfile> {
        return try {
            val response = apiService.updateProfile(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to update profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update profile")
            Result.Error("Network error: ${e.message}")
        }
    }
}
