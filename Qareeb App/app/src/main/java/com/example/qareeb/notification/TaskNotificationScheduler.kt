package com.example.qareeb.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.qareeb.data.entity.Task

object TaskNotificationScheduler {

    private const val TWO_HOURS_MS = 1 * 60 * 1000L  // 2 hours in milliseconds

    fun schedule(context: Context, task: Task) {
        val dueDate = task.dueDate ?: return  // no due date → nothing to schedule

        val notifyAt = dueDate - TWO_HOURS_MS
        val now = System.currentTimeMillis()

        // Don't schedule if the notification time has already passed
        if (notifyAt <= now) {
            Log.d("NOTIF", "Skipping ${task.title} — notification time already passed")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("task_id", task.taskId)
            putExtra("task_title", task.title)
            putExtra("task_description", task.description ?: "Your task is due in 2 hours")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.hashCode(),  // unique per task
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12+ requires exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notifyAt,
                    pendingIntent
                )
                Log.d("NOTIF", "Scheduled notification for ${task.title} at $notifyAt")
            } else {
                Log.w("NOTIF", "Exact alarm permission not granted")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notifyAt,
                pendingIntent
            )
            Log.d("NOTIF", "Scheduled notification for ${task.title} at $notifyAt")
        }
    }

    fun cancel(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("NOTIF", "Cancelled notification for ${task.title}")
    }
}