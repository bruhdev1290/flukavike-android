package com.fluxer.client.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import timber.log.Timber
import java.io.IOException

/**
 * Bulletproof secure cookie storage using EncryptedSharedPreferences.
 * Handles HttpOnly cookies (like fluxer_session) that cannot be accessed via JavaScript.
 */
class SecureCookieStorage(context: Context) : CookieJar {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to create EncryptedSharedPreferences, clearing and retrying")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _sessionCookieFlow = MutableStateFlow<String?>(null)
    val sessionCookieFlow: StateFlow<String?> = _sessionCookieFlow.asStateFlow()

    private val memoryCache = mutableMapOf<String, List<Cookie>>()

    init {
        loadSessionCookie()
    }

    /**
     * Load all cookies for a given URL from secure storage + memory cache
     */
    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val key = url.host
        
        // Check memory cache first for performance
        memoryCache[key]?.let { cached ->
            val validCookies = cached.filter { it.expiresAt > System.currentTimeMillis() }
            if (validCookies.isNotEmpty()) {
                Timber.d("Loading ${validCookies.size} cookies from memory cache for $key")
                return validCookies
            }
        }

        // Load from encrypted storage
        val cookies = mutableListOf<Cookie>()
        val allEntries = encryptedPrefs.all
        
        allEntries.forEach { (storageKey, value) ->
            if (storageKey.startsWith(COOKIE_PREFIX)) {
                try {
                    val cookie = deserializeCookie(value as String)
                    // Check if cookie matches this domain and hasn't expired
                    if (cookie.matches(url) && cookie.expiresAt > System.currentTimeMillis()) {
                        cookies.add(cookie)
                        Timber.d("Loaded cookie: ${cookie.name} for ${url.host}")
                    } else if (cookie.expiresAt <= System.currentTimeMillis()) {
                        // Clean up expired cookies
                        removeCookie(storageKey)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to deserialize cookie: $storageKey")
                    removeCookie(storageKey)
                }
            }
        }

        memoryCache[key] = cookies
        return cookies
    }

    /**
     * Save cookies from server response to secure storage
     */
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        
        cookies.forEach { cookie ->
            try {
                val storageKey = "$COOKIE_PREFIX${cookie.domain}_${cookie.name}"
                
                if (cookie.expiresAt <= System.currentTimeMillis() || cookie.value.isBlank()) {
                    // Remove expired/empty cookies
                    removeCookie(storageKey)
                    Timber.d("Removed expired/empty cookie: ${cookie.name}")
                } else {
                    // Store the cookie securely
                    val serialized = serializeCookie(cookie)
                    encryptedPrefs.edit().putString(storageKey, serialized).apply()
                    
                    // Track session cookie specifically
                    if (cookie.name == SESSION_COOKIE_NAME) {
                        _sessionCookieFlow.value = cookie.value
                        Timber.i("🔒 Session cookie updated: ${cookie.value.take(8)}...")
                    }
                    
                    Timber.d("Saved cookie: ${cookie.name} for ${cookie.domain}, expires: ${cookie.expiresAt}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save cookie: ${cookie.name}")
            }
        }

        // Update memory cache
        memoryCache[key] = loadForRequest(url)
    }

    /**
     * Get the current session token (fluxer_session) if available
     */
    fun getSessionToken(): String? {
        return _sessionCookieFlow.value ?: encryptedPrefs.all.entries
            .find { it.key.contains(SESSION_COOKIE_NAME) }
            ?.let { 
                try {
                    deserializeCookie(it.value as String).value
                } catch (e: Exception) {
                    null
                }
            }
    }

    /**
     * Check if we have a valid session
     */
    fun hasValidSession(): Boolean {
        return getSessionToken() != null
    }

    /**
     * Clear all cookies - used for logout
     */
    @Synchronized
    fun clearAllCookies() {
        Timber.i("🧹 Clearing all cookies")
        encryptedPrefs.edit().clear().apply()
        memoryCache.clear()
        _sessionCookieFlow.value = null
    }

    /**
     * Get cookies as header string for WebSocket connections
     */
    fun getCookieHeader(url: HttpUrl): String {
        val cookies = loadForRequest(url)
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    private fun loadSessionCookie() {
        val sessionEntry = encryptedPrefs.all.entries.find { 
            it.key.contains(SESSION_COOKIE_NAME) 
        }
        sessionEntry?.let { 
            try {
                val cookie = deserializeCookie(it.value as String)
                if (cookie.expiresAt > System.currentTimeMillis()) {
                    _sessionCookieFlow.value = cookie.value
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load session cookie")
            }
        }
    }

    private fun removeCookie(storageKey: String) {
        encryptedPrefs.edit().remove(storageKey).apply()
    }

    private fun serializeCookie(cookie: Cookie): String {
        return buildString {
            append(cookie.name).append(DELIMITER)
            append(cookie.value).append(DELIMITER)
            append(cookie.expiresAt).append(DELIMITER)
            append(cookie.domain).append(DELIMITER)
            append(cookie.path).append(DELIMITER)
            append(cookie.secure).append(DELIMITER)
            append(cookie.httpOnly).append(DELIMITER)
            append(cookie.hostOnly)
        }
    }

    private fun deserializeCookie(serialized: String): Cookie {
        val parts = serialized.split(DELIMITER)
        if (parts.size != 8) {
            throw IOException("Invalid cookie format")
        }
        
        return Cookie.Builder()
            .name(parts[0])
            .value(parts[1])
            .expiresAt(parts[2].toLong())
            .domain(parts[3])
            .path(parts[4])
            .apply {
                if (parts[5].toBoolean()) secure()
                if (parts[6].toBoolean()) httpOnly()
            }
            .build()
    }

    companion object {
        private const val PREFS_NAME = "fluxer_secure_cookies"
        private const val COOKIE_PREFIX = "cookie_"
        private const val SESSION_COOKIE_NAME = "fluxer_session"
        private const val DELIMITER = "|||"
    }
}
