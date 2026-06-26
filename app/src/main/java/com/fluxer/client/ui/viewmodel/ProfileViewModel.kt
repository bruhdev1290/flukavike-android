package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.fluxer.client.data.model.UpdateProfileRequest
import com.fluxer.client.data.model.UserProfile
import com.fluxer.client.data.model.toUserProfile
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.FriendsRepository
import com.fluxer.client.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCurrentUser = MutableStateFlow(false)
    val isCurrentUser: StateFlow<Boolean> = _isCurrentUser.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _relationshipType = MutableStateFlow<Int?>(null)
    val relationshipType: StateFlow<Int?> = _relationshipType.asStateFlow()

    fun loadProfile(userId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val currentUser = (authRepository.authState.value as? AuthRepository.AuthState.Authenticated)?.user
            _isCurrentUser.value = userId == null || userId == currentUser?.id
            
            val targetUserId = userId ?: currentUser?.id
            
            targetUserId?.let { id ->
                val result = if (_isCurrentUser.value) {
                    profileRepository.getCurrentUserProfile(id)
                } else {
                    profileRepository.getUserProfile(id)
                }
                
                result.onSuccess { profile ->
                    _profile.value = profile
                    _error.value = null
                }.onError { error ->
                    Timber.e("Failed to load profile: $error")
                    _error.value = error
                    if (_isCurrentUser.value && currentUser != null) {
                        _profile.value = currentUser.toUserProfile()
                    }
                }
                if (!_isCurrentUser.value) {
                    loadRelationship(id)
                } else {
                    _relationshipType.value = null
                }
            } ?: run {
                _profile.value = currentUser?.toUserProfile()
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
                _error.value = error
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            profileRepository.updateAvatar(uri).onSuccess { user ->
                _profile.value = _profile.value?.copy(avatarUrl = user.avatarUrl)
            }.onError { error ->
                Timber.e("Avatar upload failed: $error")
                _error.value = error
            }
        }
    }

    fun addFriendByUsername(username: String) {
        viewModelScope.launch {
            friendsRepository.addFriendByUsername(username)
                .onSuccess { _relationshipType.value = 4 }
                .onError { error -> _error.value = error }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            friendsRepository.blockUser(userId)
                .onSuccess { _relationshipType.value = 2 }
                .onError { error -> _error.value = error }
        }
    }

    fun removeRelationship(userId: String) {
        viewModelScope.launch {
            friendsRepository.removeRelationship(userId)
                .onSuccess { _relationshipType.value = null }
                .onError { error -> _error.value = error }
        }
    }

    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            friendsRepository.acceptFriendRequest(userId)
                .onSuccess { _relationshipType.value = 1 }
                .onError { error -> _error.value = error }
        }
    }

    fun clearError() {
        _error.value = null
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

    private fun loadRelationship(userId: String) {
        viewModelScope.launch {
            friendsRepository.getRelationships()
                .onSuccess { relationships ->
                    _relationshipType.value = relationships.firstOrNull { it.user.id == userId }?.type
                }
                .onError { error -> Timber.w("Failed to load relationship state: $error") }
        }
    }
}
