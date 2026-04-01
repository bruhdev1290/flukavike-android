package com.fluxer.client.data.remote

import com.fluxer.client.data.local.InstanceConfigStore
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlOverrideInterceptor @Inject constructor(
    private val instanceConfigStore: InstanceConfigStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val baseUrl = instanceConfigStore.getActiveBaseUrl().toHttpUrlOrNull()

        if (baseUrl == null) {
            return chain.proceed(request)
        }

        val rewrittenUrl = request.url.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()

        val rewrittenRequest = request.newBuilder()
            .url(rewrittenUrl)
            .build()

        return chain.proceed(rewrittenRequest)
    }
}
