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

        // Don't add auth header to auth endpoints
        val path = request.url.encodedPath
        if (path.startsWith("/api/auth/") && !path.startsWith("/api/auth/me")) {
            return chain.proceed(request)
        }

        return if (!token.isNullOrBlank()) {
            val newRequest = request.newBuilder()
                .header("Authorization", token)
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
}
