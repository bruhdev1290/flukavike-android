package com.fluxer.client.data.repository

import android.content.Context
import android.net.Uri
import com.fluxer.client.data.model.ServerProfile
import com.fluxer.client.data.model.UpdateProfileRequest
import com.fluxer.client.data.model.User
import com.fluxer.client.data.model.UserProfile
import com.fluxer.client.data.remote.AvatarApiService
import com.fluxer.client.data.remote.FluxerApiService
import com.fluxer.client.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: FluxerApiService,
    private val avatarApiService: AvatarApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun getServerProfile(guildId: String): Result<ServerProfile> {
        return try {
            val response = apiService.getGuild(guildId)
            if (response.isSuccessful) {
                response.body()?.let { server ->
                    Result.Success(
                        ServerProfile(
                            id = server.id,
                            name = server.name,
                            iconUrl = server.iconUrl,
                            bannerUrl = server.bannerUrl,
                            description = server.description,
                            vanityUrl = server.vanityUrl,
                            ownerId = server.ownerId,
                            memberCount = server.memberCount,
                            onlineCount = server.onlineCount,
                            createdAt = server.createdAt
                        )
                    )
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to load server profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load server profile")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it.toUserProfile())
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
                    Result.Success(it.toUserProfile())
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

    suspend fun updateAvatar(fileUri: Uri): Result<User> {
        val cr = context.contentResolver
        val mimeType = cr.getType(fileUri) ?: "image/jpeg"
        val bytes = cr.openInputStream(fileUri)?.readBytes()
            ?: return Result.Error("Could not read file")
        return try {
            val part = MultipartBody.Part.createFormData(
                "avatar", "avatar.jpg",
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            val r = avatarApiService.updateAvatar(part)
            if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty response")
            else Result.Error("Upload failed: ${r.code()}")
        } catch (e: Exception) {
            Timber.e(e, "Avatar upload failed")
            Result.Error(e.message ?: "Network error")
        }
    }
}
