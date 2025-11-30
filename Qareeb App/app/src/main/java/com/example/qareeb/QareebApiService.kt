package com.example.qareeb

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface QareebApiService {
    @Multipart
    @POST("/transcribe") // Matches your Python route
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part // Matches 'file: UploadFile' in Python
    ): QareebResponse
}