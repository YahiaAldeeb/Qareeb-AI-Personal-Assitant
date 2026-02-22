package com.example.qareeb.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

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
    val email: String
)

interface AuthApi {
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}