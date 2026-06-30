# Vosk Wake Word Migration Spec

## Goal
Replace Picovoice Porcupine with Vosk keyword spotting for "Hey Q" wake word detection. Removes API key dependency, usage limits, and simplifies reinit after overlay closes.

---

## Prerequisites

### 1. Download Vosk Model
- Model: `vosk-model-small-en-us-0.15` (~50MB)
- URL: https://alphacephei.com/vosk/models
- Extract to: `Qareeb App/app/src/main/assets/model-en-us/`
- Contents should be: `am/`, `conf/`, `graph/`, `ivector/` dirs + various config files

### 2. Verify Dependency
Already present in `build.gradle.kts:107`:
```
implementation("com.alphacephei:vosk-android:0.3.47")
```

---

## Files to Modify

### File 1: `build.gradle.kts`

**Remove:**
```kotlin
implementation("ai.picovoice:porcupine-android:4.0.0")  // line 105
```

**Keep:**
```kotlin
implementation("com.alphacephei:vosk-android:0.3.47")    // line 107
```

---

### File 2: `QareebListeningService.kt` — Full Rewrite

**Remove all Porcupine imports:**
```kotlin
// DELETE: import ai.picovoice.porcupine.PorcupineManager
```

**Add Vosk imports:**
```kotlin
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.RecognitionListener
```

**Implementation:**

```kotlin
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

    // Vosk components
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

    /**
     * Copies model-en-us/ directory tree from assets to filesDir.
     * Vosk needs a real filesystem path, not an asset URI.
     */
    private fun unpackModel(targetDir: File) {
        targetDir.mkdirs()
        copyAssetDir("model-en-us", targetDir)
    }

    private fun copyAssetDir(assetPath: String, targetDir: File) {
        val list = assets.list(assetPath) ?: return

        if (list.isEmpty()) {
            // It's a file — copy it
            assets.open(assetPath).use { input ->
                FileOutputStream(File(targetDir, "")).use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        // It's a directory — recurse
        for (entry in list) {
            val childAssetPath = "$assetPath/$entry"
            val childFiles = assets.list(childAssetPath)

            if (childFiles != null && childFiles.isNotEmpty()) {
                // Subdirectory
                val subDir = File(targetDir, entry)
                subDir.mkdirs()
                copyAssetDir(childAssetPath, subDir)
            } else {
                // File
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
            // Create recognizer with grammar — restricts to wake word only
            // "[unk]" handles all non-matching audio silently
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
        // Restart listening after error
        isListening = false
        serviceScope.launch { startListening() }
    }

    override fun onTimeout() {
        // Restart listening on timeout
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

            // KEY ADVANTAGE: No reinit dance needed!
            // Just start listening again.
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
```

---

### File 3: `QareebOverlay.kt` — Minor Change

**In the overlay close callback (already handled above), no Porcupine reinit needed.**

No changes required — the callback lambda in `QareebListeningService` already just calls `startListening()`.

---

### File 4: `MainActivity.kt` — No Changes

`startQareebService()` just starts the service. No Porcupine references.

---

### File 5: `AndroidManifest.xml` — No Changes

Permissions (`RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`) already cover Vosk. No new permissions needed.

---

## Assets Changes

### Remove:
```
app/src/main/assets/hey-q.ppn
app/src/main/assets/Hi-q_en_android_v3_0_0.ppn
app/src/main/assets/hey-Q_en_android_v3_0_0.ppn
```

### Add:
```
app/src/main/assets/model-en-us/
├── am/
│   └── final.mdl
├── conf/
│   ├── mfcc.conf
│   └── model.conf
├── graph/
│   ├── disambig_tid.int
│   ├── Gr.fst
│   ├── HCLr.fst
│   └── phones/
│       └── word_boundary.int
├── ivector/
│   ├── final.dubm
│   ├── final.ie
│   ├── final.mat
│   ├── global_cmvn.stats
│   ├── online_cmvn.conf
│   └── splice.conf
└── (other model files)
```

---

## Behavior Changes

| Aspect | Before (Porcupine) | After (Vosk) |
|--------|-------------------|--------------|
| Wake word trigger | `.ppn` model file | Grammar string `"hey q"` |
| API key | Required (`ACCESS_KEY`) | None |
| Model size in APK | ~2MB | ~50MB |
| Battery usage | Very low | Moderate (full ASR engine) |
| After overlay close | delete → delay 500ms → rebuild → delay 200ms → start | Just `startListening()` |
| Error recovery | Manual reinit | Auto-restart on error/timeout |
| False positive rate | Low | Slightly higher (mitigated by grammar mode) |

---

## Testing Checklist

- [ ] Vosk model unpacks correctly on first launch
- [ ] "Hey Q" detected in quiet environment
- [ ] "Hey Q" detected with moderate background noise
- [ ] Overlay appears after detection
- [ ] Listening resumes after overlay closes
- [ ] No crash on rapid open/close overlay cycles
- [ ] Service survives app backgrounding
- [ ] Service restarts after kill (START_STICKY)
- [ ] Battery usage acceptable for demo duration
- [ ] No false positives on normal conversation
- [ ] Porcupine dependency fully removed (build succeeds without it)

---

## Rollback Plan

If Vosk accuracy/battery is unacceptable:
1. Revert `QareebListeningService.kt` to Porcupine version
2. Re-add `ai.picovoice:porcupine-android:4.0.0` to gradle
3. Restore `.ppn` asset files
4. Remove `model-en-us/` from assets

Git branch recommended: `feature/vosk-wake-word`

---

## Estimated Effort

| Task | Time |
|------|------|
| Download + place model | 5 min |
| Replace `QareebListeningService.kt` | Copy from spec |
| Remove Porcupine from gradle | 1 min |
| Delete `.ppn` assets | 1 min |
| Build + test | 15 min |
| **Total** | **~25 min** |
