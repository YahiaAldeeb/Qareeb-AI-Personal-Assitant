package com.example.qareeb

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
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.RecognitionListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class QareebListeningService : Service(), RecognitionListener {

    private val CHANNEL_ID = "QareebServiceChannel"
    private val NOTIF_ID = 1
    private val TAG = "QAREEB_DEBUG"

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var isListening = false
    @Volatile private var overlayVisible = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, createNotification())

        serviceScope.launch {
            withContext(Dispatchers.IO) { initModel() }
            startListening()
        }

        return START_STICKY
    }

    // ── Model Init ──

    private fun initModel() {
        if (model != null) return

        val modelDir = File(filesDir, "model-en-us")

        if (!modelDir.exists()) {
            Log.d(TAG, "Unpacking Vosk model from assets...")
            unpackModel(modelDir)
        }

        model = Model(modelDir.absolutePath)
        Log.d(TAG, "Vosk model loaded")
    }

    private fun unpackModel(targetDir: File) {
        targetDir.mkdirs()
        copyAssetDir("model-en-us", targetDir)
    }

    private fun copyAssetDir(assetPath: String, targetDir: File) {
        val list = assets.list(assetPath) ?: return

        if (list.isEmpty()) {
            assets.open(assetPath).use { input ->
                FileOutputStream(File(targetDir, "")).use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        for (entry in list) {
            val childAssetPath = "$assetPath/$entry"
            val childFiles = assets.list(childAssetPath)

            if (childFiles != null && childFiles.isNotEmpty()) {
                val subDir = File(targetDir, entry)
                subDir.mkdirs()
                copyAssetDir(childAssetPath, subDir)
            } else {
                assets.open(childAssetPath).use { input ->
                    FileOutputStream(File(targetDir, entry)).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    // ── Listening ──

    private fun startListening() {
        if (isListening || model == null) return

        try {
            val rec = Recognizer(model, 16000.0f, "[\"hey q\", \"hey queue\", \"[unk]\"]")

            speechService = SpeechService(rec, 16000.0f).also {
                it.startListening(this)
            }

            isListening = true
            Log.d(TAG, "Vosk listening started (grammar mode)")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start Vosk: ${e.message}", e)
        }
    }

    private fun stopListening() {
        speechService?.stop()
        speechService = null
        isListening = false
        Log.d(TAG, "Vosk listening stopped")
    }

    // ── RecognitionListener callbacks ──

    override fun onPartialResult(hypothesis: String?) {
        if (hypothesis == null) return
        checkForWakeWord(hypothesis)
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis == null) return
        checkForWakeWord(hypothesis)
    }

    override fun onFinalResult(hypothesis: String?) {
        // Not used in continuous listening mode
    }

    override fun onError(exception: Exception?) {
        Log.e(TAG, "Vosk recognition error: ${exception?.message}", exception)
        isListening = false
        serviceScope.launch { startListening() }
    }

    override fun onTimeout() {
        isListening = false
        serviceScope.launch { startListening() }
    }

    // ── Wake Word Detection ──

    private fun checkForWakeWord(hypothesis: String) {
        try {
            val text = JSONObject(hypothesis).optString("text", "")
                .lowercase().trim()

            if (text.contains("hey q") || text.contains("hey queue")) {
                Log.d(TAG, "Wake word detected! text='$text'")
                stopListening()
                serviceScope.launch(Dispatchers.Main) {
                    onWakeWordDetected()
                }
            }
        } catch (e: Exception) {
            // Malformed JSON — ignore
        }
    }

    // ── Overlay ──

    private fun onWakeWordDetected() {
        if (overlayVisible) return

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            serviceScope.launch { startListening() }
            return
        }

        overlayVisible = true

        val sessionManager = SessionManager.getInstance(this)

        val db = AppDatabase.getDatabase(applicationContext)
        val syncRepository = SyncRepository(
            taskDao = db.taskDao(),
            userDao = db.userDao(),
            transactionDao = db.transactionDao(),
            promptDao = db.promptDao(),
            memoryDao = db.memoryDao(),
            api = RetrofitInstance.syncApi,
            prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
        )

        val overlay = QareebOverlay(this, sessionManager, syncRepository) {
            Log.d(TAG, "Overlay closed. Restarting Vosk listening...")
            overlayVisible = false
            serviceScope.launch { startListening() }
        }

        overlay.show()
    }

    // ── Notification ──

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
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

    // ── Lifecycle ──

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy()")
        stopListening()
        model?.close()
        model = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
