package com.example.qareeb

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

class QareebListeningService : Service() {

    private val CHANNEL_ID = "QareebServiceChannel"
    private var porcupineManager: PorcupineManager? = null
    private val TAG = "QAREEB_DEBUG"

    private val ACCESS_KEY = "BaoJLQKkfbjG+Xn4aYfa3altQ1tXbAgwCL1NFu2WEuti33cYmnM5JQ==" // Triple check this!
    private val MODEL_FILENAME = "Hi-q_en_android_v3_0_0.ppn"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initPorcupine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        startListening() // Helper function defined below
        return START_STICKY
    }

    private fun initPorcupine() {
        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath("Hi-q_en_android_v3_0_0.ppn")
                .setSensitivity(0.7f)
                .build(applicationContext) { keywordIndex ->
                    if (keywordIndex == 0) {
                        Log.e(TAG, "HI Q DETECTED!")

                        // 1. STOP LISTENING IMMEDIATELY
                        try {
                            porcupineManager?.stop()
                        } catch (e: Exception) { e.printStackTrace() }

                        // 2. Launch Overlay on Main Thread
                        val mainHandler = Handler(Looper.getMainLooper())
                        mainHandler.post {
                            onWakeWordDetected()
                        }
                    }
                }
            Log.e(TAG, "Porcupine Initialized")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR INIT PORCUPINE: ${e.message}")
            e.printStackTrace()
        }
    }
    private fun onWakeWordDetected() {
        if (Settings.canDrawOverlays(this)) {
            // We pass a "Callback" function: { startListening() }
            // This runs ONLY when the overlay closes.
            val overlay = QareebOverlay(this) {
                Log.e(TAG, "Overlay closed. Restarting Porcupine...")
                startListening()
            }
            overlay.show()
        }
    }

    // --- Helper Functions to Manage Mic State ---

    private fun startListening() {
        try {
            porcupineManager?.start()
            Log.d(TAG, "Porcupine Started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Porcupine: ${e.message}")
        }
    }

    private fun stopListening() {
        try {
            porcupineManager?.stop()
            Log.d(TAG, "Porcupine Stopped (Mic Released)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ... (Keep createNotification and createNotificationChannel exactly as before) ...
    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qareeb Active")
            .setContentText("Listening...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Qareeb Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        porcupineManager?.stop()
        porcupineManager?.delete()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}