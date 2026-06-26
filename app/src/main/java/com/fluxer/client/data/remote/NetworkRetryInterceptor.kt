package com.fluxer.client.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkRetryInterceptor(
    private val maxRetries: Int = 3
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastError: java.io.IOException? = null
        while (attempt <= maxRetries) {
            try {
                return chain.proceed(chain.request())
            } catch (error: java.io.IOException) {
                if (!isRetryable(error) || attempt >= maxRetries) {
                    throw error
                }
                lastError = error
                attempt += 1
                Thread.sleep(250L * (1 shl attempt))
            }
        }
        throw lastError ?: java.io.IOException("Network retry failed")
    }

    private fun isRetryable(error: java.io.IOException): Boolean {
        return when (error) {
            is ConnectException,
            is SocketException,
            is SocketTimeoutException -> true
            is UnknownHostException -> false
            is InterruptedIOException -> false
            else -> false
        }
    }
}
