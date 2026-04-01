package com.fluxer.client.data.remote

import com.fluxer.client.data.local.SecureCookieStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

/**
 * Authenticator that handles 401 Unauthorized responses.
 * Attempts to refresh the session or triggers re-authentication.
 */
class AuthAuthenticator(
    private val cookieStorage: SecureCookieStorage,
    private val tokenRefreshHandler: TokenRefreshHandler? = null
) : Authenticator {

    private val refreshMutex = Mutex()
    
    @Volatile
    private var isRefreshing = false

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only attempt refresh once per 401
        if (responseCount(response) >= 2) {
            Timber.w("🚫 Authentication failed after retry, giving up")
            return null
        }

        // Check if we have a session to refresh
        if (!cookieStorage.hasValidSession()) {
            Timber.w("🚫 No session available for refresh")
            return null
        }

        // Attempt to refresh the session
        return runBlocking {
            refreshMutex.withLock {
                if (isRefreshing) {
                    // Wait for ongoing refresh and retry with new token
                    Timber.d("⏳ Waiting for ongoing token refresh...")
                    // Return original request - it will be retried
                    return@withLock null
                }

                isRefreshing = true
                try {
                    val refreshed = attemptTokenRefresh()
                    
                    if (refreshed) {
                        Timber.i("✅ Token refreshed successfully, retrying request")
                        // Retry the original request with new cookies
                        response.request.newBuilder().build()
                    } else {
                        Timber.e("❌ Token refresh failed")
                        tokenRefreshHandler?.onTokenRefreshFailed()
                        null
                    }
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    /**
     * Attempt to refresh the authentication token/session
     */
    private suspend fun attemptTokenRefresh(): Boolean {
        return try {
            // Fluxer typically uses a /api/auth/refresh endpoint
            // The cookie jar will automatically handle the response cookies
            tokenRefreshHandler?.refreshToken() ?: false
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed with exception")
            false
        }
    }

    /**
     * Count how many times we've attempted this request
     */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

/**
 * Interface for handling token refresh logic
 */
interface TokenRefreshHandler {
    suspend fun refreshToken(): Boolean
    fun onTokenRefreshFailed()
}
