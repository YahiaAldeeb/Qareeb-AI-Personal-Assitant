package com.example.qareeb

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView

class QareebOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    // Timeout Logic
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { dismiss() }

    fun show() {
        // Run on UI Thread
        Handler(Looper.getMainLooper()).post {
            if (overlayView != null) return@post

            overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_qareeb, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            // Position at bottom with some margin
            params.gravity = Gravity.BOTTOM
            params.y = 100

            try {
                windowManager.addView(overlayView, params)

                // 1. Start the Breathing Animation
                val micIcon = overlayView?.findViewById<ImageView>(R.id.iv_mic_icon)
                if (micIcon != null) {
                    startBreathingAnimation(micIcon)
                }

                // 2. Start the 5-second timer
                resetTimeout()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startBreathingAnimation(view: View) {
        // Create a Pulse effect (Scale X and Y)
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1.2f),
            PropertyValuesHolder.ofFloat("scaleY", 1.2f)
        )
        scaleDown.duration = 1000 // 1 second beat
        scaleDown.repeatCount = ObjectAnimator.INFINITE
        scaleDown.repeatMode = ObjectAnimator.REVERSE
        scaleDown.interpolator = AccelerateDecelerateInterpolator()
        scaleDown.start()
    }

    fun resetTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 5000) // 5 Seconds
    }

    fun dismiss() {
        Handler(Looper.getMainLooper()).post {
            timeoutHandler.removeCallbacks(timeoutRunnable)
            if (overlayView != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }
        }
    }
}