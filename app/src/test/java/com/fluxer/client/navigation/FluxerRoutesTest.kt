package com.fluxer.client.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class FluxerRoutesTest {
    @Test
    fun routeBuildersMirrorCanaryPaths() {
        assertEquals("/channels/@me", RoutePaths.Me)
        assertEquals("/channels/@me/dm1", RoutePaths.dmChannel("dm1"))
        assertEquals("/channels/@me/dm1/call", RoutePaths.dmCall("dm1"))
        assertEquals("/channels/@favorites/fav1", RoutePaths.favoriteChannel("fav1"))
        assertEquals("/channels/guild1", RoutePaths.guild("guild1"))
        assertEquals("/channels/guild1/channel1", RoutePaths.guildChannel("guild1", "channel1"))
        assertEquals(
            "/channels/guild1/channel1/message1",
            RoutePaths.guildMessage("guild1", "channel1", "message1")
        )
        assertEquals("/settings/guild/guild1?tab=roles", RoutePaths.guildSettings("guild1", "roles"))
    }

    @Test
    fun parsesCanaryContentRoutes() {
        assertEquals(FluxerRoute.Me, routeFromPath("/channels/@me"))
        assertEquals(FluxerRoute.DmChannel("dm1"), routeFromPath("/channels/@me/dm1"))
        assertEquals(FluxerRoute.DmCall("dm1"), routeFromPath("/channels/@me/dm1/call"))
        assertEquals(FluxerRoute.FavoriteChannel("chan1"), routeFromPath("/channels/@favorites/chan1"))
        assertEquals(FluxerRoute.Guild("guild1"), routeFromPath("/channels/guild1"))
        assertEquals(
            FluxerRoute.GuildChannel("guild1", "chan1", "msg1"),
            routeFromPath("/channels/guild1/chan1/msg1")
        )
    }

    @Test
    fun parsesSettingsAndPlaceholderRoutes() {
        assertEquals(
            FluxerRoute.GuildSettings("guild1", "bans"),
            routeFromPath("/settings/guild/guild1?tab=bans")
        )
        assertEquals(FluxerRoute.Invite("abc"), routeFromPath("/invite/abc"))
        assertEquals(FluxerRoute.Gift("gift1"), routeFromPath("/gift/gift1"))
        assertEquals(FluxerRoute.ThemePreview("theme1"), routeFromPath("/theme/theme1"))
    }

    @Test
    fun classifiesBranchesAndRouteKinds() {
        assertEquals(RouteKind.Login, classifyRoute("/login"))
        assertEquals(RouteKind.ChannelsRoot, classifyRoute("/channels/@me"))
        assertEquals(RouteKind.Chat, classifyRoute("/channels/guild1/chan1"))
        assertEquals(RouteKind.DmCall, classifyRoute("/channels/@me/dm1/call"))
        assertEquals(ShellBranch.Home, branchForPath("/channels/guild1/chan1"))
        assertEquals(ShellBranch.Notifications, branchForPath("/notifications"))
        assertEquals(ShellBranch.You, branchForPath("/you"))
    }

    @Test
    fun malformedPathsFallBackToHome() {
        assertEquals(FluxerRoute.Me, routeFromPath("/unknown/path"))
    }
}
