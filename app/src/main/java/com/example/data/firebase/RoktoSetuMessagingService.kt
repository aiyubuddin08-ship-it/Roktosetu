package com.example.data.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Production Firebase Cloud Messaging Service for RoktoSetu.
 *
 * Responsibilities:
 * - Persists refreshed FCM registration tokens (`onNewToken`) into `/users/{uid}.fcmToken`
 *   in the custom Firestore database so Cloud Functions (`onBloodRequestCreated`,
 *   `onChatMessageCreated`) can deliver real push notifications.
 * - Renders high-priority notifications for emergency blood broadcasts and chat replies
 *   while respecting Android 13+ `POST_NOTIFICATIONS` runtime permission.
 */
class RoktoSetuMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        } ?: return

        try {
            val dbId = applicationContext.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            firestore.collection("users")
                .document(currentUser.uid)
                .update(
                    mapOf(
                        "fcmToken" to token.take(500),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .addOnFailureListener { e ->
                    Log.w(TAG, "Deferred FCM token update until profile creation: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to persist refreshed FCM token: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        ensureNotificationChannel(applicationContext)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.fcm_default_notification_title)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: getString(R.string.fcm_default_notification_body)
        val type = remoteMessage.data["type"] ?: "GENERAL"
        val requestId = remoteMessage.data["requestId"] ?: remoteMessage.data["targetId"] ?: ""
        val conversationId = remoteMessage.data["conversationId"] ?: ""

        showNotification(title, body, type, requestId, conversationId)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        requestId: String = "",
        conversationId: String = ""
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Skipping push notification display: POST_NOTIFICATIONS not granted")
                return
            }
        }

        val openRoute = when {
            type.contains("CHAT", ignoreCase = true) || conversationId.isNotBlank() -> "chat_list"
            requestId.isNotBlank() || type.contains("EMERGENCY", ignoreCase = true) || type.contains("REQUEST", ignoreCase = true) -> "request_details"
            else -> ""
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            if (openRoute.isNotBlank()) {
                putExtra("open_route", openRoute)
            }
            if (requestId.isNotBlank()) {
                putExtra("target_id", requestId)
            } else if (conversationId.isNotBlank()) {
                putExtra("target_id", conversationId)
            }
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (type + requestId + conversationId).hashCode(),
            launchIntent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException posting notification: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RoktoSetuFCM"
        const val CHANNEL_ID_EMERGENCY = "roktosetu_emergency_alerts"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val existing = manager.getNotificationChannel(CHANNEL_ID_EMERGENCY)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID_EMERGENCY,
                        context.getString(R.string.fcm_channel_emergency_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.fcm_channel_emergency_desc)
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }
}
