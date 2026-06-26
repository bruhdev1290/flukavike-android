package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Relationship
import com.fluxer.client.data.repository.FriendsRepository
import com.fluxer.client.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: FriendsRepository
) : ViewModel() {

    private val _relationships = MutableStateFlow<List<Relationship>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _addFriendInput = MutableStateFlow("")
    val addFriendInput: StateFlow<String> = _addFriendInput.asStateFlow()

    val friends: StateFlow<List<Relationship>> = _relationships
        .map { list -> list.filter { it.type == 1 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val pendingIncoming: StateFlow<List<Relationship>> = _relationships
        .map { list -> list.filter { it.type == 3 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val pendingOutgoing: StateFlow<List<Relationship>> = _relationships
        .map { list -> list.filter { it.type == 4 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val blocked: StateFlow<List<Relationship>> = _relationships
        .map { list -> list.filter { it.type == 2 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getRelationships()) {
                is Result.Success -> _relationships.value = result.data
                is Result.Error -> _error.value = result.message
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    fun updateAddFriendInput(value: String) { _addFriendInput.value = value }

    fun sendFriendRequest() {
        val username = _addFriendInput.value.trim()
        if (username.isBlank()) return
        viewModelScope.launch {
            when (val result = repository.addFriendByUsername(username)) {
                is Result.Success -> { _addFriendInput.value = ""; load() }
                is Result.Error -> _error.value = result.message
                else -> Unit
            }
        }
    }

    fun removeRelationship(userId: String) {
        viewModelScope.launch { repository.removeRelationship(userId); load() }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch { repository.blockUser(userId); load() }
    }

    fun clearError() { _error.value = null }
}
