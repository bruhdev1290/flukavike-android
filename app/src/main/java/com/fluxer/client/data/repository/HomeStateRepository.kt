package com.fluxer.client.data.repository

import com.fluxer.client.data.local.dao.FavoriteChannelDao
import com.fluxer.client.data.local.dao.NotificationFeedDao
import com.fluxer.client.data.local.dao.ReadStateDao
import com.fluxer.client.data.local.dao.UserPreferenceDao
import com.fluxer.client.data.local.model.FavoriteChannelEntity
import com.fluxer.client.data.local.model.ReadStateEntity
import com.fluxer.client.data.local.model.UserPreferenceEntity
import com.fluxer.client.data.remote.ReadStateApiService
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeStateRepository @Inject constructor(
    private val favoriteChannelDao: FavoriteChannelDao,
    private val notificationFeedDao: NotificationFeedDao,
    private val readStateDao: ReadStateDao,
    private val readStateApiService: ReadStateApiService,
    private val userPreferenceDao: UserPreferenceDao
) {
    val favoriteChannels: Flow<List<FavoriteChannelEntity>> = favoriteChannelDao.observeAll()

    val favoriteChannelIds: Flow<Set<String>> = favoriteChannels
        .map { favorites -> favorites.mapTo(linkedSetOf()) { it.channelId } }

    val hiddenDmIds: Flow<Set<String>> = userPreferenceDao.observeAll()
        .map { preferences ->
            preferences.asSequence()
                .filter { it.key.startsWith(HIDDEN_DM_PREFIX) && it.value == "true" }
                .map { it.key.removePrefix(HIDDEN_DM_PREFIX) }
                .toSet()
        }

    val mutedChannelIds: Flow<Set<String>> = userPreferenceDao.observeAll()
        .map { preferences ->
            val now = System.currentTimeMillis()
            preferences.asSequence()
                .filter { it.key.startsWith(MUTED_CHANNEL_PREFIX) }
                .filter { it.value.toLongOrNull()?.let { until -> until > now } == true }
                .map { it.key.removePrefix(MUTED_CHANNEL_PREFIX) }
                .toSet()
        }

    val unreadCountsByChannel: Flow<Map<String, Int>> = combine(
        notificationFeedDao.observeUnreadCounts(),
        hiddenDmIds,
        mutedChannelIds
    ) { counts, hidden, muted ->
        counts
            .filterNot { it.channelId in hidden || it.channelId in muted }
            .associate { it.channelId to it.unreadCount }
    }

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

    suspend fun markChannelAsRead(channelId: String) {
        notificationFeedDao.markChannelRead(channelId)
    }

    suspend fun ackChannelLatest(channelId: String, latestMessageId: String?) {
        markChannelAsRead(channelId)
        if (latestMessageId.isNullOrBlank()) return

        val current = readStateDao.get(channelId)
        if (current?.lastReadMessageId == latestMessageId && current.mentionCount == 0) {
            return
        }

        readStateDao.upsert(
            ReadStateEntity(
                channelId = channelId,
                lastReadMessageId = latestMessageId,
                mentionCount = 0,
                updatedAt = System.currentTimeMillis()
            )
        )

        try {
            val response = readStateApiService.acknowledgeMessage(channelId, latestMessageId)
            if (!response.isSuccessful) {
                Timber.w("Read ack failed for $channelId/$latestMessageId: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.w(e, "Read ack network error for $channelId/$latestMessageId")
        }
    }

    suspend fun hideDm(channelId: String) {
        userPreferenceDao.upsert(
            UserPreferenceEntity(
                key = "$HIDDEN_DM_PREFIX$channelId",
                value = "true",
                updatedAt = System.currentTimeMillis()
            )
        )
        markChannelAsRead(channelId)
    }

    suspend fun unhideDm(channelId: String) {
        userPreferenceDao.delete("$HIDDEN_DM_PREFIX$channelId")
    }

    suspend fun muteChannel(channelId: String, untilMillis: Long) {
        userPreferenceDao.upsert(
            UserPreferenceEntity(
                key = "$MUTED_CHANNEL_PREFIX$channelId",
                value = untilMillis.toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun unmuteChannel(channelId: String) {
        userPreferenceDao.delete("$MUTED_CHANNEL_PREFIX$channelId")
    }

    suspend fun isFavorite(channelId: String): Boolean =
        favoriteChannelDao.getAll().any { it.channelId == channelId }

    companion object {
        private const val HIDDEN_DM_PREFIX = "dm.hidden."
        private const val MUTED_CHANNEL_PREFIX = "channel.muted."
    }
}
