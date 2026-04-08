package com.fluxer.client.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.fluxer.client.MainActivity
import com.fluxer.client.R

/**
 * Helper class for managing on-device notifications
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Show a direct message notification with rich features
     */
    fun showMessageNotification(
        title: String,
        message: String,
        senderName: String,
        channelId: String,
        messageId: String? = null,
        senderAvatar: Bitmap? = null,
        isGroup: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "message")
            putExtra("channel_id", channelId)
            putExtra("message_id", messageId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(channelId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build messaging style notification
        val person = Person.Builder()
            .setName(senderName)
            .setIcon(senderAvatar?.let { IconCompat.createWithBitmap(it) })
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(if (isGroup) title else null)
            .addMessage(message, timestamp, person)

        // Add direct reply action
        val replyLabel = "Reply"
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REPLY
            putExtra("channel_id", channelId)
            putExtra("notification_id", generateNotificationId(channelId))
        }

        val replyPendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(channelId),
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Mark as read action
        val markReadIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_MARK_READ
            putExtra("channel_id", channelId)
            putExtra("notification_id", generateNotificationId(channelId))
        }

        val markReadPendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(channelId) + 1,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Mark as Read",
            markReadPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setShortcutId(channelId)
            .setLocusId(androidx.core.content.LocusIdCompat(channelId))
            .build()

        notificationManager.notify(generateNotificationId(channelId), notification)
    }

    /**
     * Show a mention notification
     */
    fun showMentionNotification(
        title: String,
        message: String,
        channelId: String,
        channelName: String,
        senderName: String,
        senderAvatar: Bitmap? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "mention")
            putExtra("channel_id", channelId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            generateNotificationId(channelId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val person = Person.Builder()
            .setName(senderName)
            .setIcon(senderAvatar?.let { IconCompat.createWithBitmap(it) })
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle("$channelName - Mention")
            .addMessage(message, System.currentTimeMillis(), person)

        val notification = NotificationCompat.Builder(context, CHANNEL_MENTIONS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⭐ Mention in $channelName")
            .setContentText("$senderName: $message")
            .setStyle(messagingStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFE63946.toInt())
            .build()

        notificationManager.notify(generateNotificationId(channelId), notification)
    }

    /**
     * Show incoming call notification with full-screen intent
     */
    fun showIncomingCallNotification(
        callerName: String,
        callId: String,
        isVideo: Boolean = false
    ) {
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", "call")
            putExtra("call_id", callId)
            putExtra("action", "incoming_call")
            putExtra("is_video", isVideo)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept action
        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL
            putExtra("call_id", callId)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action
        val declineIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra("call_id", callId)
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context,
            2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isVideo) "📹 Video Call" else "📞 Incoming Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, "Accept", acceptPendingIntent)
            .addAction(R.drawable.ic_notification, "Decline", declinePendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .build()

        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    /**
     * Show a general app notification
     */
    fun showGeneralNotification(
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications for a channel
     */
    fun cancelChannelNotifications(channelId: String) {
        notificationManager.cancel(generateNotificationId(channelId))
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Messages channel
            createChannel(
                CHANNEL_MESSAGES_ID,
                "Messages",
                "Direct messages and channel notifications",
                NotificationManager.IMPORTANCE_HIGH,
                enableLights = true,
                lightColor = 0xFFE63946.toInt(),
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            )

            // Mentions channel
            createChannel(
                CHANNEL_MENTIONS_ID,
                "Mentions",
                "Notifications when you are mentioned",
                NotificationManager.IMPORTANCE_HIGH,
                enableLights = true,
                lightColor = 0xFFE63946.toInt(),
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            )

            // Calls channel
            createChannel(
                CHANNEL_CALLS_ID,
                "Calls",
                "Incoming voice and video calls",
                NotificationManager.IMPORTANCE_MAX,
                sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            )

            // General channel
            createChannel(
                CHANNEL_GENERAL_ID,
                "General",
                "General app notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(
        channelId: String,
        name: String,
        description: String,
        importance: Int,
        sound: Uri? = null,
        enableLights: Boolean = false,
        lightColor: Int = 0,
        vibrationPattern: LongArray? = null
    ) {
        val channel = NotificationChannel(channelId, name, importance).apply {
            this.description = description
            vibrationPattern?.let { 
                enableVibration(true)
                this.vibrationPattern = it
            }
            if (sound != null) {
                setSound(sound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            if (enableLights) {
                enableLights(true)
                this.lightColor = lightColor
            }
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Generate a consistent notification ID from a channel ID
     */
    private fun generateNotificationId(channelId: String): Int {
        return channelId.hashCode()
    }

    companion object {
        const val CHANNEL_MESSAGES_ID = "fluxer_messages"
        const val CHANNEL_MENTIONS_ID = "fluxer_mentions"
        const val CHANNEL_CALLS_ID = "fluxer_calls"
        const val CHANNEL_GENERAL_ID = "fluxer_general"
        
        const val CALL_NOTIFICATION_ID = 9999
        
        // Actions
        const val ACTION_REPLY = "com.fluxer.client.REPLY"
        const val ACTION_MARK_READ = "com.fluxer.client.MARK_READ"
        const val ACTION_ACCEPT_CALL = "com.fluxer.client.ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.fluxer.client.DECLINE_CALL"
        
        // Remote input key
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
