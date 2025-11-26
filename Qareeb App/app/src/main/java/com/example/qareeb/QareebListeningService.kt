package com.example.qareeb

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class QareebListeningService : Service() {

    private val CHANNEL_ID = "QareebServiceChannel"
    private var porcupineManager: PorcupineManager? = null
    private val TAG = "QAREEB_DEBUG"

    // PASTE YOUR KEY HERE
    private val ACCESS_KEY = "BaoJLQKkfbjG+Xn4aYfa3altQ1tXbAgwCL1NFu2WEuti33cYmnM5JQ=="

    // YOUR EXACT FILE NAME
    private val MODEL_FILENAME = "Hi-q_en_android_v3_0_0.ppn"

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "Service onCreate called")
        createNotificationChannel()
        initPorcupine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "Service onStartCommand called")
        val notification = createNotification()
        startForeground(1, notification)

        try {
            porcupineManager?.start()
            Log.e(TAG, "Porcupine Started Successfully!")
        } catch (e: PorcupineException) {
            Log.e(TAG, "CRITICAL ERROR STARTING PORCUPINE: ${e.message}")
            e.printStackTrace()
        }

        return START_STICKY
    }

    private fun initPorcupine() {
        try {
            // 1. EXTRACT THE FILE FROM ASSETS TO STORAGE
//            val modelPath = extractAssetToStorage(applicationContext, MODEL_FILENAME)

            // 2. USE THE EXTRACTED PATH
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath("Hi-q_en_android_v3_0_0.ppn") // We pass the storage path, not the asset name
                .setSensitivity(0.7f)
                .build(applicationContext) { keywordIndex ->
                    if (keywordIndex == 0) {
                        Log.e(TAG, "HI Q DETECTED!")
                        onWakeWordDetected()
                    }
                }
            Log.e(TAG, "Porcupine Initialized with Custom Model")

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR INIT PORCUPINE: ${e.message}")
            e.printStackTrace()
        }
    }

    // --- NEW HELPER FUNCTION ---
    // Copies the .ppn file from the APK assets to the phone's internal storage
//    private fun extractAssetToStorage(context: Context, filename: String): String {
//        // Create a file in the app's private storage
//        val file = File(context.filesDir, filename)
//
//        // Only extract if it doesn't exist (to save time)
//        if (!file.exists()) {
//            try {
//                context.assets.open(filename).use { inputStream ->
//                    FileOutputStream(file).use { outputStream ->
//                        inputStream.copyTo(outputStream)
//                    }
//                }
//                Log.e(TAG, "Asset extracted to: ${file.absolutePath}")
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to extract asset: ${e.message}")
//                throw e
//            }
//        }
//        return file.absolutePath
//    }

    private fun onWakeWordDetected() {
        if (Settings.canDrawOverlays(this)) {
            val overlay = QareebOverlay(this)
            overlay.show()
        } else {
            Log.e(TAG, "Wake word heard, but NO OVERLAY PERMISSION")
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qareeb Active")
            .setContentText("Listening for 'Hi Q'...")
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