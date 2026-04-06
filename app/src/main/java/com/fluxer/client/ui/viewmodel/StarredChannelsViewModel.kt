package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.Server
import com.fluxer.client.ui.screens.StarredChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StarredChannelsViewModel @Inject constructor(
    // TODO: Inject repository for starred channels
) : ViewModel() {

    private val _starredChannels = MutableStateFlow<List<StarredChannel>>(emptyList())
    val starredChannels: StateFlow<List<StarredChannel>> = _starredChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Local cache of starred channel IDs (should be persisted in DataStore/DB)
    private val _starredChannelIds = MutableStateFlow<Set<String>>(emptySet())

    fun loadStarredChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // TODO: Load from repository/database
            // For now, generate mock data
            val mockChannels = generateMockStarredChannels()
            _starredChannels.value = mockChannels
            _starredChannelIds.value = mockChannels.map { it.channel.id }.toSet()
            
            _isLoading.value = false
        }
    }

    fun starChannel(channel: Channel, server: Server) {
        viewModelScope.launch {
            val current = _starredChannelIds.value.toMutableSet()
            current.add(channel.id)
            _starredChannelIds.value = current
            
            // Add to list if not present
            val channels = _starredChannels.value.toMutableList()
            if (channels.none { it.channel.id == channel.id }) {
                channels.add(StarredChannel(channel, server))
                _starredChannels.value = channels.sortedBy { it.server.name }
            }
            
            // TODO: Persist to repository
            Timber.d("Starred channel: ${channel.name}")
        }
    }

    fun unstarChannel(channelId: String) {
        viewModelScope.launch {
            val current = _starredChannelIds.value.toMutableSet()
            current.remove(channelId)
            _starredChannelIds.value = current
            
            // Remove from list
            _starredChannels.value = _starredChannels.value.filter { it.channel.id != channelId }
            
            // TODO: Persist to repository
            Timber.d("Unstarred channel: $channelId")
        }
    }

    fun isChannelStarred(channelId: String): Boolean {
        return _starredChannelIds.value.contains(channelId)
    }

    fun moveChannel(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = _starredChannels.value.toMutableList()
            if (fromIndex in current.indices && toIndex in current.indices) {
                val item = current.removeAt(fromIndex)
                current.add(toIndex, item)
                _starredChannels.value = current
                
                // TODO: Persist new order
            }
        }
    }

    private fun generateMockStarredChannels(): List<StarredChannel> {
        // Mock data for demonstration
        return listOf(
            StarredChannel(
                channel = com.fluxer.client.data.model.Channel(
                    id = "1",
                    name = "general",
                    type = com.fluxer.client.data.model.ChannelType.TEXT,
                    serverId = "server1"
                ),
                server = Server(
                    id = "server1",
                    name = "Fluxer Developers",
                    ownerId = "owner1",
                    memberCount = 150,
                    onlineCount = 45
                ),
                lastMessage = "Andrew: for small fixes i just use auto complete...",
                lastMessageTime = System.currentTimeMillis() - 300000,
                unreadCount = 3
            ),
            StarredChannel(
                channel = com.fluxer.client.data.model.Channel(
                    id = "2",
                    name = "tech",
                    type = com.fluxer.client.data.model.ChannelType.TEXT,
                    serverId = "server1"
                ),
                server = Server(
                    id = "server1",
                    name = "Fluxer Developers",
                    ownerId = "owner1",
                    memberCount = 150,
                    onlineCount = 45
                ),
                lastMessage = "Rakanishu: is there a website i can go on...",
                lastMessageTime = System.currentTimeMillis() - 3600000,
                unreadCount = 0
            ),
            StarredChannel(
                channel = com.fluxer.client.data.model.Channel(
                    id = "3",
                    name = "random",
                    type = com.fluxer.client.data.model.ChannelType.TEXT,
                    serverId = "server2"
                ),
                server = Server(
                    id = "server2",
                    name = "Gaming Squad",
                    ownerId = "owner2",
                    memberCount = 42,
                    onlineCount = 12
                ),
                lastMessage = "John: Anyone up for a game tonight?",
                lastMessageTime = System.currentTimeMillis() - 7200000,
                unreadCount = 5
            ),
            StarredChannel(
                channel = com.fluxer.client.data.model.Channel(
                    id = "4",
                    name = "Voice Chat",
                    type = com.fluxer.client.data.model.ChannelType.VOICE,
                    serverId = "server2"
                ),
                server = Server(
                    id = "server2",
                    name = "Gaming Squad",
                    ownerId = "owner2",
                    memberCount = 42,
                    onlineCount = 12
                ),
                lastMessage = null,
                lastMessageTime = null,
                unreadCount = 0
            )
        )
    }
}
