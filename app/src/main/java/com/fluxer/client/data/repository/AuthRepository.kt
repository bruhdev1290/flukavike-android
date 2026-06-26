// =============================================================================
// !! DO NOT TOUCH THIS FILE !!
// Auth was broken for a long time and is now stable. Key fixes:
// - runDiscovery() runs in background at startup, NOT inside login()
// - callTimeout(20s) in NetworkModule is the only reliable timeout guard
// - Interceptor order in NetworkModule is load-bearing
// See CLAUDE.md for full details.
// =============================================================================
package com.fluxer.client.data.repository

import com.fluxer.client.BuildConfig
import com.fluxer.client.data.local.dao.AuthSessionDao
import com.fluxer.client.data.local.model.AuthSessionEntity
import com.fluxer.client.data.local.SecureCookieStorage
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.*
import com.fluxer.client.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
    private val authTokenStorage: com.fluxer.client.data.local.AuthTokenStorage,
    private val authSessionDao: AuthSessionDao
) : TokenRefreshHandler {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val sessionCookieFlow: Flow<String?> = cookieStorage.sessionCookieFlow

    private var discoveredCaptchaConfig: InstanceConfig.CaptchaConfig? = null
    private var authToken: String? = null

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Wire the authenticator back to this repository so 401 refreshes can call refreshToken().
        authenticator.setTokenRefreshHandler(this)

        // Check for existing session on init
        checkExistingSession()

        // Kick off instance discovery in the background immediately — don't block login on it
        repoScope.launch { runDiscovery() }
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String, captchaToken: String? = null): LoginResult {
        return try {
            Timber.i("🔐 Attempting login for: $email")
            _authState.value = AuthState.Loading

            val captchaType = discoveredCaptchaConfig?.resolvedProvider() ?: "hcaptcha"
            val response = apiService.login(
                request = LoginRequest(email, password, captchaKey = captchaToken),
                captchaToken = captchaToken,
                captchaType = captchaType
            )

            if (response.isSuccessful) {
                val authData = response.body()
                Timber.d("🔐 Login HTTP ${response.code()}, body null=${authData == null}, token=${authData?.resolvedToken()?.take(8)}, user=${authData?.user?.username}, mfa=${authData?.mfa}")

                val newToken = authData?.resolvedToken()
                authToken = newToken

                if (authData?.mfa == true && !authData.ticket.isNullOrBlank()) {
                    _authState.value = AuthState.Unauthenticated
                    val methods = authData.allowedMethods.map { it.trim().lowercase() }
                    return LoginResult.MfaRequired(
                        ticket = authData.ticket,
                        totp = authData.totp || "totp" in methods,
                        sms = authData.sms || "sms" in methods,
                        webauthn = authData.webauthn || "webauthn" in methods,
                        smsPhoneHint = authData.smsPhoneHint
                    )
                }

                val resolvedUser = authData?.user ?: run {
                    Timber.d("Login response missing user, fetching /api/users/@me (authToken=${authToken?.take(8)})")
                    val userResponse = apiService.getCurrentUser(authToken = authToken)
                    Timber.d("/api/users/@me HTTP ${userResponse.code()}, body null=${userResponse.body() == null}")
                    if (userResponse.isSuccessful) userResponse.body() else null
                }

                if (resolvedUser != null) {
                    saveSession(resolvedUser, newToken)
                    _authState.value = AuthState.Authenticated(resolvedUser)
                    Timber.i("✅ Login successful for: ${resolvedUser.username}")

                    // Connect to Gateway after successful login
                    gatewayManager.connect()

                    LoginResult.Success
                } else if (newToken != null || cookieStorage.hasValidSession()) {
                    if (!newToken.isNullOrBlank()) {
                        authTokenStorage.setToken(newToken)
                    }
                    _authState.value = AuthState.Authenticated(null)
                    Timber.i("✅ Login established a session without immediate user body")
                    gatewayManager.connect()
                    LoginResult.Success
                } else {
                    _authState.value = AuthState.Error("Empty response body")
                    LoginResult.Error("Empty response body")
                }
            } else {
                val result = parseLoginError(response.code(), response.errorBody()?.string())
                when (result) {
                    is LoginResult.Error -> {
                        _authState.value = AuthState.Error(result.message)
                        Timber.e("❌ Login failed: ${result.message}")
                    }
                    is LoginResult.CaptchaRequired,
                    is LoginResult.MfaRequired,
                    is LoginResult.IpAuthorizationRequired -> {
                        _authState.value = AuthState.Unauthenticated
                    }
                    LoginResult.Success -> Unit
                }
                result
            }
        } catch (e: HttpException) {
            val result = parseLoginError(e.code(), e.response()?.errorBody()?.string())
            when (result) {
                is LoginResult.Error -> _authState.value = AuthState.Error(result.message)
                is LoginResult.CaptchaRequired,
                is LoginResult.MfaRequired,
                is LoginResult.IpAuthorizationRequired -> _authState.value = AuthState.Unauthenticated
                LoginResult.Success -> Unit
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

    suspend fun getMfaWebAuthnOptions(ticket: String): Result<JsonObject> {
        if (ticket.isBlank()) {
            return Result.Error("Invalid MFA challenge")
        }

        return try {
            val url = buildWebAuthnMfaUrl("webauthn/authentication-options")
            Timber.d("Loading WebAuthn MFA options from $url")
            val response = apiService.getWebAuthnMfaOptions(url, MfaTicketRequest(ticket))
            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) }
                    ?: Result.Error("Empty WebAuthn options response")
            } else {
                Result.Error(parseError(response.code(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load WebAuthn MFA options")
            Result.Error("Failed to start passkey verification: ${e.message}")
        }
    }

    suspend fun verifyMfaWebAuthn(
        ticket: String,
        challenge: String,
        credentialResponse: JsonElement
    ): Result<Unit> {
        if (ticket.isBlank() || challenge.isBlank()) {
            return Result.Error("Invalid MFA challenge")
        }

        return try {
            val url = buildWebAuthnMfaUrl("webauthn")
            Timber.d("Verifying WebAuthn MFA via $url")
            val response = apiService.loginWithWebAuthnMfa(
                url = url,
                request = WebAuthnMfaRequest(
                    response = credentialResponse,
                    challenge = challenge,
                    ticket = ticket
                )
            )

            if (response.isSuccessful) {
                response.body()?.let { finalizeAuthResponse(it) }
                    ?: Result.Error("Empty MFA response body")
            } else {
                Result.Error(parseError(response.code(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Timber.e(e, "WebAuthn MFA verification failed")
            Result.Error("Passkey verification failed: ${e.message}")
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
                    }
                    val resolvedUser = it.user ?: run {
                        Timber.d("Registration response missing user, fetching /api/auth/me")
                        val userResponse = apiService.getCurrentUser(authToken = authToken)
                        if (userResponse.isSuccessful) userResponse.body() else null
                    }
                    resolvedUser?.let { user ->
                        saveSession(user, authToken)
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
                val body = response.body()
                val refreshedToken = body?.resolvedToken()
                if (!refreshedToken.isNullOrBlank()) {
                    val userId = body.user?.id ?: body.userId ?: authTokenStorage.activeUserId
                    if (userId != null) {
                        authTokenStorage.saveToken(userId, refreshedToken)
                    } else {
                        authTokenStorage.setToken(refreshedToken)
                    }
                    authToken = refreshedToken
                }
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
        repoScope.launch { runDiscovery() }
    }

    fun exitAuthChallenge() {
        if (_authState.value is AuthState.Loading) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Check for existing valid session
     */
    private fun checkExistingSession() {
        repoScope.launch {
            val active = authSessionDao.getActiveSession()
            if (active != null || cookieStorage.hasValidSession()) {
                Timber.i("🔍 Found existing session metadata, waiting for validation")
                active?.let {
                    authTokenStorage.setActiveUser(it.userId)
                    authToken = authTokenStorage.token
                }
                _authState.value = AuthState.Loading
            }
        }
    }

    /**
     * Validate and restore existing session
     */
    suspend fun validateSession(): Result<Unit> {
        return try {
            authSessionDao.getActiveSession()?.let {
                authTokenStorage.setActiveUser(it.userId)
                authToken = authTokenStorage.token
            }
            val response = apiService.getCurrentUser(authToken = authToken)

            if (response.isSuccessful) {
                val user = response.body()
                user?.let {
                    saveSession(it, authToken)
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
        val userId = authTokenStorage.activeUserId
        cookieStorage.clearAllCookies()
        csrfInterceptor.clearCsrfToken()
        if (userId != null) {
            authTokenStorage.deleteToken(userId)
            repoScope.launch { authSessionDao.delete(userId) }
        } else {
            authTokenStorage.clear()
        }
        authToken = null
        _authState.value = AuthState.Unauthenticated
        Timber.i("🧹 Session cleared")
    }

    private suspend fun runDiscovery() {
        try {
            withTimeout(6_000L) {
                val wellKnownUrl = instanceConfigStore.buildWellKnownUrl()
                val response = apiService.discoverInstance(wellKnownUrl)
                if (response.isSuccessful) {
                    response.body()?.let { config ->
                        discoveredCaptchaConfig = config.captcha
                        instanceConfigStore.saveDiscoveredConfig(config)
                        Timber.d("Instance discovered. API: ${config.resolvedApi()}, Gateway: ${config.resolvedGateway()}, Captcha: ${config.captcha != null}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w("Instance discovery failed (${e.javaClass.simpleName}), continuing with defaults")
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

    private suspend fun saveSession(user: User, token: String?) {
        authSessionDao.clearActive()
        if (!token.isNullOrBlank()) {
            authTokenStorage.saveToken(user.id, token)
            authToken = token
        } else {
            authTokenStorage.setActiveUser(user.id)
            authToken = authTokenStorage.token
        }
        authSessionDao.upsert(
            AuthSessionEntity(
                userId = user.id,
                active = true,
                username = user.username,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                instanceSnapshotJson = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun finalizeAuthResponse(authData: AuthResponse): Result<Unit> {
        val token = authData.resolvedToken()
        val user = authData.user ?: run {
            val userResponse = apiService.getCurrentUser(authToken = token)
            if (userResponse.isSuccessful) userResponse.body() else null
        }

        return if (user != null) {
            saveSession(user, token)
            _authState.value = AuthState.Authenticated(user)
            gatewayManager.connect()
            Result.Success(Unit)
        } else if (!token.isNullOrBlank()) {
            authData.userId?.let { authTokenStorage.saveToken(it, token) }
                ?: authTokenStorage.setToken(token)
            authToken = token
            _authState.value = AuthState.Authenticated(null)
            gatewayManager.connect()
            Result.Success(Unit)
        } else {
            Result.Error("Passkey verification returned no session")
        }
    }

    private fun buildWebAuthnMfaUrl(pathSuffix: String): String {
        val base = instanceConfigStore.getActiveApiBaseUrl().trimEnd('/')
        return "$base/auth/login/mfa/$pathSuffix"
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
        data class MfaRequired(
            val ticket: String,
            val totp: Boolean,
            val sms: Boolean,
            val webauthn: Boolean,
            val smsPhoneHint: String?
        ) : LoginResult()
        data class IpAuthorizationRequired(val ticket: String?, val email: String?, val resendAvailableIn: Int?) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}
