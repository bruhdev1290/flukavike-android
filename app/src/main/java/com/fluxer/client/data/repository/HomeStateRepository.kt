package com.fluxer.client.data.repository

import com.fluxer.client.data.local.dao.FavoriteChannelDao
import com.fluxer.client.data.local.dao.NotificationFeedDao
import com.fluxer.client.data.local.model.FavoriteChannelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeStateRepository @Inject constructor(
    private val favoriteChannelDao: FavoriteChannelDao,
    private val notificationFeedDao: NotificationFeedDao
) {
    val favoriteChannels: Flow<List<FavoriteChannelEntity>> = favoriteChannelDao.observeAll()

    val favoriteChannelIds: Flow<Set<String>> = favoriteChannels
        .map { favorites -> favorites.mapTo(linkedSetOf()) { it.channelId } }

    val unreadCountsByChannel: Flow<Map<String, Int>> = notificationFeedDao.observeUnreadCounts()
        .map { counts -> counts.associate { it.channelId to it.unreadCount } }

    suspend fun toggleFavorite(channelId: String, guildId: String?) {
        val current = favoriteChannelDao.getAll()
        val existing = current.firstOrNull { it.channelId == channelId }
        if (existing != null) {
            favoriteChannelDao.delete(channelId)
            return
        }

        val nextPosition = (current.maxOfOrNull { it.position } ?: -1) + 1
        favoriteChannelDao.upsert(
            FavoriteChannelEntity(
                channelId = channelId,
                guildId = guildId,
                position = nextPosition,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFavorite(channelId: String) {
        favoriteChannelDao.delete(channelId)
    }
}
