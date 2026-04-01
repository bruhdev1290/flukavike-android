package com.fluxer.client.data.local

import android.content.Context
import com.fluxer.client.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstanceConfigStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCustomBaseUrl(): String? = prefs.getString(KEY_CUSTOM_BASE_URL, null)

    fun getActiveBaseUrl(): String {
        return getCustomBaseUrl() ?: BuildConfig.FLUXER_BASE_URL
    }

    fun getActiveWebSocketUrl(): String {
        val baseUrl = getActiveBaseUrl().toHttpUrlOrNull() ?: return BuildConfig.FLUXER_WS_URL
        val wsScheme = if (baseUrl.scheme == "https") "wss" else "ws"
        val portPart = when {
            wsScheme == "ws" && baseUrl.port == 80 -> ""
            wsScheme == "wss" && baseUrl.port == 443 -> ""
            else -> ":${baseUrl.port}"
        }
        return "$wsScheme://${baseUrl.host}$portPart"
    }

    fun saveCustomBaseUrl(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(KEY_CUSTOM_BASE_URL).apply()
            return BuildConfig.FLUXER_BASE_URL
        }

        val normalized = normalizeToBaseUrl(trimmed) ?: return null
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, normalized).apply()
        return normalized
    }

    private fun normalizeToBaseUrl(input: String): String? {
        val withScheme = if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else {
            "https://$input"
        }

        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        val portPart = when {
            parsed.scheme == "http" && parsed.port == 80 -> ""
            parsed.scheme == "https" && parsed.port == 443 -> ""
            else -> ":${parsed.port}"
        }

        return "${parsed.scheme}://${parsed.host}$portPart/"
    }

    companion object {
        private const val PREFS_NAME = "fluxer_instance_config"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
    }
}
