package com.fluxer.client.service

import android.content.Context
import com.fluxer.client.data.repository.NotificationRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.MessagingReceiver
import timber.log.Timber

/**
 * UnifiedPush messaging receiver for handling push notifications
 * without relying on Google Play Services / FCM.
 */
class FluxerUnifiedPushReceiver : MessagingReceiver() {

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        Timber.d("Received UnifiedPush message for instance: $instance")
        try {
            val payload = String(message, Charsets.UTF_8)
            Timber.d("UnifiedPush payload: $payload")

            // Parse JSON payload into map
            val data = parsePayload(payload)
            FluxerNotificationHandler.showNotification(context, data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle UnifiedPush message")
        }
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        Timber.d("UnifiedPush new endpoint for instance: $instance")
        val notificationRepository = getNotificationRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.registerPushToken(
                token = endpoint,
                provider = "unifiedpush",
                instance = instance
            )
        }
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        Timber.e("UnifiedPush registration failed for instance: $instance")
    }

    override fun onUnregistered(context: Context, instance: String) {
        Timber.d("UnifiedPush unregistered for instance: $instance")
        val notificationRepository = getNotificationRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.unregisterPushToken()
        }
    }

    private fun getNotificationRepository(context: Context): NotificationRepository {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FluxerNotificationEntryPoint::class.java
        )
        return entryPoint.notificationRepository()
    }

    private fun parsePayload(payload: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            // Simple JSON parsing for flat string-key/string-value maps
            // Remove outer braces and split by commas not inside quotes
            val cleaned = payload.trim().removePrefix("{").removeSuffix("}")
            val regex = """"([^"]+)"\s*:\s*"([^"]*)"""".toRegex()
            regex.findAll(cleaned).forEach { match ->
                val key = match.groupValues[1]
                val value = match.groupValues[2]
                result[key] = value
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse UnifiedPush payload")
        }
        return result
    }
}
