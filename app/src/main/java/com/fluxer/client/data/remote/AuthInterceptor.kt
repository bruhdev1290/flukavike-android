package com.fluxer.client.data.remote

import com.fluxer.client.data.local.AuthTokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authTokenStorage: AuthTokenStorage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = authTokenStorage.token

        if (SkipAuthPolicy.shouldSkipAuth(request)) {
            return chain.proceed(
                request.newBuilder()
                    .removeHeader("Authorization")
                    .removeHeader(SkipAuthPolicy.SKIP_AUTH_HEADER)
                    .build()
            )
        }

        return if (!token.isNullOrBlank()) {
            val newRequest = request.newBuilder()
                .header("Authorization", formatSessionAuthorizationHeader(token))
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }

    private fun formatSessionAuthorizationHeader(token: String): String {
        val trimmed = token.trim()
        return if (trimmed.startsWith("Bearer ")) {
            trimmed.removePrefix("Bearer ").trim()
        } else {
            trimmed
        }
    }
}
