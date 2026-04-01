package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name")
    val deviceName: String = "Android Device"
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
    val user: User,
    val message: String? = null
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
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
