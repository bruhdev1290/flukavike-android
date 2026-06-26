package com.fluxer.client.data.remote

import okhttp3.Request

object SkipAuthPolicy {
    private val publicAuthPaths = setOf(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot",
        "/api/auth/reset",
        "/api/auth/authorize-ip",
        "/api/auth/sso/start",
        "/api/auth/sso/complete",
        "/api/auth/sso/status",
        "/api/auth/username-suggestions",
        "/api/auth/verify",
        "/api/auth/email-revert",
        "/api/auth/handoff/complete"
    )

    fun shouldSkipAuth(request: Request): Boolean {
        if (request.header(SKIP_AUTH_HEADER) == "true") {
            return true
        }
        return isPublicAuthPath(request.url.encodedPath)
    }

    fun isPublicAuthPath(path: String): Boolean {
        if (publicAuthPaths.contains(path)) {
            return true
        }
        if (path.startsWith("/api/auth/login/mfa/")) {
            return true
        }
        if (path.startsWith("/v1/auth/login/mfa/")) {
            return true
        }
        if (path.startsWith("/api/auth/reset/")) {
            return true
        }
        if (path.startsWith("/api/auth/ip-authorization/")) {
            return true
        }
        if (path.startsWith("/api/auth/webauthn/")) {
            return true
        }
        if (path.startsWith("/v1/auth/webauthn/")) {
            return true
        }
        if (path.startsWith("/api/auth/handoff/") &&
            (path.endsWith("/info") || path.endsWith("/status"))
        ) {
            return true
        }
        return false
    }

    const val SKIP_AUTH_HEADER = "X-Fluxer-Skip-Auth"
}
