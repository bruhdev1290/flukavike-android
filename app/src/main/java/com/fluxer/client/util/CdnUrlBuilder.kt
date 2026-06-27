package com.fluxer.client.util

object CdnUrlBuilder {
    private const val FALLBACK_CDN = "https://fluxerusercontent.com"

    fun avatarUrl(cdnBase: String?, userId: String, hash: String?): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.startsWith("http://") || hash.startsWith("https://")) return hash
        val base = mediaBase(cdnBase)
        return "$base/avatars/$userId/$hash.${extensionFor(hash)}"
    }

    fun serverIconUrl(cdnBase: String?, guildId: String, hash: String?): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.startsWith("http://") || hash.startsWith("https://")) return hash
        val base = mediaBase(cdnBase)
        return "$base/icons/$guildId/$hash.${extensionFor(hash)}"
    }

    private fun extensionFor(hash: String): String = if (hash.startsWith("a_")) "gif" else "png"

    private fun mediaBase(cdnBase: String?): String {
        val base = cdnBase?.trimEnd('/') ?: return FALLBACK_CDN
        return if (
            base == "https://web.fluxer.app" ||
            base == "https://fluxerstatic.com"
        ) {
            FALLBACK_CDN
        } else {
            base
        }
    }
}
