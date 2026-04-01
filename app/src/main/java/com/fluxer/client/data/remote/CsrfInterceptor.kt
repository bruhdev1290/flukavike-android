package com.fluxer.client.data.remote

import com.fluxer.client.data.local.SecureCookieStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Interceptor that handles CSRF token extraction and injection.
 * Fluxer uses Double Submit Cookie pattern for CSRF protection.
 */
class CsrfInterceptor(
    private val cookieStorage: SecureCookieStorage
) : Interceptor {

    @Volatile
    private var cachedCsrfToken: String? = null
    
    private val csrfLock = Object()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        // Add CSRF token to state-changing requests
        if (requiresCsrf(request.method)) {
            val csrfToken = getCsrfToken()
            
            csrfToken?.let { token ->
                // Add to header - Fluxer expects it in X-CSRF-Token header
                builder.header(CSRF_HEADER, token)
                Timber.d("Added CSRF token to ${request.method} ${request.url.encodedPath}")
            } ?: run {
                Timber.w("No CSRF token available for ${request.method} request!")
            }
        }

        // Add common headers
        builder.header("Accept", "application/json")
        builder.header("X-Requested-With", "FluxerAndroidClient")

        val response = chain.proceed(builder.build())

        // Extract CSRF token from response if present
        extractCsrfToken(response)

        return response
    }

    /**
     * Get current CSRF token, fetching a fresh one if needed
     */
    private fun getCsrfToken(): String? {
        cachedCsrfToken?.let { return it }
        
        // Try to extract from cookies
        return synchronized(csrfLock) {
            cachedCsrfToken ?: runBlocking {
                fetchCsrfTokenFromCookies()
            }.also { 
                cachedCsrfToken = it 
            }
        }
    }

    /**
     * Extract CSRF token from cookies or response headers
     */
    private fun extractCsrfToken(response: Response) {
        // Check for CSRF token in Set-Cookie header
        val cookies = response.headers("Set-Cookie")
        cookies.forEach { cookieStr ->
            when {
                cookieStr.contains(CSRF_COOKIE_NAME) -> {
                    val token = extractCookieValue(cookieStr, CSRF_COOKIE_NAME)
                    token?.let {
                        synchronized(csrfLock) {
                            cachedCsrfToken = it
                            Timber.d("Extracted CSRF token from cookie")
                        }
                    }
                }
                cookieStr.contains(CSRF_COOKIE_NAME_ALT) -> {
                    val token = extractCookieValue(cookieStr, CSRF_COOKIE_NAME_ALT)
                    token?.let {
                        synchronized(csrfLock) {
                            cachedCsrfToken = it
                            Timber.d("Extracted CSRF token from alternative cookie")
                        }
                    }
                }
            }
        }

        // Also check for token in response header
        response.header(CSRF_HEADER)?.let { token ->
            synchronized(csrfLock) {
                cachedCsrfToken = token
                Timber.d("Extracted CSRF token from header")
            }
        }
    }

    /**
     * Fetch CSRF token from the cookie storage
     */
    private fun fetchCsrfTokenFromCookies(): String? {
        // This would typically require a request to /api/csrf or similar endpoint
        // For now, we rely on the initial login/auth request to set the CSRF cookie
        return cachedCsrfToken
    }

    /**
     * Reset cached CSRF token (call on logout)
     */
    fun clearCsrfToken() {
        synchronized(csrfLock) {
            cachedCsrfToken = null
            Timber.d("CSRF token cleared")
        }
    }

    private fun extractCookieValue(cookieString: String, name: String): String? {
        val pattern = "$name=([^;]+)".toRegex()
        return pattern.find(cookieString)?.groupValues?.get(1)
    }

    private fun requiresCsrf(method: String): Boolean {
        // Only state-changing methods need CSRF protection
        return method.equals("POST", ignoreCase = true) ||
               method.equals("PUT", ignoreCase = true) ||
               method.equals("DELETE", ignoreCase = true) ||
               method.equals("PATCH", ignoreCase = true)
    }

    companion object {
        private const val CSRF_HEADER = "X-CSRF-Token"
        private const val CSRF_COOKIE_NAME = "fluxer_csrf"
        private const val CSRF_COOKIE_NAME_ALT = "XSRF-TOKEN"
    }
}
