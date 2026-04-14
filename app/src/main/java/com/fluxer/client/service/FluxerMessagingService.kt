package com.fluxer.client.service

import android.content.Context
import com.fluxer.client.data.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FluxerMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        FluxerNotificationHandler.createNotificationChannels(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token: $token")

        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.registerPushToken(
                token = token,
                provider = "fcm"
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("Received FCM message: ${message.data}")
        FluxerNotificationHandler.showNotification(this, message.data)
    }
}
