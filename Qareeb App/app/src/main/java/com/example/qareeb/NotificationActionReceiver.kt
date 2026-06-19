package com.example.qareeb

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // ✅ Get notification ID
        val notificationId =
            intent.getIntExtra("notification_id", -1)

        // ✅ Remove notification
        if (notificationId != -1) {

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            notificationManager.cancel(notificationId)

            Log.d(
                "FCM",
                "Notification cleared: id=$notificationId"
            )
        }

        when (intent.action) {

            "ACTION_YES" -> {

                val suggestion =
                    intent.getStringExtra("suggestion") ?: ""

                val userID =
                    intent.getStringExtra("userID") ?: ""

                Log.d(
                    "FCM",
                    "User said YES to suggestion: $suggestion"
                )

                CoroutineScope(Dispatchers.IO).launch {

                    try {

                        val client = OkHttpClient()

                        val json = JSONObject().apply {

                            put("text", suggestion)

                            put("userID", userID)
                        }

                        val body = json.toString()
                            .toRequestBody(
                                "application/json".toMediaType()
                            )

                        val request = Request.Builder()

                            .url("http://10.0.2.2:8000/api/ai/text")

                            .post(body)

                            .build()

                        val response =
                            client.newCall(request).execute()

                        Log.d(
                            "FCM",
                            "Task created: ${response.code}"
                        )

                        response.close()

                        // ✅ Open app and trigger sync
                        val openIntent = Intent(
                            context,
                            MainActivity::class.java
                        ).apply {

                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )

                            putExtra("trigger_sync", true)

                            putExtra("userID", userID)
                        }

                        context.startActivity(openIntent)

                        Log.d(
                            "FCM",
                            "App opened for sync"
                        )

                    } catch (e: Exception) {

                        Log.e(
                            "FCM",
                            "Failed to create task: ${e.message}",
                            e
                        )
                    }
                }
            }

            "ACTION_NO" -> {

                Log.d(
                    "FCM",
                    "User dismissed suggestion"
                )
            }
        }
    }
}