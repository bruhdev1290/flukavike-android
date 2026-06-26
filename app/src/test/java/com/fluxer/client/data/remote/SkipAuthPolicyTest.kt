package com.fluxer.client.data.remote

import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipAuthPolicyTest {
    @Test
    fun skipsPublicAuthEndpoints() {
        assertTrue(SkipAuthPolicy.isPublicAuthPath("/api/auth/login"))
        assertTrue(SkipAuthPolicy.isPublicAuthPath("/api/auth/register"))
        assertTrue(SkipAuthPolicy.isPublicAuthPath("/api/auth/login/mfa/totp"))
        assertTrue(SkipAuthPolicy.isPublicAuthPath("/v1/auth/login/mfa/webauthn/options"))
        assertTrue(SkipAuthPolicy.isPublicAuthPath("/api/auth/webauthn/options"))
        assertTrue(SkipAuthPolicy.isPublicAuthPath("/v1/auth/webauthn/options"))
    }

    @Test
    fun keepsAuthForPrivateEndpoints() {
        assertFalse(SkipAuthPolicy.isPublicAuthPath("/api/users/@me"))
        assertFalse(SkipAuthPolicy.isPublicAuthPath("/api/channels/1/messages"))
        assertFalse(SkipAuthPolicy.isPublicAuthPath("/api/auth/refresh"))
    }

    @Test
    fun skipsWhenRequestHeaderIsSet() {
        val request = Request.Builder()
            .url("https://web.fluxer.app/api/users/@me")
            .header(SkipAuthPolicy.SKIP_AUTH_HEADER, "true")
            .build()
        assertTrue(SkipAuthPolicy.shouldSkipAuth(request))
    }
}
