package com.example.qareeb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class QareebFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        val title =
            remoteMessage.notification?.title ?: "Qareeb"

        val body =
            remoteMessage.notification?.body ?: ""

        val suggestion =
            remoteMessage.data["suggestion"] ?: ""

        val actionType =
            remoteMessage.data["action"] ?: ""

        val userID =
            remoteMessage.data["userID"] ?: ""

        Log.d(
            "FCM",
            "Notification: title=$title, body=$body"
        )

        Log.d(
            "FCM",
            "Data: action=$actionType, suggestion=$suggestion"
        )

        showNotification(
            title,
            body,
            suggestion,
            actionType,
            userID
        )
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "New token: $token")
    }

    private fun showNotification(
        title: String,
        body: String,
        suggestion: String,
        actionType: String,
        userID: String
    ) {

        val channelId = "qareeb_reminders"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        // =========================
        // CREATE CHANNEL
        // =========================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Qareeb Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "Qareeb task reminders and suggestions"

                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(channel)
        }

        // =========================
        // UNIQUE IDS
        // =========================

        val uniqueId = System.currentTimeMillis().toInt()

        // =========================
        // OPEN APP INTENT
        // =========================

        val openIntent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            putExtra("suggestion", suggestion)
            putExtra("action", actionType)
            putExtra("userID", userID)
            putExtra("from_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            uniqueId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // =========================
        // YES ACTION
        // =========================

        val yesIntent = Intent(
            this,
            NotificationActionReceiver::class.java
        ).apply {

            action = "ACTION_YES"

            putExtra("suggestion", suggestion)
            putExtra("userID", userID)
            putExtra("notification_id", uniqueId)

            // ✅ make intent unique
            data = android.net.Uri.parse(
                "qareeb://yes/$uniqueId"
            )
        }

        val yesPendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueId + 1000,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // =========================
        // NO ACTION
        // =========================

        val noIntent = Intent(
            this,
            NotificationActionReceiver::class.java
        ).apply {

            action = "ACTION_NO"

            putExtra("suggestion", suggestion)
            putExtra("notification_id", uniqueId)

            data = android.net.Uri.parse(
                "qareeb://no/$uniqueId"
            )
        }

        val noPendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueId + 2000,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // =========================
        // BUILD NOTIFICATION
        // =========================

        val notification =
            NotificationCompat.Builder(this, channelId)

                .setSmallIcon(android.R.drawable.ic_dialog_info)

                .setContentTitle(title)

                .setContentText(body)

                .setAutoCancel(true)

                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )

                .setContentIntent(pendingIntent)

                .addAction(
                    android.R.drawable.ic_input_add,
                    "Yes, add it!",
                    yesPendingIntent
                )

                .addAction(
                    android.R.drawable.ic_delete,
                    "No thanks",
                    noPendingIntent
                )

                .build()

        // =========================
        // SHOW NOTIFICATION
        // =========================

        notificationManager.notify(
            uniqueId,
            notification
        )

        Log.d(
            "FCM",
            "Notification displayed with unique actions"
        )
    }
}