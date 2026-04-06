package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.UpdateProfileRequest
import com.fluxer.client.data.model.UserProfile
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCurrentUser = MutableStateFlow(false)
    val isCurrentUser: StateFlow<Boolean> = _isCurrentUser.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    fun loadProfile(userId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val currentUser = (authRepository.authState.value as? AuthRepository.AuthState.Authenticated)?.user
            _isCurrentUser.value = userId == null || userId == currentUser?.id
            
            val targetUserId = userId ?: currentUser?.id
            
            targetUserId?.let { id ->
                val result = if (_isCurrentUser.value) {
                    profileRepository.getCurrentUserProfile()
                } else {
                    profileRepository.getUserProfile(id)
                }
                
                result.onSuccess { profile ->
                    _profile.value = profile
                }.onError { error ->
                    Timber.e("Failed to load profile: $error")
                }
            }
            
            _isLoading.value = false
        }
    }

    fun updateProfile(displayName: String?, bio: String?, customStatus: String?) {
        viewModelScope.launch {
            val request = UpdateProfileRequest(
                displayName = displayName,
                bio = bio,
                customStatus = customStatus
            )
            
            val result = profileRepository.updateProfile(request)
            result.onSuccess { profile ->
                _profile.value = profile
            }.onError { error ->
                Timber.e("Failed to update profile: $error")
            }
        }
    }

    fun showEditDialog() {
        _showEditDialog.value = true
    }

    fun hideEditDialog() {
        _showEditDialog.value = false
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
