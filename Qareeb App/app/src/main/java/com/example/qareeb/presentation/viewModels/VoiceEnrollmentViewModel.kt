package com.example.qareeb.presentation.viewModels

import android.content.Context
import android.media.MediaRecorder
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.usecase.UpdateVoiceEmbeddingUseCase
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class VoiceEnrollmentViewModel(
    private val updateVoiceEmbeddingUseCase: UpdateVoiceEmbeddingUseCase
) : ViewModel() {

    private var mediaRecorder: MediaRecorder? = null
    private var wavFilePath: String = ""

    var isRecording = mutableStateOf(false)
    var isLoading = mutableStateOf(false)
    var isSuccess = mutableStateOf(false)
    var statusMessage = mutableStateOf("Press the mic to start recording")

    fun setStatus(msg: String) { statusMessage.value = msg }

    fun startRecording(context: Context) {
        wavFilePath = "${context.cacheDir.absolutePath}/voice_enrollment.wav"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(wavFilePath)
            prepare()
            start()
        }

        isRecording.value = true
        statusMessage.value = "Recording... tap to stop"
    }

    fun stopRecording(context: Context, user: UserDomain) {
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        isRecording.value = false
        statusMessage.value = "Processing..."
        uploadVoice(wavFilePath, user)
    }

    private fun uploadVoice(filePath: String, user: UserDomain) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val file = File(filePath)
                val filePart = MultipartBody.Part.createFormData(
                    "wav_file",
                    file.name,
                    file.asRequestBody("audio/wav".toMediaTypeOrNull())
                )

                val response = RetrofitInstance.api.registerVoice(user.userId, filePart)

                if (response.voice_embedding.isNotEmpty()) {

                    // ✅ Decode Base64 embedding from Python → ByteArray
                    val embeddingBytes = android.util.Base64.decode(
                        response.voice_embedding,
                        android.util.Base64.DEFAULT
                    )

                    // ✅ Call use case → saves embedding to RoomDB
                    updateVoiceEmbeddingUseCase(user, embeddingBytes)

                    statusMessage.value = "Voice enrolled successfully!"
                    isSuccess.value = true
                    file.delete()

                } else {
                    statusMessage.value = "Enrollment failed, try again"
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