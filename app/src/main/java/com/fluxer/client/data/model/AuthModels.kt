// =============================================================================
// !! DO NOT MAKE User.email NON-NULLABLE !!
// Message author objects from the Fluxer API do not include email.
// email defaults to "" so deserialization does not crash on message responses.
// See CLAUDE.md for full details.
// =============================================================================
package com.fluxer.client.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name")
    val deviceName: String = "Android Device",
    @SerialName("captcha_key")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val captchaKey: String? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    @SerialName("device_name")
    val deviceName: String = "Android Device"
)

@Serializable
data class AuthResponse(
    val token: String? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val user: User? = null,
    val message: String? = null,
    val mfa: Boolean = false,
    val ticket: String? = null,
    @SerialName("allowed_methods")
    val allowedMethods: List<String> = emptyList(),
    val totp: Boolean = false,
    val sms: Boolean = false,
    val webauthn: Boolean = false,
    @SerialName("sms_phone_hint")
    val smsPhoneHint: String? = null
) {
    fun resolvedToken(): String? = token ?: accessToken
}

@Serializable
data class MfaTicketRequest(
    val ticket: String
)

@Serializable
data class WebAuthnMfaRequest(
    val response: JsonElement,
    val challenge: String,
    val ticket: String
)

@Serializable
data class User(
    val id: String,
    val email: String = "",
    val username: String,
    val discriminator: String = "0001",
    @JsonNames("global_name")
    @SerialName("display_name")
    val displayName: String? = null,
    @JsonNames("avatar")
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
enum class UserStatus {
    ONLINE, AWAY, DND, OFFLINE
}

@Serializable
data class CsrfResponse(
    @SerialName("csrf_token")
    val csrfToken: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null,
    val code: Int? = null
)
