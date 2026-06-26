package com.fluxer.client.navigation

import android.content.Intent

sealed class FluxerRoute(open val path: String) {
    data object Loading : FluxerRoute("/loading")
    data object Login : FluxerRoute("/login")
    data object Reconnecting : FluxerRoute("/reconnecting")
    data object Me : FluxerRoute("/channels/@me")
    data class DmChannel(val channelId: String) : FluxerRoute("/channels/@me/$channelId")
    data class DmCall(val channelId: String) : FluxerRoute("/channels/@me/$channelId/call")
    data object Favorites : FluxerRoute("/channels/@favorites")
    data class FavoriteChannel(val channelId: String) : FluxerRoute("/channels/@favorites/$channelId")
    data class Guild(val guildId: String) : FluxerRoute("/channels/$guildId")
    data class GuildChannel(
        val guildId: String,
        val channelId: String,
        val messageId: String? = null
    ) : FluxerRoute(
        if (messageId == null) {
            "/channels/$guildId/$channelId"
        } else {
            "/channels/$guildId/$channelId/$messageId"
        }
    )
    data object Notifications : FluxerRoute("/notifications")
    data object You : FluxerRoute("/you")
    data object Settings : FluxerRoute("/settings")
    data class GuildSettings(val guildId: String, val tab: String? = null) : FluxerRoute(
        if (tab == null) "/settings/guild/$guildId" else "/settings/guild/$guildId?tab=$tab"
    )
    data class Invite(val code: String) : FluxerRoute("/invite/$code")
    data class Gift(val code: String) : FluxerRoute("/gift/$code")
    data class ThemePreview(val themeId: String) : FluxerRoute("/theme/$themeId")
    data class UserProfile(val userId: String) : FluxerRoute("/users/$userId")
    data class GuildVoice(val guildId: String, val channelId: String) : FluxerRoute("/voice/$guildId/$channelId")
}

enum class ShellBranch {
    Home,
    Notifications,
    You
}

enum class RouteKind {
    Loading,
    Login,
    Reconnecting,
    ChannelsRoot,
    Chat,
    DmCall,
    Notifications,
    You,
    Settings,
    Placeholder
}

object RoutePaths {
    const val Loading = "/loading"
    const val Login = "/login"
    const val Reconnecting = "/reconnecting"
    const val Me = "/channels/@me"
    const val Favorites = "/channels/@favorites"
    const val Notifications = "/notifications"
    const val You = "/you"
    const val Settings = "/settings"

    fun dmChannel(channelId: String) = "/channels/@me/$channelId"
    fun dmCall(channelId: String) = "/channels/@me/$channelId/call"
    fun favoriteChannel(channelId: String) = "/channels/@favorites/$channelId"
    fun guild(guildId: String) = "/channels/$guildId"
    fun guildChannel(guildId: String, channelId: String) = "/channels/$guildId/$channelId"
    fun guildMessage(guildId: String, channelId: String, messageId: String) =
        "/channels/$guildId/$channelId/$messageId"
    fun guildSettings(guildId: String, tab: String? = null) =
        if (tab == null) "/settings/guild/$guildId" else "/settings/guild/$guildId?tab=$tab"
    fun invite(code: String) = "/invite/$code"
    fun gift(code: String) = "/gift/$code"
    fun theme(themeId: String) = "/theme/$themeId"
}

fun classifyRoute(path: String): RouteKind {
    val clean = path.substringBefore("?")
    val segments = clean.trim('/').split('/').filter { it.isNotBlank() }
    return when {
        clean == RoutePaths.Loading -> RouteKind.Loading
        clean == RoutePaths.Login -> RouteKind.Login
        clean == RoutePaths.Reconnecting -> RouteKind.Reconnecting
        clean == RoutePaths.Notifications -> RouteKind.Notifications
        clean == RoutePaths.You -> RouteKind.You
        clean.startsWith("/settings/") -> RouteKind.Settings
        segments.size >= 4 && segments[0] == "channels" && segments[1] == "@me" && segments[3] == "call" ->
            RouteKind.DmCall
        segments.firstOrNull() == "channels" && segments.size <= 2 -> RouteKind.ChannelsRoot
        segments.firstOrNull() == "channels" && segments.size >= 3 -> RouteKind.Chat
        else -> RouteKind.Placeholder
    }
}

