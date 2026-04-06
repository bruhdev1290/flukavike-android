package com.fluxer.client.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fluxer.client.MainActivity
import com.fluxer.client.R
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
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token: $token")
        
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("Received FCM message: ${message.data}")
        
        val data = message.data
        val notificationType = data["type"] ?: "message"
        val title = data["title"] ?: "New Message"
        val body = data["body"] ?: ""
        val channelId = data["channel_id"]
        val senderId = data["sender_id"]
        val messageId = data["message_id"]
        
        when (notificationType) {
            "direct_message" -> showDMNotification(title, body, channelId, senderId, messageId)
            "mention" -> showMentionNotification(title, body, channelId, senderId, messageId)
            "call" -> showCallNotification(title, body, data["call_id"])
            "call_missed" -> showMissedCallNotification(title, body, data["call_id"])
            else -> showDefaultNotification(title, body, channelId)
        }
    }

    private fun showDMNotification(
        title: String,
        body: String,
        channelId: String?,
        senderId: String?,
        messageId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "dm")
            putExtra("channel_id", channelId)
            putExtra("sender_id", senderId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_DM_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showMentionNotification(
        title: String,
        body: String,
        channelId: String?,
        senderId: String?,
        messageId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "mention")
            putExtra("channel_id", channelId)
            putExtra("message_id", messageId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_MENTIONS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⭐ $title")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFE63946.toInt())
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showCallNotification(title: String, body: String, callId: String?) {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "incoming_call")
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "accept_call")
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val declineIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "decline_call")
        }
        val declinePendingIntent = PendingIntent.getActivity(
            this, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📞 $title")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "Accept", acceptPendingIntent)
            .addAction(0, "Decline", declinePendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun showMissedCallNotification(title: String, body: String, callId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "call_missed")
            putExtra("call_id", callId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📞 Missed Call")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showDefaultNotification(title: String, body: String, channelId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_GENERAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val dmChannel = NotificationChannel(
                CHANNEL_DM_ID, "Direct Messages", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for direct messages"
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            
            val mentionsChannel = NotificationChannel(
                CHANNEL_MENTIONS_ID, "Mentions", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when you are mentioned"
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            
            val callsChannel = NotificationChannel(
                CHANNEL_CALLS_ID, "Calls", NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Incoming call notifications"
                setSound(android.provider.Settings.System.DEFAULT_RINGTONE_URI, 
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID, "General", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
            }
            
            notificationManager.createNotificationChannels(
                listOf(dmChannel, mentionsChannel, callsChannel, generalChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_DM_ID = "fluxer_dm"
        const val CHANNEL_MENTIONS_ID = "fluxer_mentions"
        const val CHANNEL_CALLS_ID = "fluxer_calls"
        const val CHANNEL_GENERAL_ID = "fluxer_general"
        const val CALL_NOTIFICATION_ID = 9999
    }
}
