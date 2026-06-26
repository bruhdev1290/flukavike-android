package com.fluxer.client.data.local

import android.content.Context
import com.fluxer.client.BuildConfig
import com.fluxer.client.data.model.InstanceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstanceConfigStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCustomBaseUrl(): String? = prefs.getString(KEY_CUSTOM_BASE_URL, null)

    fun getDisplayDomain(): String = prefs.getString(KEY_DISPLAY_DOMAIN, null)
        ?: extractDisplayDomain(getActiveBaseUrl())

    fun getActiveBaseUrl(): String {
        return getCustomBaseUrl() ?: BuildConfig.FLUXER_BASE_URL
    }

    fun getActiveApiBaseUrl(): String {
        val active = getActiveBaseUrl()
        val parsed = active.toHttpUrlOrNull() ?: return active.trimEnd('/')
        val host = parsed.host.lowercase()

        val officialWebHost = when (host) {
            "fluxer.app", "web.fluxer.app", "api.fluxer.app" -> "web.fluxer.app"
            "canary.fluxer.app", "web.canary.fluxer.app", "api.canary.fluxer.app" -> "web.canary.fluxer.app"
            "fluxer.com", "web.fluxer.com", "api.fluxer.com" -> "web.fluxer.com"
            "canary.fluxer.com", "web.canary.fluxer.com", "api.canary.fluxer.com" -> "web.canary.fluxer.com"
            else -> null
        }

        if (officialWebHost != null) {
            return parsed.newBuilder()
                .host(officialWebHost)
                .encodedPath("/api/v1")
                .query(null)
                .build()
                .toString()
                .trimEnd('/')
        }

        return active.trimEnd('/')
    }

    fun getActiveWebSocketUrl(): String {
        return prefs.getString(KEY_DISCOVERED_WS_URL, null)
            ?: BuildConfig.FLUXER_WS_URL
    }

    fun saveCustomBaseUrl(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            prefs.edit()
                .remove(KEY_CUSTOM_BASE_URL)
                .remove(KEY_DISCOVERED_WS_URL)
                .remove(KEY_INSTANCE_SNAPSHOT)
                .remove(KEY_DISPLAY_DOMAIN)
                .apply()
            return BuildConfig.FLUXER_BASE_URL
        }

        val normalized = normalizeEndpoint(trimmed) ?: return null
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, normalized).apply()
        return normalized
    }

    fun buildWellKnownUrl(apiEndpoint: String = getActiveBaseUrl()): String {
        val parsed = apiEndpoint.toHttpUrlOrNull()
        if (parsed == null) {
            val base = apiEndpoint.trimEnd('/').removeSuffix("/api")
            return "$base/api/.well-known/fluxer"
        }
        val official = parsed.host == "api.fluxer.app" || parsed.host == "api.canary.fluxer.app"
        val path = if (official) "/.well-known/fluxer" else "/api/.well-known/fluxer"
        return parsed.newBuilder()
            .encodedPath(path)
            .query(null)
            .build()
            .toString()
    }

    fun saveDiscoveredConfig(config: InstanceConfig) {
        val api = config.resolvedApi().takeIf { it.isNotBlank() } ?: getActiveBaseUrl()
        val gateway = config.resolvedGateway()
        prefs.edit()
            .putString(KEY_CUSTOM_BASE_URL, stripTrailingSlashes(api))
            .putString(KEY_DISPLAY_DOMAIN, extractDisplayDomain(api))
            .putString(KEY_INSTANCE_SNAPSHOT, Json.encodeToString(config))
            .apply()
        saveDiscoveredWebSocketUrl(gateway)
    }

    fun saveDiscoveredWebSocketUrl(url: String) {
        if (url.isNotBlank()) {
            prefs.edit().putString(KEY_DISCOVERED_WS_URL, url).apply()
        }
    }

    private fun normalizeEndpoint(input: String): String? {
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
        val path = parsed.encodedPath
            .ifBlank { "/api" }
            .let { if (it == "/") "/api" else it.trimEnd('/') }

        return "${parsed.scheme}://${parsed.host}$portPart$path"
    }

    private fun extractDisplayDomain(endpoint: String): String {
        val host = endpoint.toHttpUrlOrNull()?.host ?: endpoint
        return host.removePrefix("api.").lowercase()
    }

    private fun stripTrailingSlashes(value: String): String {
        return value.trimEnd('/')
    }

    companion object {
        private const val PREFS_NAME = "fluxer_instance_config"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private const val KEY_DISCOVERED_WS_URL = "discovered_ws_url"
        private const val KEY_INSTANCE_SNAPSHOT = "instance_snapshot"
        private const val KEY_DISPLAY_DOMAIN = "display_domain"
    }
}
