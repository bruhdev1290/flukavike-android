package com.fluxer.client.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CdnUrlBuilderTest {
    @Test
    fun `builds static avatar and guild icon urls`() {
        assertEquals(
            "https://media.example/avatars/user-1/hash.png",
            CdnUrlBuilder.avatarUrl("https://media.example/", "user-1", "hash")
        )
        assertEquals(
            "https://media.example/icons/guild-1/hash.png",
            CdnUrlBuilder.serverIconUrl("https://media.example", "guild-1", "hash")
        )
    }

    @Test
    fun `uses gif for animated hashes and preserves absolute urls`() {
        assertEquals(
            "https://fluxerusercontent.com/avatars/user-1/a_hash.gif",
            CdnUrlBuilder.avatarUrl(null, "user-1", "a_hash")
        )
        assertEquals(
            "https://example.com/icon.webp",
            CdnUrlBuilder.serverIconUrl(null, "guild-1", "https://example.com/icon.webp")
        )
    }

    @Test
    fun `repairs legacy official cdn values`() {
        assertEquals(
            "https://fluxerusercontent.com/avatars/user-1/hash.png",
            CdnUrlBuilder.avatarUrl("https://web.fluxer.app", "user-1", "hash")
        )
    }
}
