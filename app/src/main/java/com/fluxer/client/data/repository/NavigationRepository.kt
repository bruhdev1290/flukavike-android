package com.fluxer.client.data.repository

import com.fluxer.client.data.local.dao.GuildLastChannelDao
import com.fluxer.client.data.local.dao.NavigationStateDao
import com.fluxer.client.data.local.dao.NotificationFeedDao
import com.fluxer.client.data.local.dao.ReadStateDao
import com.fluxer.client.data.local.model.GuildLastChannelEntity
import com.fluxer.client.data.local.model.NavigationStateEntity
import com.fluxer.client.data.local.model.NotificationFeedEntity
import com.fluxer.client.data.local.model.ReadStateEntity
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.ChannelType
import com.fluxer.client.data.model.NotificationData
import com.fluxer.client.navigation.FluxerRoute
import com.fluxer.client.navigation.RoutePaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationRepository @Inject constructor(
    private val navigationStateDao: NavigationStateDao,
    private val guildLastChannelDao: GuildLastChannelDao,
    private val readStateDao: ReadStateDao,
    private val notificationFeedDao: NotificationFeedDao
) {
    private val _pendingRoute = MutableStateFlow<FluxerRoute?>(null)
    val pendingRoute: StateFlow<FluxerRoute?> = _pendingRoute.asStateFlow()

    suspend fun restoreState(): NavigationStateEntity =
        navigationStateDao.get() ?: defaultState()

    suspend fun saveActivePath(
        activePath: String,
        homePath: String,
        notificationsPath: String,
        youPath: String,
        preReconnectPath: String? = null,
        pendingPath: String? = null
    ) {
        navigationStateDao.upsert(
            NavigationStateEntity(
                activePath = activePath,
                homePath = homePath,
                notificationsPath = notificationsPath,
                youPath = youPath,
                preReconnectPath = preReconnectPath,
                pendingPath = pendingPath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun rememberLastGuildChannel(guildId: String, channelId: String) {
        guildLastChannelDao.upsert(
            GuildLastChannelEntity(
                guildId = guildId,
                channelId = channelId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getLastChannelForGuild(guildId: String): String? = guildLastChannelDao.getLastChannel(guildId)

    suspend fun resolveGuildRoot(guildId: String, channels: List<Channel>): FluxerRoute {
        val saved = guildLastChannelDao.getLastChannel(guildId)
        if (saved != null && channels.any { it.id == saved }) {
            return FluxerRoute.GuildChannel(guildId, saved)
        }
        val fallback = channels.firstOrNull {
            it.type != ChannelType.CATEGORY && it.type != ChannelType.UNKNOWN
        }
        return if (fallback == null) {
            FluxerRoute.Guild(guildId)
        } else {
            rememberLastGuildChannel(guildId, fallback.id)
            FluxerRoute.GuildChannel(guildId, fallback.id)
        }
    }

    suspend fun markRead(channelId: String, messageId: String?) {
        readStateDao.upsert(
            ReadStateEntity(
                channelId = channelId,
                lastReadMessageId = messageId,
                mentionCount = 0,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordNotification(
        id: String,
        type: String,
        title: String,
        body: String,
        data: NotificationData?
    ) {
        notificationFeedDao.upsert(
            NotificationFeedEntity(
                id = id,
                type = type,
                title = title,
                body = body,
                guildId = data?.guildId,
                channelId = data?.channelId,
                messageId = data?.messageId,
                createdAt = System.currentTimeMillis(),
                read = false
            )
        )
    }

    fun resolveNotificationRoute(data: NotificationData): FluxerRoute {
        val guildId = data.guildId
        val channelId = data.channelId
        val messageId = data.messageId
        return when {
            !guildId.isNullOrBlank() && !channelId.isNullOrBlank() ->
                FluxerRoute.GuildChannel(guildId, channelId, messageId)
            !channelId.isNullOrBlank() -> FluxerRoute.DmChannel(channelId)
            !data.callId.isNullOrBlank() -> FluxerRoute.DmCall(data.callId)
            !data.url.isNullOrBlank() -> runCatching {
                com.fluxer.client.navigation.routeFromPath(data.url.substringAfter("://").substringAfter("/"))
            }.getOrDefault(FluxerRoute.Notifications)
            else -> FluxerRoute.Notifications
        }
    }

    fun setPendingRoute(route: FluxerRoute?) {
        _pendingRoute.value = route
        if (route != null) {
            Timber.d("Pending navigation route: ${route.path}")
        }
    }

    private fun defaultState() = NavigationStateEntity(
        activePath = RoutePaths.Login,
        homePath = RoutePaths.Me,
        notificationsPath = RoutePaths.Notifications,
        youPath = RoutePaths.You,
        preReconnectPath = null,
        pendingPath = null,
        updatedAt = System.currentTimeMillis()
    )
}
