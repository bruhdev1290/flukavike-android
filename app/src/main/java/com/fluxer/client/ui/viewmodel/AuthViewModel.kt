package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.BuildConfig
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.AuthRepository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val instanceConfigStore: InstanceConfigStore
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _customInstanceUrl = MutableStateFlow(instanceConfigStore.getCustomBaseUrl() ?: "")
    val customInstanceUrl: StateFlow<String> = _customInstanceUrl.asStateFlow()

    private val _activeInstanceBaseUrl = MutableStateFlow(instanceConfigStore.getActiveBaseUrl())
    val activeInstanceBaseUrl: StateFlow<String> = _activeInstanceBaseUrl.asStateFlow()

    private val _instanceMessage = MutableStateFlow<String?>(null)
    val instanceMessage: StateFlow<String?> = _instanceMessage.asStateFlow()

    private val _navigateToChat = MutableSharedFlow<Unit>()
    val navigateToChat: SharedFlow<Unit> = _navigateToChat.asSharedFlow()

    // Captcha state
    private val _captchaRequired = MutableStateFlow(false)
    val captchaRequired: StateFlow<Boolean> = _captchaRequired.asStateFlow()

    private val _captchaSiteKey = MutableStateFlow("")
    val captchaSiteKey: StateFlow<String> = _captchaSiteKey.asStateFlow()

    private val _captchaProvider = MutableStateFlow("hcaptcha")
    val captchaProvider: StateFlow<String> = _captchaProvider.asStateFlow()

    private val _captchaToken = MutableStateFlow<String?>(null)
    val captchaToken: StateFlow<String?> = _captchaToken.asStateFlow()

    init {
        // Check for existing session on startup
        viewModelScope.launch {
            if (authRepository.sessionCookieFlow.firstOrNull() != null) {
                _isLoading.value = true
                authRepository.validateSession()
                    .onSuccess { 
                        _navigateToChat.emit(Unit)
                    }
                    .onError { 
                        Timber.w("Session validation failed: $it")
                    }
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            
            when (val result = authRepository.login(email, password, _captchaToken.value)) {
                is AuthRepository.LoginResult.Success -> {
                    _isLoading.value = false
                    _navigateToChat.emit(Unit)
                    resetCaptchaState()
                }
                is AuthRepository.LoginResult.CaptchaRequired -> {
                    _isLoading.value = false
                    _captchaRequired.value = true
                    _captchaSiteKey.value = result.sitekey?.takeIf { it.isNotBlank() } ?: BuildConfig.HCAPTCHA_SITE_KEY
                    val resolved = result.provider?.trim()?.lowercase() ?: "hcaptcha"
                    _captchaProvider.value = if (resolved.contains("turnstile")) "turnstile" else "hcaptcha"
                    _loginError.value = "Please complete the verification below."
                }
                is AuthRepository.LoginResult.IpAuthorizationRequired -> {
                    _isLoading.value = false
                    resetCaptchaState()
                    _loginError.value = if (!result.email.isNullOrBlank()) {
                        "Check ${result.email} and approve this login attempt."
                    } else {
                        "Approve this login attempt from your email, then try again."
                    }
                }
                is AuthRepository.LoginResult.Error -> {
                    _isLoading.value = false
                    _loginError.value = result.message
                    resetCaptchaState()
                }
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        if (!validateRegisterInput(email, username, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            
            authRepository.register(email, username, password)
                .onSuccess {
                    _navigateToChat.emit(Unit)
                }
                .onError { error ->
                    _loginError.value = error
                }
            
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearError() {
        _loginError.value = null
    }

    fun clearInstanceMessage() {
        _instanceMessage.value = null
    }

    fun applyCustomInstance(rawInput: String) {
        val appliedBaseUrl = instanceConfigStore.saveCustomBaseUrl(rawInput)
        if (appliedBaseUrl == null) {
            _loginError.value = "Invalid instance URL. Example: https://web.fluxer.app"
            return
        }

        authRepository.onInstanceChanged()
        _customInstanceUrl.value = instanceConfigStore.getCustomBaseUrl() ?: ""
        _activeInstanceBaseUrl.value = appliedBaseUrl
        _instanceMessage.value = if (_customInstanceUrl.value.isBlank()) {
            "Using default instance"
        } else {
            "Custom instance applied"
        }
        _loginError.value = null
    }

    fun onCaptchaToken(token: String) {
        _captchaToken.value = token
        _loginError.value = null
    }

    fun resetCaptchaState() {
        _captchaRequired.value = false
        _captchaSiteKey.value = ""
        _captchaProvider.value = "hcaptcha"
        _captchaToken.value = null
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _loginError.value = "Email is required"
            return false
        }
        if (password.isBlank()) {
            _loginError.value = "Password is required"
            return false
        }
        return true
    }

    private fun validateRegisterInput(email: String, username: String, password: String): Boolean {
        if (email.isBlank()) {
            _loginError.value = "Email is required"
            return false
        }
        if (username.isBlank()) {
            _loginError.value = "Username is required"
            return false
        }
        if (password.length < 8) {
            _loginError.value = "Password must be at least 8 characters"
            return false
        }
        return true
    }
}
