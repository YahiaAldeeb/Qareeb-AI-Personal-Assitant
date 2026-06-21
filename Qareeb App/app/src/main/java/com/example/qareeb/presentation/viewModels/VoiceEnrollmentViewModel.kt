package com.example.qareeb.presentation.viewModels

import android.content.Context
import android.media.MediaRecorder
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class VoiceEnrollmentViewModel : ViewModel() {

    private var mediaRecorder: MediaRecorder? = null
    
    // Track file paths for the 3 samples
    private val filePaths = Array(3) { "" }

    var isRecording = mutableStateOf(false)
    var isLoading = mutableStateOf(false)
    var isEnrollmentFinished = mutableStateOf(false) // True after successful upload
    
    var currentStep = mutableStateOf(0) // 0, 1, 2
    var samplesRecorded = mutableStateOf(listOf(false, false, false))
    
    var statusMessage = mutableStateOf("Press the mic to record Sentence 1")

    val phrases = listOf(
        "\"Every voice tells a story, and Qareeb knows mine\"",
        "\"Secure access is my voice signature\"",
        "\"Activate my personal assistant now\""
    )

    fun setStatus(msg: String) { statusMessage.value = msg }

    fun startRecording(context: Context) {
        val step = currentStep.value
        filePaths[step] = "${context.cacheDir.absolutePath}/voice_enrollment_sample_${step + 1}.wav"

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(filePaths[step])
                prepare()
                start()
            }
            isRecording.value = true
            statusMessage.value = "Recording Sentence ${step + 1}... tap to stop"
        } catch (e: Exception) {
            statusMessage.value = "Error starting recorder: ${e.message}"
            isRecording.value = false
        }
    }

    fun stopRecording(context: Context) {
        try {
            mediaRecorder?.apply { stop(); release() }
        } catch (e: Exception) {
            statusMessage.value = "Error stopping recorder: ${e.message}"
        } finally {
            mediaRecorder = null
            isRecording.value = false
        }

        val step = currentStep.value
        // Mark current sample as recorded
        samplesRecorded.value = samplesRecorded.value.toMutableList().apply {
            set(step, true)
        }

        if (step < 2) {
            currentStep.value = step + 1
            statusMessage.value = "Sentence ${step + 1} recorded! Speak Sentence ${step + 2}."
        } else {
            statusMessage.value = "All sentences recorded! Tap Confirm to finalize."
        }
    }

    fun resetRecordings() {
        currentStep.value = 0
        samplesRecorded.value = listOf(false, false, false)
        statusMessage.value = "Press the mic to record Sentence 1"
    }

    fun uploadVoice(context: Context, user: UserDomain, onSuccess: () -> Unit) {
        isLoading.value = true
        statusMessage.value = "Uploading voice enrollment samples..."
        
        viewModelScope.launch {
            try {
                // Prepare multi-part file list
                val parts = mutableListOf<MultipartBody.Part>()
                for (i in 0..2) {
                    val file = File(filePaths[i])
                    if (!file.exists()) {
                        throw IllegalStateException("Missing recorded sample ${i + 1}")
                    }
                    val part = MultipartBody.Part.createFormData(
                        "wav_files", // Key name matching FastAPI List[UploadFile]
                        file.name,
                        file.asRequestBody("audio/wav".toMediaTypeOrNull())
                    )
                    parts.add(part)
                }

                val response = RetrofitInstance.api.registerVoice(user.userId, parts)

                if (response.voiceEmbedding.isNotEmpty()) {
                    SessionManager.getInstance(context).setVoiceEnrolled(true)
                    SessionManager.getInstance(context).setSkippedVoiceEnrollment(false)
                    statusMessage.value = "Voice enrolled successfully!"
                    isEnrollmentFinished.value = true
                    
                    // Clean up files
                    for (i in 0..2) {
                        try {
                            File(filePaths[i]).delete()
                        } catch (e: Exception) {}
                    }
                    
                    onSuccess()
                } else {
                    statusMessage.value = "Enrollment failed, please try again"
                }

            } catch (e: Exception) {
                statusMessage.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}