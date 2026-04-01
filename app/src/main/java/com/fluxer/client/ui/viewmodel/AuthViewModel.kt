package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.AuthRepository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _navigateToChat = MutableSharedFlow<Unit>()
    val navigateToChat: SharedFlow<Unit> = _navigateToChat.asSharedFlow()

    init {
        // Check for existing session on startup
        viewModelScope.launch {
            if (authRepository.sessionCookieFlow.value != null) {
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
            
            authRepository.login(email, password)
                .onSuccess {
                    _navigateToChat.emit(Unit)
                }
                .onError { error ->
                    _loginError.value = error
                }
            
            _isLoading.value = false
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
