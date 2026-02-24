package com.example.qareeb

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.data.dao.TransactionDao
import com.example.qareeb.data.entity.Task
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

enum class OverlayState {
    LISTENING,  // Waiting for user input
    THINKING,   // Processing/uploading
    SUCCESS,    // Task completed successfully
    FAILED      // Task failed
}

class QareebOverlay(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository,
    private val onDismiss: () -> Unit
) {
    
    private val db: AppDatabase = AppDatabase.getDatabase(context)
    private val transactionDao: TransactionDao = db.transactionDao()
    private val taskDao: TaskDao = db.taskDao()

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var overlayContainer: FrameLayout? = null

    private val recorder = AudioRecorderHelper(context)
    private var audioFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var currentState = OverlayState.LISTENING
    private var thinkingAnimator: ValueAnimator? = null

    // --- SMART TIMING VARIABLES ---
    private var hasUserStartedTalking = false
    private var lastSpeechTimestamp = 0L

    // CONFIGURATION
    private val THRESHOLD = 1500     // Volume level to consider "Talking"
    private val INITIAL_PATIENCE = 5000L // Wait 5s if user hasn't said anything yet
    private val SILENCE_TIMEOUT = 2000L  // Wait only 2s once user stops talking

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isProcessing) return

            val amplitude = recorder.getAmplitude()
            val currentTime = System.currentTimeMillis()

            // 1. Detect Speech
            if (amplitude > THRESHOLD) {
                if (!hasUserStartedTalking) {
                    Log.d("Qareeb", "User STARTED talking.")
                    hasUserStartedTalking = true
                }
                // Reset the "Last time heard speech" clock
                lastSpeechTimestamp = currentTime
            }

            // 2. Decide if we should close
            val timeSinceLastSpeech = currentTime - lastSpeechTimestamp

            if (hasUserStartedTalking) {
                // CASE A: User was talking, now is silent. Wait for short timeout (2s)
                if (timeSinceLastSpeech > SILENCE_TIMEOUT) {
                    Log.d("Qareeb", "Silence detected ($SILENCE_TIMEOUT ms). Finishing.")
                    dismissAndUpload()
                    return // Stop the loop
                }
            } else {
                // CASE B: User hasn't started yet. Wait for long timeout (5s)
                if (timeSinceLastSpeech > INITIAL_PATIENCE) {
                    Log.d("Qareeb", "User never spoke ($INITIAL_PATIENCE ms). Closing.")
                    closeOverlay()
                    return // Stop the loop
                }
            }

            // Check again in 100ms
            mainHandler.postDelayed(this, 100)
        }
    }

    fun show() {
        mainHandler.post {
            if (overlayView != null) return@post

            overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_qareeb, null)
            overlayContainer = overlayView?.findViewById(R.id.overlay_container)
            val params = createLayoutParams()

            try {
                windowManager.addView(overlayView, params)

                // Set initial state to listening
                setOverlayState(OverlayState.LISTENING)

                // --- THE FIX IS HERE ---
                // Wait 500 before grabbing the mic.
                // This prevents the "start failed: -38" error.
                mainHandler.postDelayed({
                    startRecordingAndListening()
                }, 500)

            } catch (e: Exception) {
                e.printStackTrace()
                onDismiss()
            }
        }
    }

    private fun startRecordingAndListening() {
        try {
            // 1. Start Recording
            audioFile = recorder.startRecording()

            // 2. Start Animation
           // val micIcon = overlayView?.findViewById<ImageView>(R.id.iv_mic_icon)
           // if (micIcon != null) startBreathingAnimation(micIcon)

            // 3. Start Smart Logic
            // Initialize Timing Logic
            hasUserStartedTalking = false
            lastSpeechTimestamp = System.currentTimeMillis()

            // Start the monitoring loop
            mainHandler.post(monitorRunnable)

        } catch (e: Exception) {
            Log.e("Qareeb", "Failed to start recording: ${e.message}")
            // If mic fails, close immediately so we don't hang
            onDismiss()
        }
    }

    private fun dismissAndUpload() {
        mainHandler.post {
            isProcessing = true
            mainHandler.removeCallbacks(monitorRunnable)

            recorder.stopRecording()

            // Change to thinking state
            setOverlayState(OverlayState.THINKING)

            if (audioFile != null && audioFile!!.exists()) {
                uploadAudioToServer(audioFile!!)
            } else {
                setOverlayState(OverlayState.FAILED)
                mainHandler.postDelayed({ closeOverlay() }, 2000)
            }
        }
    }


    // ... (Keep uploadAudioToServer, closeOverlay, startBreathingAnimation, createLayoutParams exactly as before) ...

    private fun uploadAudioToServer(file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get userID from session
                val userId = sessionManager.getUserId()
                if (userId.isNullOrEmpty()) {
                    Log.e("Qareeb", "Upload failed: userID is null or empty")
                    withContext(Dispatchers.Main) {
                        setOverlayState(OverlayState.FAILED)
                        mainHandler.postDelayed({ closeOverlay() }, 2000)
                    }
                    return@launch
                }

                val requestFile = file.asRequestBody("audio/mp3".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                
                // Create userID as multipart form field (null filename for non-file parts)
                val userIdRequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
                val userIdPart = MultipartBody.Part.createFormData("userID", null, userIdRequestBody)

                val response = NetworkModule.api.uploadAudio(filePart, userIdPart)

                // Direct insertion: Parse and insert transaction/task directly from API response
                if (response.status == "success" && response.result?.success == true) {
                    try {
                        withContext(Dispatchers.IO) {
                            when (response.intent) {
                                "FINANCE" -> {
                                    val transactionData = response.result.data?.transaction
                                    if (transactionData != null) {
                                        val transaction = parseTransactionFromResponse(transactionData, userId)
                                        transactionDao.upsertTransaction(transaction)
                                        Log.d("Qareeb", "Transaction inserted directly: ${transaction.transactionId}")
                                    } else {
                                        Log.w("Qareeb", "Transaction data missing in response, falling back to sync")
                                        syncRepository.sync(userId)
                                    }
                                }
                                "TASK_TRACKER" -> {
                                    val taskData = response.result.data?.task
                                    if (taskData != null) {
                                        val task = parseTaskFromResponse(taskData, userId)
                                        taskDao.upsertTask(task)
                                        Log.d("Qareeb", "Task inserted directly: ${task.taskId}")
                                    } else {
                                        Log.w("Qareeb", "Task data missing in response, falling back to sync")
                                        syncRepository.sync(userId)
                                    }
                                }
                                else -> {
                                    Log.d("Qareeb", "No transaction/task created (intent: ${response.intent})")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Qareeb", "Failed to insert transaction/task: ${e.message}", e)
                        // Fallback to sync if direct insertion fails
                        try {
                            withContext(Dispatchers.IO) {
                                syncRepository.sync(userId)
                            }
                        } catch (syncError: Exception) {
                            Log.e("Qareeb", "Sync fallback also failed: ${syncError.message}", syncError)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    // Success state
                    setOverlayState(OverlayState.SUCCESS)
                    mainHandler.postDelayed({ closeOverlay() }, 2000)
                }
            } catch (e: Exception) {
                Log.e("Qareeb", "Upload failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // Failed state
                    setOverlayState(OverlayState.FAILED)
                    mainHandler.postDelayed({ closeOverlay() }, 2000)
                }
            }
        }
    }


    private fun closeOverlay() {
        // Stop any running animations
        thinkingAnimator?.cancel()
        thinkingAnimator = null

        // 1. Remove the View immediately (Visual feedback)
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) { e.printStackTrace() }
            overlayView = null
            overlayContainer = null
        }
        isProcessing = false
        currentState = OverlayState.LISTENING

        // 2. RELEASE THE MIC
        recorder.stopRecording()

        // 3. WAIT before restarting Porcupine (The Fix)
        // This gives Android time to fully release the hardware resource
        // preventing the "VoiceProcessorReadException" loop.
        mainHandler.postDelayed({
            onDismiss()
        }, 500)
    }

    private fun setOverlayState(state: OverlayState) {
        if (currentState == state) return
        currentState = state

        mainHandler.post {
            val container = overlayContainer ?: return@post
            val drawableRes = when (state) {
                OverlayState.LISTENING -> R.drawable.edge_glow_listening
                OverlayState.THINKING -> R.drawable.edge_glow_thinking
                OverlayState.SUCCESS -> R.drawable.edge_glow_success
                OverlayState.FAILED -> R.drawable.edge_glow_failed
            }

            // Smooth transition animation
            val fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0.3f)
            fadeOut.duration = 150
            fadeOut.interpolator = AccelerateDecelerateInterpolator()

            val fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0.3f, 1f)
            fadeIn.duration = 150
            fadeIn.interpolator = AccelerateDecelerateInterpolator()

            fadeOut.start()
            fadeOut.addUpdateListener {
                if (it.animatedFraction >= 0.5f && container.background == null) {
                    container.setBackgroundResource(drawableRes)
                }
            }

            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    container.setBackgroundResource(drawableRes)
                    fadeIn.start()
                }
            })

            // Start pulsing animation for thinking state
            if (state == OverlayState.THINKING) {
                startThinkingAnimation(container)
            } else {
                thinkingAnimator?.cancel()
                thinkingAnimator = null
            }
        }
    }

    private fun startThinkingAnimation(view: View) {
        thinkingAnimator?.cancel()

        // Create pulsing animation for thinking state
        thinkingAnimator = ObjectAnimator.ofFloat(view, "alpha", 0.7f, 1f, 0.7f)
        thinkingAnimator?.duration = 1500
        thinkingAnimator?.repeatCount = ObjectAnimator.INFINITE
        thinkingAnimator?.repeatMode = ObjectAnimator.RESTART
        thinkingAnimator?.interpolator = LinearInterpolator()
        thinkingAnimator?.start()
    }

    private fun startBreathingAnimation(view: View) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view, PropertyValuesHolder.ofFloat("scaleX", 1.2f), PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        )
        scaleDown.duration = 1000
        scaleDown.repeatCount = ObjectAnimator.INFINITE
        scaleDown.repeatMode = ObjectAnimator.REVERSE
        scaleDown.interpolator = AccelerateDecelerateInterpolator()
        scaleDown.start()
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        params.y = 100
        return params
    }
    
    /**
     * Parse transaction from API response and convert to Room entity
     * Ensures userID is set correctly from the session
     */
    private fun parseTransactionFromResponse(
        transactionData: TransactionResponse,
        userId: String
    ): Transaction {
        // Parse date string to milliseconds
        val dateMillis = try {
            if (transactionData.date != null) {
                try {
                    OffsetDateTime.parse(transactionData.date).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(transactionData.date).toEpochMilli()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        
        // Parse state
        val state = try {
            transactionData.state?.let { TransactionState.valueOf(it.uppercase()) }
                ?: TransactionState.PENDING
        } catch (e: Exception) {
            TransactionState.PENDING
        }
        
        // Ensure userID from session is used (not from API response for security)
        return Transaction(
            transactionId = transactionData.transactionID,
            userId = userId, // Use session userID, not from API
            categoryId = transactionData.categoryID,
            amount = transactionData.amount ?: 0.0,
            date = dateMillis,
            source = transactionData.source,
            description = transactionData.description,
            title = transactionData.title ?: transactionData.description ?: "Transaction",
            income = transactionData.income ?: false,
            state = state,
            isDeleted = transactionData.is_deleted ?: false,
            is_synced = true, // Mark as synced since it came from server
            updatedAt = transactionData.created_at ?: java.time.OffsetDateTime.now().toString()
        )
    }
    
    /**
     * Parse task from API response and convert to Room entity
     * Ensures userID is set correctly from the session
     */
    private fun parseTaskFromResponse(
        taskData: TaskResponse,
        userId: String
    ): Task {
        // Parse dueDate string to milliseconds
        // Handles multiple formats: YYYY-MM-DD, ISO datetime, etc.
        val dueDateMillis = taskData.dueDate?.let { dueDateStr ->
            try {
                // Try parsing as full datetime first
                try {
                    OffsetDateTime.parse(dueDateStr).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(dueDateStr).toEpochMilli()
                    } catch (e2: Exception) {
                        // Try parsing as date only (YYYY-MM-DD)
                        try {
                            java.time.LocalDate.parse(dueDateStr)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        } catch (e3: Exception) {
                            Log.w("Qareeb", "Could not parse dueDate: $dueDateStr, using today")
                            // If parsing fails, use today's date at start of day
                            java.time.LocalDate.now()
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("Qareeb", "Error parsing dueDate: ${e.message}")
                // Default to today at start of day
                java.time.LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
        } ?: run {
            // If no dueDate provided, use today at start of day
            java.time.LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        
        // Parse created_at to milliseconds
        val createdAtMillis = taskData.created_at?.let { createdAtStr ->
            try {
                try {
                    OffsetDateTime.parse(createdAtStr).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(createdAtStr).toEpochMilli()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()
        
        // Parse status - handle lowercase status strings
        val status = try {
            taskData.status?.let { statusStr ->
                // Handle both "pending" and "PENDING" formats
                val normalizedStatus = statusStr.uppercase()
                TaskStatus.valueOf(normalizedStatus)
            } ?: TaskStatus.PENDING
        } catch (e: Exception) {
            Log.w("Qareeb", "Unknown task status: ${taskData.status}, defaulting to PENDING")
            TaskStatus.PENDING
        }
        
        // Ensure userID from session is used (not from API response for security)
        val task = Task(
            taskId = taskData.taskID,
            userId = userId, // Use session userID, not from API
            title = taskData.title ?: "Task",
            description = taskData.description,
            status = status,
            progressPercentage = taskData.progressPercentage ?: 0,
            priority = taskData.priority,
            dueDate = dueDateMillis,
            createdAt = createdAtMillis,
            updatedAt = taskData.updated_at ?: System.currentTimeMillis().toString(),
            isDeleted = taskData.is_deleted ?: false,
            is_synced = true // Mark as synced since it came from server
        )
        
        Log.d("Qareeb", "Parsed task: id=${task.taskId}, title=${task.title}, dueDate=${task.dueDate}, status=${task.status}")
        return task
    }
}