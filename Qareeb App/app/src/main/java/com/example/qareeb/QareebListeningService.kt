package com.example.qareeb

import ai.picovoice.porcupine.PorcupineManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class QareebListeningService : Service() {

    private val CHANNEL_ID = "QareebServiceChannel"
    private val NOTIF_ID = 1

    private val TAG = "QAREEB_DEBUG"

    // IMPORTANT: keep your real key here
    private val ACCESS_KEY =
        "8JyKVGnhuojA9vWl/gfyJfOb3HIzPSPV1sjk9MMXAerRbmaNn3/15w=="

    // This file should exist in app/src/main/assets/
    private val KEYWORD_ASSET_NAME = "hey-q.ppn"

    private var porcupineManager: PorcupineManager? = null

    // Service scope for background work
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Simple guards to prevent re-entrant starts/stops
    @Volatile private var isInitialized = false
    @Volatile private var isListening = false
    @Volatile private var isStarting = false
    @Volatile private var overlayVisible = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service onCreate()")
        // DO NOT init Porcupine here (could block main)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground MUST start immediately
        startForeground(NOTIF_ID, createNotification())

        // Heavy work in background
        serviceScope.launch {
            ensureInitializedAndStart()
        }

        return START_STICKY
    }

    private suspend fun ensureInitializedAndStart() {
        if (isStarting) return
        isStarting = true

        try {
            if (!isInitialized) {
                withContext(Dispatchers.IO) {
                    initPorcupineInBackground()
                }
                isInitialized = true
            }

            // Start listening (also off main)
            withContext(Dispatchers.Default) {
                startListeningInternal()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init/start Porcupine: ${e.message}", e)
        } finally {
            isStarting = false
        }
    }

    private fun initPorcupineInBackground() {
        // Ensure keyword file exists as a real file path
        val keywordPath = copyAssetToFilesIfNeeded(KEYWORD_ASSET_NAME).absolutePath

        Log.d(TAG, "Porcupine keyword path: $keywordPath")

        porcupineManager = PorcupineManager.Builder()
            .setAccessKey(ACCESS_KEY)
            .setKeywordPath(keywordPath)
            .setSensitivity(0.7f)
            .build(applicationContext) { keywordIndex ->
                if (keywordIndex == 0) {
                    Log.d(TAG, "Wake word detected!")

                    // Stop listening in background quickly
                    serviceScope.launch(Dispatchers.Default) {
                        stopListeningInternal()
                    }

                    // Show overlay on main
                    serviceScope.launch(Dispatchers.Main) {
                        onWakeWordDetected()
                    }
                }
            }

        Log.d(TAG, "Porcupine Initialized")
    }

    private fun onWakeWordDetected() {
        if (overlayVisible) return

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted, cannot show overlay.")
            // If no overlay, just restart listening safely
            serviceScope.launch(Dispatchers.Default) { startListeningInternal() }
            return
        }

        overlayVisible = true

        val sessionManager = SessionManager.getInstance(this)
        
        // Create SyncRepository for the overlay to trigger sync after voice commands
        val db = AppDatabase.getDatabase(applicationContext)
        val syncRepository = SyncRepository(
            taskDao = db.taskDao(),
            userDao = db.userDao(),
            transactionDao = db.transactionDao(),
            api = RetrofitInstance.syncApi,
            prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
        )
        
        val overlay = QareebOverlay(this, sessionManager, syncRepository) {
            Log.d(TAG, "Overlay closed. Restarting listening...")
            overlayVisible = false
            serviceScope.launch(Dispatchers.Default) {
                startListeningInternal()
            }
        }

        overlay.show()
    }

    // --- Listening controls (internal, thread-safe-ish) ---

    private fun startListeningInternal() {
        if (isListening) return
        try {
            porcupineManager?.start()
            isListening = true
            Log.d(TAG, "Porcupine Started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Porcupine: ${e.message}", e)
            isListening = false
        }
    }

    private fun stopListeningInternal() {
        if (!isListening) return
        try {
            porcupineManager?.stop()
            isListening = false
            Log.d(TAG, "Porcupine Stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}", e)
        }
    }

    // --- Asset copy helper ---

    private fun copyAssetToFilesIfNeeded(assetName: String): File {
        val outFile = File(filesDir, assetName)
        if (outFile.exists() && outFile.length() > 0) return outFile

        assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        return outFile
    }

    // --- Notification stuff ---

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qareeb Active")
            .setContentText("Listening...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Qareeb Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy()")

        serviceScope.launch(Dispatchers.Default) {
            try {
                stopListeningInternal()
                porcupineManager?.delete()
                porcupineManager = null
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up Porcupine: ${e.message}", e)
            }
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}