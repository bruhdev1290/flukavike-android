package com.fluxer.client.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.FluxerApiService
import com.fluxer.client.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val USER_SETTINGS_KEY = stringPreferencesKey("user_settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val apiService: FluxerApiService
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getSettings(): Flow<UserSettings> = dataStore.data
        .map { preferences ->
            val settingsJson = preferences[USER_SETTINGS_KEY]
            settingsJson?.let {
                try {
                    json.decodeFromString(it)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse settings")
                    UserSettings()
                }
            } ?: UserSettings()
        }
        .catch { e ->
            Timber.e(e, "Error reading settings")
            emit(UserSettings())
        }

    suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        return try {
            // Save locally first
            dataStore.edit { preferences ->
                preferences[USER_SETTINGS_KEY] = json.encodeToString(settings)
            }
            
            // Sync with server
            val request = UpdateSettingsRequest(
                theme = settings.theme,
                messageDisplay = settings.messageDisplay,
                fontSize = settings.fontSize,
                compactMode = settings.compactMode,
                showAnimations = settings.showAnimations,
                notificationsEnabled = settings.notificationsEnabled
            )
            
            val response = apiService.updateUserSettings(request)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Failed to sync settings: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save settings")
            Result.Error("Failed to save settings: ${e.message}")
        }
    }

    suspend fun fetchSettingsFromServer(): Result<UserSettings> {
        return try {
            val response = apiService.getUserSettings()
            if (response.isSuccessful) {
                val settings = response.body()
                settings?.let {
                    // Save to local storage
                    dataStore.edit { preferences ->
                        preferences[USER_SETTINGS_KEY] = json.encodeToString(it)
                    }
                    Result.Success(it)
                } ?: Result.Error("Empty response")
            } else {
                Result.Error("Failed to fetch settings: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch settings from server")
            Result.Error("Network error: ${e.message}")
        }
    }
}
