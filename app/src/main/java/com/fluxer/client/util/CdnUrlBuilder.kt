package com.fluxer.client.util

object CdnUrlBuilder {
    private const val FALLBACK_CDN = "https://fluxerusercontent.com"

    fun avatarUrl(cdnBase: String?, userId: String, hash: String?): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.startsWith("http://") || hash.startsWith("https://")) return hash
        val base = cdnBase?.trimEnd('/') ?: FALLBACK_CDN
        return "$base/avatars/$userId/$hash.png"
    }

    fun serverIconUrl(cdnBase: String?, guildId: String, hash: String?): String? {
        if (hash.isNullOrBlank()) return null
        if (hash.startsWith("http://") || hash.startsWith("https://")) return hash
        val base = cdnBase?.trimEnd('/') ?: FALLBACK_CDN
        return "$base/icons/$guildId/$hash.png"
    }
}
