package com.fluxer.client.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.EntryPointAccessors
import com.fluxer.client.MainActivity
import com.fluxer.client.R
import com.fluxer.client.data.model.NotificationData
import com.fluxer.client.util.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Shared notification handler used by both FCM and UnifiedPush.
 */
object FluxerNotificationHandler {

    const val CHANNEL_DM_ID = "fluxer_dm"
    const val CHANNEL_MENTIONS_ID = "fluxer_mentions"
    const val CHANNEL_CALLS_ID = "fluxer_calls"
    const val CHANNEL_GENERAL_ID = "fluxer_general"
    const val CALL_NOTIFICATION_ID = 9999

    fun showNotification(context: Context, data: Map<String, String>) {
        val notificationType = data["type"] ?: "message"
        val settings = NotificationPreferences.get(context)
        val enabled = settings.globalEnabled && when (notificationType) {
            "direct_message" -> settings.dmNotifications
            "mention" -> settings.mentionNotifications
            "call", "call_missed" -> settings.callNotifications
            "friend_request" -> settings.friendRequestNotifications
            else -> true
        }
        if (!enabled) return

        val title = data["title"] ?: "New Message"
        val body = if (settings.showPreview) data["body"].orEmpty() else "Open Fluxer to view"
        val channelId = data["channel_id"]
        val guildId = data["guild_id"]
        val senderId = data["sender_id"]
        val messageId = data["message_id"]
        val callId = data["call_id"]

        persistNotification(
            context = context,
            id = data["id"] ?: data["notification_id"] ?: "${notificationType}_${messageId ?: channelId ?: System.currentTimeMillis()}",
            type = notificationType,
            title = title,
            body = body,
            data = NotificationData(
                channelId = channelId,
                guildId = guildId,
                messageId = messageId,
                senderId = senderId,
                callId = callId,
                url = data["url"]
            )
        )

        when (notificationType) {
            "direct_message" -> showDMNotification(context, title, body, channelId, senderId, messageId)
            "mention" -> showMentionNotification(context, title, body, guildId, channelId, senderId, messageId)
            "call" -> showCallNotification(context, title, body, callId)
            "call_missed" -> showMissedCallNotification(context, title, body, callId)
            else -> showDefaultNotification(context, title, body, guildId, channelId, messageId)
        }
    }

    private fun persistNotification(
        context: Context,
        id: String,
        type: String,
        title: String,
        body: String,
        data: NotificationData
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FluxerNotificationEntryPoint::class.java
        )
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                entryPoint.notificationRepository().recordNotification(
                    id = id,
                    type = type,
                    title = title,
                    body = body,
                    data = data
                )
            }.onFailure { error ->
                Timber.e(error, "Failed to persist notification feed item")
            }
        }
    }

    private fun showDMNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String?,
        senderId: String?,
        messageId: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "dm")
            putExtra("channel_id", channelId)
            putExtra("sender_id", senderId)
            putExtra("message_id", messageId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DM_ID)
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showMentionNotification(
        context: Context,
        title: String,
        body: String,
        guildId: String?,
        channelId: String?,
        senderId: String?,
        messageId: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "mention")
            putExtra("guild_id", guildId)
            putExtra("channel_id", channelId)
            putExtra("sender_id", senderId)
            putExtra("message_id", messageId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MENTIONS_ID)
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showCallNotification(context: Context, title: String, body: String, callId: String?) {
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "incoming_call")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "accept_call")
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "decline_call")
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun showMissedCallNotification(context: Context, title: String, body: String, callId: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "call_missed")
            putExtra("call_id", callId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📞 Missed Call")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showDefaultNotification(
        context: Context,
        title: String,
        body: String,
        guildId: String?,
        channelId: String?,
        messageId: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "message")
            putExtra("guild_id", guildId)
            putExtra("channel_id", channelId)
            putExtra("message_id", messageId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                setSound(
                    android.provider.Settings.System.DEFAULT_RINGTONE_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
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
            Timber.d("Notification channels created")
        }
    }
}
