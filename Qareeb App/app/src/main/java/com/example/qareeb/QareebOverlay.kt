package com.example.qareeb

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
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
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class QareebOverlay(
    private val context: Context,
    private val onDismiss: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    private val recorder = AudioRecorderHelper(context)
    private var audioFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isProcessing = false

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
            val params = createLayoutParams()

            try {
                windowManager.addView(overlayView, params)

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
            val micIcon = overlayView?.findViewById<ImageView>(R.id.iv_mic_icon)
            if (micIcon != null) startBreathingAnimation(micIcon)

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
            mainHandler.removeCallbacks(monitorRunnable) // Stop monitoring

            recorder.stopRecording()

            val statusText = overlayView?.findViewById<TextView>(R.id.tv_overlay_status)
            statusText?.text = "Thinking..."

            if (audioFile != null && audioFile!!.exists()) {
                uploadAudioToServer(audioFile!!)
            } else {
                closeOverlay()
            }
        }
    }

    // ... (Keep uploadAudioToServer, closeOverlay, startBreathingAnimation, createLayoutParams exactly as before) ...

    private fun uploadAudioToServer(file: File) {
        val statusText = overlayView?.findViewById<TextView>(R.id.tv_overlay_status)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestFile = file.asRequestBody("audio/mp3".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = NetworkModule.api.uploadAudio(body)
                withContext(Dispatchers.Main) {
                    statusText?.text = "Command: ${response.command}"
                    mainHandler.postDelayed({ closeOverlay() }, 3000)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText?.text = "Error"
                    mainHandler.postDelayed({ closeOverlay() }, 3000)
                }
            }
        }
    }

    private fun closeOverlay() {
        // 1. Remove the View immediately (Visual feedback)
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) { e.printStackTrace() }
            overlayView = null
        }
        isProcessing = false

        // 2. RELEASE THE MIC
        recorder.stopRecording()

        // 3. WAIT before restarting Porcupine (The Fix)
        // This gives Android time to fully release the hardware resource
        // preventing the "VoiceProcessorReadException" loop.
        mainHandler.postDelayed({
            onDismiss()
        }, 500)
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
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        params.y = 100
        return params
    }
}