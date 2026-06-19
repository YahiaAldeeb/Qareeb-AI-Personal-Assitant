package com.example.qareeb.data.entity

import androidx.room.*
import java.util.UUID

@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey
    @ColumnInfo(name = "prompt_id")
    val promptId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "userID")
    val userId: String,

    @ColumnInfo(name = "user_message")
    val userMessage: String,

    @ColumnInfo(name = "qareeb_response")
    val qareebResponse: String,

    @ColumnInfo(name = "prompt_type")
    val promptType: String,

    @ColumnInfo(name = "module")
    val module: String = "CHATBOT", // "CHATBOT","FINANCE","TASK","UI_AUTOMATION"

    @ColumnInfo(name = "intent_detected")
    val intentDetected: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    val metadata: String? = null
)