fun branchForPath(path: String): ShellBranch = when {
    path.startsWith(RoutePaths.Notifications) -> ShellBranch.Notifications
    path.startsWith(RoutePaths.You) || path.startsWith(RoutePaths.Settings) -> ShellBranch.You
    else -> ShellBranch.Home
}

fun routeFromPath(rawPath: String): FluxerRoute {
    val path = rawPath.substringBefore("?")
    val query = rawPath.substringAfter("?", "")
    val segments = path.trim('/').split('/').filter { it.isNotBlank() }
    return when {
        path == RoutePaths.Loading -> FluxerRoute.Loading
        path == RoutePaths.Login -> FluxerRoute.Login
        path == RoutePaths.Reconnecting -> FluxerRoute.Reconnecting
        path == RoutePaths.Me -> FluxerRoute.Me
        path == RoutePaths.Favorites -> FluxerRoute.Favorites
        path == RoutePaths.Notifications -> FluxerRoute.Notifications
        path == RoutePaths.You -> FluxerRoute.You
        path == RoutePaths.Settings -> FluxerRoute.Settings
        segments.size == 3 && segments[0] == "channels" && segments[1] == "@me" ->
            FluxerRoute.DmChannel(segments[2])
        segments.size == 4 && segments[0] == "channels" && segments[1] == "@me" && segments[3] == "call" ->
            FluxerRoute.DmCall(segments[2])
        segments.size == 3 && segments[0] == "channels" && segments[1] == "@favorites" ->
            FluxerRoute.FavoriteChannel(segments[2])
        segments.size == 2 && segments[0] == "channels" ->
            FluxerRoute.Guild(segments[1])
        segments.size >= 3 && segments[0] == "channels" ->
            FluxerRoute.GuildChannel(segments[1], segments[2], segments.getOrNull(3))
        segments.size >= 3 && segments[0] == "settings" && segments[1] == "guild" ->
            FluxerRoute.GuildSettings(segments[2], queryParameter(query, "tab"))
        segments.size == 2 && segments[0] == "invite" -> FluxerRoute.Invite(segments[1])
        segments.size == 2 && segments[0] == "gift" -> FluxerRoute.Gift(segments[1])
        segments.size == 2 && segments[0] == "theme" -> FluxerRoute.ThemePreview(segments[1])
        else -> FluxerRoute.Me
    }
}

private fun queryParameter(query: String, key: String): String? {
    if (query.isBlank()) return null
    return query.split('&')
        .mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
        }
        .firstOrNull { it.first == key }
        ?.second
}

fun routeFromIntent(intent: Intent?): FluxerRoute? {
    if (intent == null) return null
    intent.data?.let { uri ->
        val path = if (uri.scheme == "fluxer" && !uri.host.isNullOrBlank()) {
            "/" + uri.host + uri.path.orEmpty()
        } else {
            uri.path.orEmpty().ifBlank { RoutePaths.Me }
        }
        return routeFromPath(path + uri.encodedQuery?.let { "?$it" }.orEmpty())
    }

    val notificationType = intent.getStringExtra("notification_type")
    val guildId = intent.getStringExtra("guild_id")
    val channelId = intent.getStringExtra("channel_id")
    val messageId = intent.getStringExtra("message_id")
    val callId = intent.getStringExtra("call_id")

    return when {
        notificationType == "call" && !channelId.isNullOrBlank() -> FluxerRoute.DmCall(channelId)
        notificationType == "call" && !callId.isNullOrBlank() -> FluxerRoute.DmCall(callId)
        !guildId.isNullOrBlank() && !channelId.isNullOrBlank() ->
            FluxerRoute.GuildChannel(guildId, channelId, messageId)
        !channelId.isNullOrBlank() -> FluxerRoute.DmChannel(channelId)
        notificationType != null -> FluxerRoute.Notifications
        else -> null
    }
}
