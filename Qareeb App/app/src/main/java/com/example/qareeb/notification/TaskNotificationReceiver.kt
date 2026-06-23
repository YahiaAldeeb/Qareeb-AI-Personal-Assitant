package com.example.qareeb.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.qareeb.MainActivity
import com.example.qareeb.R

class TaskNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("task_title") ?: "Task Reminder"
        val taskDescription = intent.getStringExtra("task_description") ?: "Your task is due in 2 hours"
        val taskId = intent.getStringExtra("task_id") ?: return

        // Tap notification → open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.qareeb)       // your app icon
            .setContentTitle("⏰ $taskTitle")
            .setContentText(taskDescription)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your task \"$taskTitle\" is due in 2 hours. Stay on track!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "qareeb_task_reminder"
    }
}