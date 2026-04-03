package com.fluxer.client.data.repository

import com.fluxer.client.BuildConfig
import com.fluxer.client.data.local.SecureCookieStorage
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.*
import com.fluxer.client.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations.
 * Handles login, registration, logout, and session management.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: FluxerApiService,
    private val cookieStorage: SecureCookieStorage,
    private val instanceConfigStore: InstanceConfigStore,
    private val csrfInterceptor: CsrfInterceptor,
    private val authenticator: AuthAuthenticator,
    private val gatewayManager: GatewayWebSocketManager,
    private val authTokenStorage: com.fluxer.client.data.local.AuthTokenStorage
) : TokenRefreshHandler {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val sessionCookieFlow: Flow<String?> = cookieStorage.sessionCookieFlow

    private var discoveredCaptchaConfig: InstanceConfig.CaptchaConfig? = null
    private var authToken: String? = null

    private val jsonParser = Json { ignoreUnknownKeys = true }

    init {
        // Wire the authenticator back to this repository so 401 refreshes can call refreshToken().
        authenticator.setTokenRefreshHandler(this)

        // Check for existing session on init
        checkExistingSession()
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String, captchaToken: String? = null): LoginResult {
        return try {
            Timber.i("🔐 Attempting login for: $email")
            _authState.value = AuthState.Loading

            // Discover instance endpoints and captcha config before login
            runDiscovery()

            val captchaType = discoveredCaptchaConfig?.resolvedProvider() ?: "hcaptcha"
            val response = apiService.login(
                request = LoginRequest(email, password, captchaKey = captchaToken),
                captchaToken = captchaToken,
                captchaType = captchaType
            )

            if (response.isSuccessful) {
                val authData = response.body()
                Timber.d("🔐 Login HTTP ${response.code()}, body null=${authData == null}, token=${authData?.resolvedToken()?.take(8)}, user=${authData?.user?.username}")

                // Persist token if present (some instances omit it and rely on cookies)
                authData?.resolvedToken()?.let { token ->
                    authToken = token
                    authTokenStorage.setToken(token)
                }

                // Get user from body, or fall back to /api/auth/me (handles empty-body 200 responses)
                val resolvedUser = authData?.user ?: run {
                    Timber.d("Login response missing user, fetching /api/auth/me (authToken=${authToken?.take(8)})")
                    val userResponse = apiService.getCurrentUser(authToken = authToken)
                    Timber.d("/api/auth/me HTTP ${userResponse.code()}, body null=${userResponse.body() == null}")
                    if (userResponse.isSuccessful) userResponse.body() else null
                }

                if (resolvedUser != null) {
                    _authState.value = AuthState.Authenticated(resolvedUser)
                    Timber.i("✅ Login successful for: ${resolvedUser.username}")

                    // Connect to Gateway after successful login
                    gatewayManager.connect()

                    LoginResult.Success
                } else {
                    LoginResult.Error("Empty response body")
                }
            } else {
                val result = parseLoginError(response.code(), response.errorBody()?.string())
                if (result is LoginResult.Error) {
                    _authState.value = AuthState.Error(result.message)
                    Timber.e("❌ Login failed: ${result.message}")
                }
                result
            }
        } catch (e: HttpException) {
            val result = parseLoginError(e.code(), e.response()?.errorBody()?.string())
            if (result is LoginResult.Error) {
                _authState.value = AuthState.Error(result.message)
            }
            result
        } catch (e: IOException) {
            val error = if (e is UnknownHostException) {
                "Cannot reach instance: ${instanceConfigStore.getActiveBaseUrl()}"
            } else {
                "Network error: ${e.message}"
            }
            _authState.value = AuthState.Error(error)
            LoginResult.Error(error)
        } catch (e: Exception) {
            val error = "Unexpected error: ${e.message}"
            _authState.value = AuthState.Error(error)
            LoginResult.Error(error)
        }
    }

    /**
     * Register new account
     */
    suspend fun register(email: String, username: String, password: String): Result<Unit> {
        return try {
            Timber.i("📝 Attempting registration for: $email")
            _authState.value = AuthState.Loading

            val response = apiService.register(RegisterRequest(email, username, password))

            if (response.isSuccessful) {
                val authData = response.body()
                authData?.let {
                    // Persist token for REST API auth header
                    it.resolvedToken()?.let { token ->
                        authToken = token
                        authTokenStorage.setToken(token)
                    }
                    val resolvedUser = it.user ?: run {
                        Timber.d("Registration response missing user, fetching /api/auth/me")
                        val userResponse = apiService.getCurrentUser(authToken = authToken)
                        if (userResponse.isSuccessful) userResponse.body() else null
                    }
                    resolvedUser?.let { user ->
                        _authState.value = AuthState.Authenticated(user)
                        Timber.i("✅ Registration successful for: ${user.username}")

                        // Connect to Gateway after successful registration
                        gatewayManager.connect()

                        Result.Success(Unit)
                    } ?: Result.Error("Empty response body")
                } ?: Result.Error("Empty response body")
            } else {
                val error = parseError(response.code(), response.errorBody()?.string())
                _authState.value = AuthState.Error(error)
                Result.Error(error)
            }
        } catch (e: IOException) {
            val error = if (e is UnknownHostException) {
                "Cannot reach instance: ${instanceConfigStore.getActiveBaseUrl()}"
            } else {
                "Registration failed: network error"
            }
            _authState.value = AuthState.Error(error)
            Result.Error(error)
        } catch (e: Exception) {
            val error = "Registration failed: ${e.message}"
            _authState.value = AuthState.Error(error)
            Result.Error(error)
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit> {
        return try {
            Timber.i("👋 Logging out")

            // Disconnect from Gateway first
            gatewayManager.disconnect()

            // Call logout endpoint (cookies will be cleared server-side)
            apiService.logout()

            // Clear local data
            clearSession()

            Result.Success(Unit)
        } catch (e: Exception) {
            // Still clear local session even if server call fails
            clearSession()
            Result.Success(Unit) // Logout is successful from client perspective
        }
    }

    /**
     * Refresh the current session
     */
    override suspend fun refreshToken(): Boolean {
        return try {
            Timber.i("🔄 Refreshing session token")
            val response = apiService.refreshToken()

            if (response.isSuccessful) {
                Timber.i("✅ Token refreshed successfully")
                true
            } else {
                Timber.e("❌ Token refresh failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh exception")
            false
        }
    }

    override fun onTokenRefreshFailed() {
        Timber.e("🚫 Token refresh failed, logging out")
        // Clear session and require re-login
        clearSession()
    }

    fun onInstanceChanged() {
        gatewayManager.disconnect()
        clearSession()
        discoveredCaptchaConfig = null
    }

    /**
     * Check for existing valid session
     */
    private fun checkExistingSession() {
        if (cookieStorage.hasValidSession()) {
            Timber.i("🔍 Found existing session, validating...")
            _authState.value = AuthState.Loading

            // Try to fetch current user to validate session
            // This would typically be a suspend function, handle properly in ViewModel
            _authState.value = AuthState.Unauthenticated // Will be updated by validateSession
        }
    }

    /**
     * Validate and restore existing session
     */
    suspend fun validateSession(): Result<Unit> {
        return try {
            val response = apiService.getCurrentUser(authToken = authToken)

            if (response.isSuccessful) {
                val user = response.body()
                user?.let {
                    _authState.value = AuthState.Authenticated(it)
                    // Reconnect to Gateway
                    gatewayManager.connect()
                    Result.Success(Unit)
                } ?: Result.Error("Empty user data")
            } else {
                // Session invalid
                clearSession()
                Result.Error("Session expired")
            }
        } catch (e: Exception) {
            Result.Error("Failed to validate session: ${e.message}")
        }
    }

    /**
     * Clear all session data
     */
    private fun clearSession() {
        cookieStorage.clearAllCookies()
        csrfInterceptor.clearCsrfToken()
        authTokenStorage.clear()
        authToken = null
        _authState.value = AuthState.Unauthenticated
        Timber.i("🧹 Session cleared")
    }

    private suspend fun runDiscovery() {
        try {
            val response = apiService.discoverInstance()
            if (response.isSuccessful) {
                response.body()?.let { config ->
                    discoveredCaptchaConfig = config.captcha
                    config.resolvedGateway().takeIf { it.isNotBlank() }?.let {
                        instanceConfigStore.saveDiscoveredWebSocketUrl(it)
                    }
                    Timber.d("Instance discovered. API: ${config.resolvedApi()}, Gateway: ${config.resolvedGateway()}, Captcha: ${config.captcha != null}")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Instance discovery failed, continuing with defaults")
        }
    }

    private fun parseLoginError(code: Int, errorBody: String?): LoginResult {
        val raw = errorBody ?: return LoginResult.Error(parseError(code, null))
        return try {
            val err = jsonParser.decodeFromString(CaptchaRequiredResponse.serializer(), raw)
            when {
                err.error == "captcha_required" -> {
                    val sitekey = err.sitekey?.takeIf { it.isNotBlank() }
                        ?: discoveredCaptchaConfig?.resolvedSitekey()
                        ?: BuildConfig.HCAPTCHA_SITE_KEY
                    val provider = err.provider?.trim()?.lowercase()
                        ?: discoveredCaptchaConfig?.resolvedProvider()
                        ?: "hcaptcha"
                    LoginResult.CaptchaRequired(sitekey, provider)
                }
                else -> LoginResult.Error(parseError(code, raw))
            }
        } catch (e: Exception) {
            // Try to parse as IP authorization error first
            try {
                val ipErr = jsonParser.decodeFromString(IpAuthRequiredResponse.serializer(), raw)
                if (ipErr.code == "IP_AUTHORIZATION_REQUIRED" || ipErr.ipAuthorizationRequired == true) {
                    return LoginResult.IpAuthorizationRequired(ipErr.ticket, ipErr.email, ipErr.resendAvailableIn)
                }
            } catch (_: Exception) { }

            val lower = raw.lowercase()
            val normalized = lower.filter { it.isLetterOrDigit() }
            if (lower.contains("captcha") || lower.contains("api error 7") || normalized.contains("apierror7")) {
                val sitekey = discoveredCaptchaConfig?.resolvedSitekey() ?: BuildConfig.HCAPTCHA_SITE_KEY
                val provider = discoveredCaptchaConfig?.resolvedProvider() ?: "hcaptcha"
                LoginResult.CaptchaRequired(sitekey, provider)
            } else {
                LoginResult.Error(parseError(code, raw))
            }
        }
    }

    private fun parseError(code: Int, errorBody: String?): String {
        return when (code) {
            401 -> "Invalid email or password"
            403 -> "Access denied. Verify credentials and instance URL (${instanceConfigStore.getActiveBaseUrl()})."
            429 -> "Too many requests, please try again later"
            in 500..599 -> "Server error, please try again later"
            else -> errorBody ?: "Unknown error (code: $code)"
        }
    }

    sealed class AuthState {
        object Unauthenticated : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: User?) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    sealed class LoginResult {
        object Success : LoginResult()
        data class CaptchaRequired(val sitekey: String?, val provider: String?) : LoginResult()
        data class IpAuthorizationRequired(val ticket: String?, val email: String?, val resendAvailableIn: Int?) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
