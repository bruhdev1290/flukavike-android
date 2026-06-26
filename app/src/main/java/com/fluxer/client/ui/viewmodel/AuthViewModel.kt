package com.fluxer.client.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.BuildConfig
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.data.remote.WebAuthnResult
import com.fluxer.client.data.remote.WebAuthnService
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.AuthRepository.AuthState
import com.fluxer.client.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val instanceConfigStore: InstanceConfigStore,
    private val webAuthnService: WebAuthnService
) : ViewModel() {

    companion object {
        private val PASSKEY_ENABLED_PACKAGES = setOf("com.fluxer", "com.fluxer.canary")
        private const val PASSKEY_UNSUPPORTED_MESSAGE =
            "Passkey sign-in is not available in this Android build yet. Fluxer only trusts its official Android app package for passkeys."
    }

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

    private val _mfaChallenge = MutableStateFlow<MfaChallengeUi?>(null)
    val mfaChallenge: StateFlow<MfaChallengeUi?> = _mfaChallenge.asStateFlow()

    private val _isMfaLoading = MutableStateFlow(false)
    val isMfaLoading: StateFlow<Boolean> = _isMfaLoading.asStateFlow()

    val isPasskeySupportedInThisBuild: Boolean =
        BuildConfig.APPLICATION_ID in PASSKEY_ENABLED_PACKAGES

    private var sessionRestoreAttempted = false

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Loading && !sessionRestoreAttempted) {
                    sessionRestoreAttempted = true
                    _isLoading.value = true
                    authRepository.validateSession()
                        .onSuccess {
                            _navigateToChat.emit(Unit)
                        }
                        .onError {
                            Timber.w("Session validation failed: $it")
                        }
                    _isLoading.value = false
                } else if (state !is AuthState.Loading) {
                    sessionRestoreAttempted = false
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            _mfaChallenge.value = null
            
            when (val result = authRepository.login(email, password, _captchaToken.value)) {
                is AuthRepository.LoginResult.Success -> {
                    _isLoading.value = false
                    _navigateToChat.emit(Unit)
                    resetCaptchaState()
                }
                is AuthRepository.LoginResult.CaptchaRequired -> {
                    _isLoading.value = false
                    authRepository.exitAuthChallenge()
                    _captchaRequired.value = true
                    _captchaSiteKey.value = result.sitekey?.takeIf { it.isNotBlank() } ?: BuildConfig.HCAPTCHA_SITE_KEY
                    val resolved = result.provider?.trim()?.lowercase() ?: "hcaptcha"
                    _captchaProvider.value = if (resolved.contains("turnstile")) "turnstile" else "hcaptcha"
                    _loginError.value = "Please complete the verification below."
                }
                is AuthRepository.LoginResult.IpAuthorizationRequired -> {
                    _isLoading.value = false
                    authRepository.exitAuthChallenge()
                    resetCaptchaState()
                    _loginError.value = if (!result.email.isNullOrBlank()) {
                        "Check ${result.email} and approve this login attempt."
                    } else {
                        "Approve this login attempt from your email, then try again."
                    }
                }
                is AuthRepository.LoginResult.MfaRequired -> {
                    _isLoading.value = false
                    resetCaptchaState()
                    _mfaChallenge.value = MfaChallengeUi(
                        ticket = result.ticket,
                        totp = result.totp,
                        sms = result.sms,
                        webauthn = result.webauthn,
                        smsPhoneHint = result.smsPhoneHint
                    )
                    _loginError.value = null
                }
                is AuthRepository.LoginResult.Error -> {
                    _isLoading.value = false
                    _loginError.value = result.message
                    resetCaptchaState()
                }
            }
        }
    }

    fun completeMfaWithPasskey(context: Context) {
        val challenge = _mfaChallenge.value ?: run {
            _loginError.value = "No active two-factor challenge."
            return
        }
        if (!challenge.webauthn) {
            _loginError.value = "Passkey verification is not available for this challenge."
            return
        }
        if (!isPasskeySupportedInThisBuild) {
            _loginError.value = PASSKEY_UNSUPPORTED_MESSAGE
            return
        }

        viewModelScope.launch {
            _isMfaLoading.value = true
            _loginError.value = null

            when (val options = authRepository.getMfaWebAuthnOptions(challenge.ticket)) {
                is Result.Success -> {
                    when (val credential = webAuthnService.authenticate(context, options.data)) {
                        is WebAuthnResult.Success -> {
                            when (val verified = authRepository.verifyMfaWebAuthn(
                                ticket = challenge.ticket,
                                challenge = credential.challenge,
                                credentialResponse = credential.credentialResponse
                            )) {
                                is Result.Success -> {
                                    _mfaChallenge.value = null
                                    _isMfaLoading.value = false
                                    _navigateToChat.emit(Unit)
                                }
                                is Result.Error -> {
                                    _isMfaLoading.value = false
                                    _loginError.value = verified.message
                                }
                                Result.Loading -> Unit
                            }
                        }
                        WebAuthnResult.Cancelled -> {
                            _isMfaLoading.value = false
                        }
                        is WebAuthnResult.Error -> {
                            _isMfaLoading.value = false
                            _loginError.value = credential.message
                        }
                    }
                }
                is Result.Error -> {
                    _isMfaLoading.value = false
                    _loginError.value = options.message
                }
                Result.Loading -> Unit
            }
        }
    }

    fun cancelMfaChallenge() {
        _mfaChallenge.value = null
        _isMfaLoading.value = false
        _loginError.value = null
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

    data class MfaChallengeUi(
        val ticket: String,
        val totp: Boolean,
        val sms: Boolean,
        val webauthn: Boolean,
        val smsPhoneHint: String?
    ) {
        val hasCodeMethod: Boolean = totp || sms
    }

    fun passkeyUnavailableMessage(): String = PASSKEY_UNSUPPORTED_MESSAGE
}
