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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

enum class OverlayState {
    LISTENING,  // Waiting for user input
    THINKING,   // Processing/uploading
    SUCCESS,    // Task completed successfully
    FAILED      // Task failed
}

class QareebOverlay(
    private val context: Context,
    private val onDismiss: () -> Unit
) {

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
                val requestFile = file.asRequestBody("audio/mp3".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = NetworkModule.api.uploadAudio(body)

                withContext(Dispatchers.Main) {
                    // Success state
                    setOverlayState(OverlayState.SUCCESS)
                    mainHandler.postDelayed({ closeOverlay() }, 2000)
                }
            } catch (e: Exception) {
                Log.e("Qareeb", "Upload failed: ${e.message}")
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
}