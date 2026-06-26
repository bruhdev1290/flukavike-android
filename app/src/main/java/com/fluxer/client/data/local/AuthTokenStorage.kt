package com.fluxer.client.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Volatile
    var token: String? = prefs.getString(KEY_ACTIVE_USER_ID, null)
        ?.let { activeUserId -> prefs.getString(tokenKey(activeUserId), null) }
        private set

    val activeUserId: String?
        get() = prefs.getString(KEY_ACTIVE_USER_ID, null)

    fun setToken(token: String?) {
        this.token = token
        val userId = activeUserId ?: return
        if (token == null) {
            prefs.edit().remove(tokenKey(userId)).apply()
        } else {
            prefs.edit().putString(tokenKey(userId), token).apply()
        }
    }

    fun saveToken(userId: String, token: String) {
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(tokenKey(userId), token)
            .apply()
        this.token = token
    }

    fun loadToken(userId: String): String? {
        return prefs.getString(tokenKey(userId), null)
    }

    fun setActiveUser(userId: String) {
        prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply()
        token = loadToken(userId)
    }

    fun deleteToken(userId: String) {
        prefs.edit().remove(tokenKey(userId)).apply()
        if (activeUserId == userId) {
            prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
            token = null
        }
    }

    fun clear() {
        activeUserId?.let { prefs.edit().remove(tokenKey(it)).apply() }
        prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
        token = null
    }

    private fun tokenKey(userId: String) = "$KEY_TOKEN_PREFIX$userId"

    companion object {
        private const val PREFS_NAME = "fluxer_auth_tokens"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_TOKEN_PREFIX = "token_"
    }
}
