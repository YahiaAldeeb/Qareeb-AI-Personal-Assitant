package com.example.qareeb

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File? {
        outputFile = File(context.cacheDir, "command_audio.m4a")

        // Attempt 1: Try High Quality (Optimized for Whisper)
        if (initRecorder(highQuality = true)) {
            return outputFile
        }

        // Attempt 2: If that failed, try Standard Quality (Safe Mode)
        Log.e("AudioRecorder", "High Quality failed. Retrying with defaults...")
        if (initRecorder(highQuality = false)) {
            return outputFile
        }

        return null // Both failed
    }

    private fun initRecorder(highQuality: Boolean): Boolean {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                if (highQuality) {
                    // Whisper Preferred Settings
                    setAudioEncodingBitRate(64000)
                    setAudioSamplingRate(16000)
                } else {
                    // Android Defaults (Safe for all devices)
                    setAudioEncodingBitRate(96000)
                    setAudioSamplingRate(44100)
                }

                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            Log.d("AudioRecorder", "Recording started (HighQuality: $highQuality)")
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to init: ${e.message}")
            stopRecording() // Clean up
            return false
        }
    }

    fun getAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore stop errors
        }
        mediaRecorder = null
    }
}