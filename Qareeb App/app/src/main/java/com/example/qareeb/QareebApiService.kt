package com.example.qareeb

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Body
import retrofit2.http.Headers

interface QareebApiService {
    @Multipart
    @POST("/api/ai/transcribe")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part userID: MultipartBody.Part
    ): QareebResponse

    @Headers("Content-Type: application/json")
    @POST("/api/ai/text")
    suspend fun sendTextMessage(
        @Body request: TextMessageRequest
    ): QareebResponse
}

data class TextMessageRequest(
    val text: String,
    val userID: String
)