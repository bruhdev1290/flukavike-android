package com.fluxer.client.util

import android.content.Context
import com.fluxer.client.data.model.NotificationSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object NotificationPreferences {
    private const val PREFS_NAME = "fluxer_notification_preferences"
    private const val SETTINGS_KEY = "settings"
    private val json = Json { ignoreUnknownKeys = true }

    fun get(context: Context): NotificationSettings {
        val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SETTINGS_KEY, null)
            ?: return NotificationSettings()
        return runCatching { json.decodeFromString<NotificationSettings>(encoded) }
            .getOrDefault(NotificationSettings())
    }

    fun save(context: Context, settings: NotificationSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SETTINGS_KEY, json.encodeToString(settings))
            .apply()
    }
}
