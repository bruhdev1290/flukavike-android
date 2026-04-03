package com.fluxer.client.data.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStorage @Inject constructor() {
    @Volatile
    var token: String? = null
        private set

    fun setToken(token: String?) {
        this.token = token
    }

    fun clear() {
        token = null
    }
}
