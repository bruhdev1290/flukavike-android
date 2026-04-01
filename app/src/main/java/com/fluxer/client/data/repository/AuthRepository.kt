package com.fluxer.client.data.repository

import com.fluxer.client.data.local.SecureCookieStorage
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.model.*
import com.fluxer.client.data.remote.*
import com.fluxer.client.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val gatewayManager: GatewayWebSocketManager
) : TokenRefreshHandler {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val sessionCookieFlow: Flow<String?> = cookieStorage.sessionCookieFlow

    init {
        // Wire the authenticator back to this repository so 401 refreshes can call refreshToken().
        authenticator.setTokenRefreshHandler(this)

        // Check for existing session on init
        checkExistingSession()
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            Timber.i("🔐 Attempting login for: $email")
            _authState.value = AuthState.Loading
            
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.isSuccessful) {
                val authData = response.body()
                authData?.let {
                    _authState.value = AuthState.Authenticated(it.user)
                    Timber.i("✅ Login successful for: ${it.user.username}")
                    
                    // Connect to Gateway after successful login
                    gatewayManager.connect()
                    
                    Result.Success(Unit)
                } ?: Result.Error("Empty response body")
            } else {
                val error = parseError(response.code(), response.errorBody()?.string())
                _authState.value = AuthState.Error(error)
                Timber.e("❌ Login failed: $error")
                Result.Error(error)
            }
        } catch (e: HttpException) {
            val error = "HTTP ${e.code()}: ${e.message()}"
            _authState.value = AuthState.Error(error)
            Result.Error(error)
        } catch (e: IOException) {
            val error = if (e is UnknownHostException) {
                "Cannot reach instance: ${instanceConfigStore.getActiveBaseUrl()}"
            } else {
                "Network error: ${e.message}"
            }
            _authState.value = AuthState.Error(error)
            Result.Error(error)
        } catch (e: Exception) {
            val error = "Unexpected error: ${e.message}"
            _authState.value = AuthState.Error(error)
            Result.Error(error)
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
                    _authState.value = AuthState.Authenticated(it.user)
                    Timber.i("✅ Registration successful for: ${it.user.username}")
                    
                    // Connect to Gateway after successful registration
                    gatewayManager.connect()
                    
                    Result.Success(Unit)
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
            val response = apiService.getCurrentUser()
            
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
        _authState.value = AuthState.Unauthenticated
        Timber.i("🧹 Session cleared")
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
        data class Authenticated(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
