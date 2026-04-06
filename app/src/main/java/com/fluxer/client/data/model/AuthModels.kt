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
    val user: User? = null,
    val message: String? = null
) {
    fun resolvedToken(): String? = token ?: accessToken
}

@Serializable
data class User(
    val id: String,
    val email: String = "",
    val username: String,
    val discriminator: String = "0001",
    @SerialName("display_name")
    val displayName: String? = null,
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
