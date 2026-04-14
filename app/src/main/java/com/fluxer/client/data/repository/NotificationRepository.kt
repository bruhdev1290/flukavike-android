package com.fluxer.client.data.repository

import com.fluxer.client.data.model.FcmTokenRequest
import com.fluxer.client.data.model.NotificationSettings
import com.fluxer.client.data.model.PushTokenRequest
import com.fluxer.client.data.remote.FluxerApiService
import com.fluxer.client.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: FluxerApiService
) {
    suspend fun registerFcmToken(token: String): Result<Unit> {
        return try {
            val request = FcmTokenRequest(
                fcmToken = token,
                deviceType = "android",
                deviceName = android.os.Build.MODEL
            )
            val response = apiService.registerFcmToken(request)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to register token: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to register FCM token")
            Result.Error("Network error: ${e.message}")
        }
    }

    /**
     * Register a push token with the backend.
     * Supports both FCM and UnifiedPush providers.
     */
    suspend fun registerPushToken(
        token: String,
        provider: String,
        instance: String? = null
    ): Result<Unit> {
        return try {
            val request = PushTokenRequest(
                token = token,
                provider = provider,
                deviceType = "android",
                deviceName = android.os.Build.MODEL,
                instance = instance
            )
            val response = apiService.registerPushToken(request)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to register $provider token: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to register $provider token")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun unregisterFcmToken(): Result<Unit> {
        return try {
            val response = apiService.unregisterFcmToken()
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to unregister token: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister FCM token")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun unregisterPushToken(): Result<Unit> {
        return try {
            val response = apiService.unregisterPushToken()
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to unregister push token: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister push token")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun getNotificationSettings(): Result<NotificationSettings> {
        return try {
            val response = apiService.getNotificationSettings()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to load settings: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification settings")
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings): Result<NotificationSettings> {
        return try {
            val response = apiService.updateNotificationSettings(settings)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to update settings: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification settings")
            Result.Error("Network error: ${e.message}")
        }
    }
}
