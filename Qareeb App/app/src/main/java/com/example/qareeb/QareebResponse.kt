package com.example.qareeb

import com.google.gson.annotations.SerializedName

data class QareebResponse(
    // This tells Gson: "Look for 'transcription' in JSON, but call it 'command' in Kotlin"
    @SerializedName("transcription")
    val command: String
)