package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
 data class IpAuthRequiredResponse(
    val code: String,
    val message: String? = null,
    @SerialName("ip_authorization_required")
    val ipAuthorizationRequired: Boolean? = null,
    val ticket: String? = null,
    val email: String? = null,
    @SerialName("resend_available_in")
    val resendAvailableIn: Int? = null
)
