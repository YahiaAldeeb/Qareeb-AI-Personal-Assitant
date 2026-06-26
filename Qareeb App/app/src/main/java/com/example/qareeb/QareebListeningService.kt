package com.example.qareeb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class QareebListeningService : Service() {

    private val CHANNEL_ID = "QareebServiceChannel"
    private val NOTIF_ID = 1
    private val TAG = "QAREEB_DEBUG"

    companion object {
        const val ACTION_ACTIVATE = "com.example.qareeb.ACTION_ACTIVATE"
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE_FACTOR = 2
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var isListening = false
    @Volatile private var overlayVisible = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, createNotification())

        if (intent?.action == ACTION_ACTIVATE) {
            Log.d(TAG, "onStartCommand: notification tap → activating overlay")
            serviceScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) { initModel() }
                stopListening()
                onWakeWordDetected()
            }
            return START_STICKY
        }

        serviceScope.launch {
            try {
                withContext(Dispatchers.IO) { initModel() }
                if (!overlayVisible) {
                    Log.d(TAG, "onStartCommand: ensuring Vosk active (isListening=$isListening)")
                    stopListening()
                    startListening()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Service startup failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QareebListeningService, "Vosk FAILED: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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

    // ── Listening (own AudioRecord, like Porcupine did) ──

    private fun startListening() {
        if (isListening || model == null) return

        try {
            recognizer?.close()
            recognizer = Recognizer(model, SAMPLE_RATE.toFloat())

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * BUFFER_SIZE_FACTOR

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize (state=${recorder.state})")
                recorder.release()
                retryListening()
                return
            }

            audioRecord = recorder
            recorder.startRecording()
            isListening = true
            Log.d(TAG, "Vosk listening started (VOICE_RECOGNITION source, own AudioRecord)")

            recordingThread = Thread({
                val buffer = ShortArray(bufferSize / 2)
                var silentReads = 0
                while (isListening) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var maxAmp: Short = 0
                        for (i in 0 until read) {
                            val abs = if (buffer[i] < 0) (-buffer[i]).toShort() else buffer[i]
                            if (abs > maxAmp) maxAmp = abs
                        }

                        if (maxAmp == 0.toShort()) {
                            silentReads++
                            if (silentReads == 50) {
                                Log.w(TAG, "AudioRecord dead (50 silent reads). Forcing restart.")
                                mainHandler.post {
                                    stopListening()
                                    retryCount = 0
                                    startListening()
                                }
                                return@Thread
                            }
                        } else {
                            silentReads = 0
                        }

                        val rec = recognizer ?: break
                        if (rec.acceptWaveForm(buffer, read)) {
                            val result = rec.result
                            processHypothesis(result)
                        } else {
                            val partial = rec.partialResult
                            processHypothesis(partial)
                        }
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord.read() returned error: $read")
                        break
                    }
                }
                Log.d(TAG, "Recording thread exiting")
            }, "VoskAudioThread").also { it.start() }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Vosk: ${e.message}", e)
            isListening = false
            retryListening()
        }
    }

    private var retryCount = 0

    private fun retryListening() {
        if (retryCount < 5) {
            retryCount++
            val delay = 1000L * retryCount
            Log.d(TAG, "Retrying Vosk in ${delay}ms (attempt $retryCount)")
            mainHandler.postDelayed({ startListening() }, delay)
        } else {
            Log.e(TAG, "Vosk failed after $retryCount retries, giving up")
        }
    }

    private fun stopListening() {
        isListening = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordingThread?.join(2000)
        recordingThread = null
        recognizer?.close()
        recognizer = null
        Log.d(TAG, "Vosk listening stopped")
    }

    // ── Recognition Processing ──

    private fun processHypothesis(json: String?) {
        if (json == null) return
        try {
            val text = JSONObject(json).optString("text", "")
                .lowercase().trim()

            if (text.isEmpty()) return
            Log.v(TAG, "Vosk heard: '$text'")

            if (wakePatterns.any { text.contains(it) }) {
                Log.d(TAG, "Wake word detected! text='$text'")
                mainHandler.post {
                    stopListening()
                    onWakeWordDetected()
                }
            }
        } catch (_: Exception) {}
    }

    // ── Wake Word Detection ──

    private val wakePatterns = listOf(
        "hey q", "hey queue", "hey cue", "hey cu",
        "hey cute", "hey qu", "a q", "hay q",
        "hey key", "hey kew", "heyq", "hey cool"
    )

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
            Log.d(TAG, "onDismiss fired. isListening=$isListening, model=${model != null}")
            overlayVisible = false
            retryCount = 0
            mainHandler.postDelayed({
                Log.d(TAG, "onDismiss: calling startListening()")
                startListening()
                Log.d(TAG, "onDismiss: startListening() returned, isListening=$isListening")
            }, 1500)
        }

        overlay.show()
    }

    // ── Notification ──

    private fun createNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val activateIntent = Intent(this, QareebListeningService::class.java).apply {
            action = ACTION_ACTIVATE
        }
        val activatePending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 1, activateIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 1, activateIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qareeb Active")
            .setContentText("Listening... Tap 'Hey Q' if voice isn't working")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "Hey Q", activatePending)
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
