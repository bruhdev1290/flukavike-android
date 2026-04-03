package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
 data class InstanceConfig(
    val api: String,
    val gateway: String = "",
    val cdn: String? = null,
    @SerialName("public_api")
    val publicApi: String? = null,
    val web: String? = null,
    val admin: String? = null,
    val invite: String? = null,
    val captcha: CaptchaConfig? = null,
    val endpoints: EndpointsConfig? = null,
    val name: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val banner: String? = null,
    @SerialName("public_instance")
    val publicInstance: Boolean? = null,
    @SerialName("user_count")
    val userCount: Int? = null,
    val version: String? = null,
    val features: List<String>? = null
) {
    fun resolvedApi(): String {
        return api.takeIf { it.isNotBlank() }
            ?: endpoints?.api?.takeIf { it.isNotBlank() }
            ?: endpoints?.apiClient?.takeIf { it.isNotBlank() }
            ?: endpoints?.apiPublic?.takeIf { it.isNotBlank() }
            ?: ""
    }

    fun resolvedGateway(): String {
        return gateway.takeIf { it.isNotBlank() }
            ?: endpoints?.gateway?.takeIf { it.isNotBlank() }
            ?: ""
    }

    fun resolvedCdn(): String? {
        return cdn?.takeIf { it.isNotBlank() }
            ?: endpoints?.staticCdn?.takeIf { it.isNotBlank() }
            ?: endpoints?.media?.takeIf { it.isNotBlank() }
    }

    fun resolvedWeb(): String? {
        return web?.takeIf { it.isNotBlank() }
            ?: endpoints?.webapp?.takeIf { it.isNotBlank() }
    }

    @Serializable
    data class EndpointsConfig(
        val api: String? = null,
        @SerialName("api_client")
        val apiClient: String? = null,
        @SerialName("api_public")
        val apiPublic: String? = null,
        val gateway: String? = null,
        val media: String? = null,
        @SerialName("static_cdn")
        val staticCdn: String? = null,
        val webapp: String? = null,
        val admin: String? = null,
        val invite: String? = null
    )

    @Serializable
    data class CaptchaConfig(
        val provider: String = "hcaptcha",
        val sitekey: String = "",
        val service: String? = null,
        @SerialName("site_key")
        val siteKeyAlt: String? = null,
        val key: String? = null,
        @SerialName("hcaptcha_site_key")
        val hcaptchaSiteKey: String? = null,
        @SerialName("turnstile_site_key")
        val turnstileSiteKey: String? = null
    ) {
        fun resolvedSitekey(): String {
            return sitekey.takeIf { it.isNotBlank() }
                ?: siteKeyAlt?.takeIf { it.isNotBlank() }
                ?: key?.takeIf { it.isNotBlank() }
                ?: hcaptchaSiteKey?.takeIf { it.isNotBlank() }
                ?: turnstileSiteKey?.takeIf { it.isNotBlank() }
                ?: ""
        }

        fun resolvedProvider(): String {
            val p = provider.trim().lowercase()
            if (p.isNotBlank() && p != "none") return p
            if (!turnstileSiteKey.isNullOrBlank()) return "turnstile"
            if (!hcaptchaSiteKey.isNullOrBlank()) return "hcaptcha"
            return "hcaptcha"
        }
    }
}
