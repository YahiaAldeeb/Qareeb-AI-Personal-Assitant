package com.example.qareeb.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val userID: String,
    val name: String,
    val email: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phoneNumber: String
)

data class RegisterResponse(
    val userID: String,
    val name: String,
    val email: String,

    )
data class VoiceEmbeddingResponse(
    val voice_embedding: String  // Base64 string from Python
)

interface AuthApi {
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    // ✅ second API call — send .wav file after registration
    @Multipart
    @POST("users/voice/{userID}")
    suspend fun registerVoice(
        @Path("userID") userID: String,
        @Part wavFile: MultipartBody.Part
    ): VoiceEmbeddingResponse
}