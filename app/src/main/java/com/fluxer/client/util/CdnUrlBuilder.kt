package com.fluxer.client.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Constructs media URLs for the Fluxer CDN.
 *
 * Format parity with flutter_client-canary:
 *  - Avatars: static -> .webp (a_ prefix stripped), animated -> .gif?animated=true
 *  - Guild icons: static -> .png, animated static preview -> .webp?animated=false, animated -> .gif?animated=true
 *  - Banners: user -> .webp/.gif, guild -> .png/.gif
 */
object CdnUrlBuilder {
    private const val FALLBACK_MEDIA_CDN = "https://fluxerusercontent.com"
    private const val FALLBACK_STATIC_CDN = "https://fluxerstatic.com"
    private const val DEFAULT_AVATAR_COUNT = 6

    object Sizes {
        const val AVATAR_DEFAULT = 160
        const val AVATAR_PROFILE = 240
        const val ICON_DEFAULT = 160
        const val PROFILE_BANNER = 1024
        const val GUILD_BANNER = 1024
    }

    fun avatarUrl(
        cdnBase: String?,
        userId: String,
        hash: String?,
        size: Int = Sizes.AVATAR_DEFAULT,
        animated: Boolean = false
    ): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.isFullUrl()) return hash
        val base = mediaBase(cdnBase)
        val isAnimated = isAnimatedHash(hash)
        return if (animated && isAnimated) {
            buildUrl(base, "avatars/$userId/$hash.gif", mapOf("animated" to "true", "size" to size.toString()))
        } else {
            val normalized = normalizeHash(hash)
            buildUrl(base, "avatars/$userId/$normalized.webp", mapOf("size" to size.toString()))
        }
    }

    fun userBannerUrl(
        cdnBase: String?,
        userId: String,
        hash: String?,
        size: Int = Sizes.PROFILE_BANNER,
        animated: Boolean = false
    ): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.isFullUrl()) return hash
        val base = mediaBase(cdnBase)
        val isAnimated = isAnimatedHash(hash)
        return if (animated && isAnimated) {
            buildUrl(base, "banners/$userId/$hash.gif", mapOf("animated" to "true", "size" to size.toString()))
        } else {
            val normalized = normalizeHash(hash)
            buildUrl(base, "banners/$userId/$normalized.webp", mapOf("size" to size.toString()))
        }
    }

    fun serverIconUrl(
        cdnBase: String?,
        guildId: String,
        hash: String?,
        size: Int = Sizes.ICON_DEFAULT,
        animated: Boolean = false
    ): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.isFullUrl()) return hash
        val base = mediaBase(cdnBase)
        val isAnimated = isAnimatedHash(hash)
        return when {
            animated && isAnimated -> {
                buildUrl(base, "icons/$guildId/$hash.gif", mapOf("animated" to "true", "size" to size.toString()))
            }
            isAnimated -> {
                buildUrl(base, "icons/$guildId/$hash.webp", mapOf("animated" to "false", "size" to size.toString()))
            }
            else -> {
                buildUrl(base, "icons/$guildId/$hash.png", mapOf("size" to size.toString()))
            }
        }
    }

    fun serverBannerUrl(
        cdnBase: String?,
        guildId: String,
        hash: String?,
        size: Int = Sizes.GUILD_BANNER,
        animated: Boolean = false
    ): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.isFullUrl()) return hash
        val base = mediaBase(cdnBase)
        val isAnimated = isAnimatedHash(hash)
        return if (animated && isAnimated) {
            buildUrl(base, "banners/$guildId/$hash.gif", mapOf("animated" to "true", "size" to size.toString()))
        } else {
            buildUrl(base, "banners/$guildId/$hash.png", mapOf("size" to size.toString()))
        }
    }

    /**
     * Returns a default avatar URL based on the user id, or null if the id is blank.
     * Matches flutter_client-canary: {staticCdn}/avatars/{userId % 6}.png
     */
    fun avatarUrlOrDefault(
        cdnBase: String?,
        staticCdnBase: String?,
        userId: String,
        hash: String?,
        size: Int = Sizes.AVATAR_DEFAULT
    ): String? {
        return avatarUrl(cdnBase, userId, hash, size) ?: defaultAvatarUrl(staticCdnBase, userId)
    }

    fun defaultAvatarUrl(
        staticCdnBase: String?,
        userId: String?
    ): String? {
        if (userId.isNullOrBlank()) return null
        val index = runCatching {
            java.math.BigInteger(userId).mod(java.math.BigInteger.valueOf(DEFAULT_AVATAR_COUNT.toLong())).toInt()
        }.getOrElse { userId.hashCode().absoluteValue % DEFAULT_AVATAR_COUNT }
        val base = staticCdnBase?.trimEnd('/')?.takeIf { it.startsWith("http") } ?: FALLBACK_STATIC_CDN
        return "$base/avatars/$index.png"
    }

    private fun isAnimatedHash(hash: String): Boolean = hash.startsWith("a_")

    private fun normalizeHash(hash: String): String =
        if (hash.startsWith("a_")) hash.substring(2) else hash

    private fun String.isFullUrl(): Boolean =
        startsWith("http://") || startsWith("https://")

    private fun mediaBase(cdnBase: String?): String {
        val base = cdnBase?.trimEnd('/') ?: return FALLBACK_MEDIA_CDN
        return if (
            base == "https://web.fluxer.app" ||
            base == "https://fluxerstatic.com"
        ) {
            FALLBACK_MEDIA_CDN
        } else {
            base
        }
    }

    private fun buildUrl(base: String, path: String, query: Map<String, String>): String {
        val builder = "$base/$path".toHttpUrlOrNull()?.newBuilder() ?: return "$base/$path"
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}
