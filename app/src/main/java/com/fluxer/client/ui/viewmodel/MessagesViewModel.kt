package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.ChannelType
import com.fluxer.client.data.model.displayName
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.data.repository.HomeStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val homeStateRepository: HomeStateRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _allDmChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _dmChannels = MutableStateFlow<List<Channel>>(emptyList())
    val dmChannels: StateFlow<List<Channel>> = _dmChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _hiddenDmIds = MutableStateFlow<Set<String>>(emptySet())
    private val _favoriteChannelIds = MutableStateFlow<Set<String>>(emptySet())

    val unreadCountsByChannel: StateFlow<Map<String, Int>> = homeStateRepository.unreadCountsByChannel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            chatRepository.dmChannelsFlow.collectLatest { channels ->
                _allDmChannels.value = channels
                filterChannels(_searchQuery.value)
            }
        }

        viewModelScope.launch {
            homeStateRepository.hiddenDmIds.collectLatest { hiddenIds ->
                _hiddenDmIds.value = hiddenIds
                filterChannels(_searchQuery.value)
            }
        }

        viewModelScope.launch {
            homeStateRepository.favoriteChannelIds.collectLatest { favoriteIds ->
                _favoriteChannelIds.value = favoriteIds
                filterChannels(_searchQuery.value)
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collectLatest { query ->
                    filterChannels(query)
                }
        }
    }

    fun loadDMChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.getDMChannels()
            result.onSuccess { channels ->
                _allDmChannels.value = channels
                filterChannels(_searchQuery.value)
            }.onError { error ->
                Timber.e("Failed to load DM channels: $error")
            }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun filterChannels(query: String) {
        val hidden = _hiddenDmIds.value
        val favoriteIds = _favoriteChannelIds.value
        val currentUserId =
            (authRepository.authState.value as? AuthRepository.AuthState.Authenticated)?.user?.id
        val channels = _allDmChannels.value
            .filter(::isDirectConversation)
            .filterNot { channel -> currentUserId != null && channel.id == currentUserId }
            .filterNot { it.id in hidden }
        val filtered = if (query.isBlank()) {
            channels
        } else {
            channels.filter { channel ->
                channel.displayName().contains(query, ignoreCase = true)
            }
        }
        val sorted = filtered.sortedWith(
            compareByDescending<Channel> { it.id in favoriteIds }
                .thenByDescending { it.lastMessageId ?: "" }
                .thenBy { it.displayName().lowercase() }
        )
        _filteredChannels.value = sorted
        _dmChannels.value = sorted
    }

    fun createDMChannel(recipientId: String, onCreated: ((com.fluxer.client.data.model.Channel) -> Unit)? = null) {
        viewModelScope.launch {
            val result = chatRepository.createDMChannel(recipientId)
            result.onSuccess { channel ->
                val current = _allDmChannels.value.toMutableList()
                if (current.none { it.id == channel.id }) current.add(0, channel)
                _allDmChannels.value = current
                filterChannels(_searchQuery.value)
                onCreated?.invoke(channel)
            }.onError { error ->
                Timber.e("Failed to create DM channel: $error")
            }
        }
    }

    fun openPersonalNotes(onOpened: (Channel) -> Unit) {
        val currentUser = (authRepository.authState.value as? AuthRepository.AuthState.Authenticated)?.user
        if (currentUser == null) {
            Timber.w("Cannot open personal notes without an authenticated user")
            return
        }

        val channel = chatRepository.ensurePersonalNotesChannel(currentUser)
        val current = _allDmChannels.value.toMutableList()
        if (current.none { it.id == channel.id }) {
            current.add(0, channel)
            _allDmChannels.value = current
            filterChannels(_searchQuery.value)
        }
        onOpened(channel)
    }
    
    fun closeDM(channelId: String) {
        viewModelScope.launch {
            homeStateRepository.hideDm(channelId)
            filterChannels(_searchQuery.value)
            Timber.d("Closing DM: $channelId")
        }
    }
    
    fun muteDM(channelId: String, duration: com.fluxer.client.ui.screens.MuteDuration) {
        viewModelScope.launch {
            homeStateRepository.muteChannel(channelId, duration.untilMillis())
            Timber.d("Muting DM: $channelId for $duration")
        }
    }

    fun togglePinnedDM(channelId: String) {
        viewModelScope.launch {
            val channel = _allDmChannels.value.firstOrNull { it.id == channelId } ?: return@launch
            homeStateRepository.toggleFavorite(channel.id, channel.serverId)
        }
    }

    fun isPinned(channelId: String): Boolean = channelId in _favoriteChannelIds.value

    private fun isDirectConversation(channel: Channel): Boolean =
        channel.type == ChannelType.DM &&
            channel.serverId == null &&
            channel.recipients.isNotEmpty() &&
            channel.recipients.none { it.id == SYSTEM_USER_ID }

    private fun com.fluxer.client.ui.screens.MuteDuration.untilMillis(): Long {
        val now = System.currentTimeMillis()
        return when (this) {
            com.fluxer.client.ui.screens.MuteDuration.MINUTES_15 -> now + 15 * 60_000L
            com.fluxer.client.ui.screens.MuteDuration.MINUTES_30 -> now + 30 * 60_000L
            com.fluxer.client.ui.screens.MuteDuration.HOURS_1 -> now + 60 * 60_000L
            com.fluxer.client.ui.screens.MuteDuration.HOURS_3 -> now + 3 * 60 * 60_000L
            com.fluxer.client.ui.screens.MuteDuration.HOURS_4 -> now + 4 * 60 * 60_000L
            com.fluxer.client.ui.screens.MuteDuration.HOURS_8 -> now + 8 * 60 * 60_000L
            com.fluxer.client.ui.screens.MuteDuration.UNTIL_MORNING -> nextMorningMillis(now)
            com.fluxer.client.ui.screens.MuteDuration.ALWAYS -> Long.MAX_VALUE
        }
    }

    private companion object {
        const val SYSTEM_USER_ID = "0"
    }

    private fun nextMorningMillis(now: Long): Long {
        val zone = java.time.ZoneId.systemDefault()
        val current = java.time.Instant.ofEpochMilli(now).atZone(zone)
        val morning = current.toLocalDate().atTime(8, 0).atZone(zone)
        val target = if (morning.toInstant().toEpochMilli() > now) morning else morning.plusDays(1)
        return target.toInstant().toEpochMilli()
    }
}
