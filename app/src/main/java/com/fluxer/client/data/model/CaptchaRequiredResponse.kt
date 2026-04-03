package com.fluxer.client.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CaptchaRequiredResponse(
    val error: String,
    val message: String? = null,
    val code: Int? = null,
    val sitekey: String? = null,
    val provider: String? = null
)
