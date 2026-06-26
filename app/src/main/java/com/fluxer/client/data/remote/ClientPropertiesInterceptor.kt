package com.fluxer.client.data.remote

import android.os.Build
import com.fluxer.client.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Base64

class ClientPropertiesInterceptor : Interceptor {
    private val userAgent =
        "FluxerAndroid/${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE})"

    private val propertiesHeader: String by lazy {
        val json = buildString {
            append('{')
            append("\"os\":\"Android\",")
            append("\"browser\":\"Fluxer Android\",")
            append("\"device\":\"").append(escape(Build.MODEL ?: "Android")).append("\",")
            append("\"system_locale\":\"").append(escape(java.util.Locale.getDefault().toLanguageTag())).append("\",")
            append("\"client_version\":\"").append(escape(BuildConfig.VERSION_NAME)).append("\"")
            append('}')
        }
        Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .header("X-Fluxer-Client-Properties", propertiesHeader)
            .build()
        return chain.proceed(request)
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
