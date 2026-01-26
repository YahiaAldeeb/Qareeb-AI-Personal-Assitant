package com.example.qareeb

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // ⚠️ TODO: REPLACE WITH YOUR PC'S IP ADDRESS (Run 'ipconfig' or 'ifconfig')
    // Example: "http://192.168.1.15:8000/"
    // Do NOT use localhost.
    private const val BASE_URL = "http://192.168.1.10:8000/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)) // Logs API details
        .connectTimeout(60, TimeUnit.SECONDS) // Whisper takes time to process!
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: QareebApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QareebApiService::class.java)
    }
}