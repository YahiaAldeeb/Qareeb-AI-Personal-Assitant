package com.example.qareeb.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: SyncApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://your-backend-url.com") // ← your backend URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncApi::class.java)
    }
}