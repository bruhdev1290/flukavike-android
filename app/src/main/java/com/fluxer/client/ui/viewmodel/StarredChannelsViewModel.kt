package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.Server
import com.fluxer.client.data.model.displayName
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.data.repository.HomeStateRepository
import com.fluxer.client.ui.screens.StarredChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StarredChannelsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val homeStateRepository: HomeStateRepository
) : ViewModel() {

    private val _guilds = MutableStateFlow<List<Server>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val starredChannels: StateFlow<List<StarredChannel>> = combine(
        homeStateRepository.favoriteChannels,
        homeStateRepository.unreadCountsByChannel,
        chatRepository.channelCacheFlow,
        chatRepository.dmChannelsFlow,
        _guilds
    ) { favorites, unreadCounts, channelCache, dmChannels, guilds ->
        val guildMap = guilds.associateBy { it.id }
        val guildChannels = channelCache.values.flatten().associateBy { it.id }
        val dmMap = dmChannels.associateBy { it.id }

        favorites.mapNotNull { favorite ->
            val channel = guildChannels[favorite.channelId] ?: dmMap[favorite.channelId]
            val server = favorite.guildId?.let(guildMap::get) ?: channel?.serverId?.let(guildMap::get)
            if (channel == null || server == null) {
                return@mapNotNull null
            }

            StarredChannel(
                channel = channel,
                server = server,
                lastMessage = channel.topic?.takeIf { it.isNotBlank() } ?: channel.displayName(),
                unreadCount = unreadCounts[channel.id] ?: 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadStarredChannels() {
        viewModelScope.launch {
            _isLoading.value = true

            chatRepository.getUserGuilds()
                .onSuccess { guilds -> _guilds.value = guilds }
                .onError { error -> Timber.e("Failed to load guilds for favorites: $error") }

            chatRepository.getDMChannels()
                .onError { error -> Timber.e("Failed to load DMs for favorites: $error") }

            _isLoading.value = false
        }
    }

    fun unstarChannel(channelId: String) {
        viewModelScope.launch {
            homeStateRepository.removeFavorite(channelId)
        }
    }

    fun isChannelStarred(channelId: String): Boolean {
        return starredChannels.value.any { it.channel.id == channelId }
    }

    fun moveChannel(fromIndex: Int, toIndex: Int) {
        Timber.d("Reordering favorites is not wired yet: $fromIndex -> $toIndex")
    }
}
