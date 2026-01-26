package com.example.qareeb.data.entity

import androidx.room.*

@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "prompt_id")
    val promptId: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "user_message")
    val userMessage: String,

    @ColumnInfo(name = "qareeb_response")
    val qareebResponse: String,

    @ColumnInfo(name = "prompt_type")
    val promptType: String,

    @ColumnInfo(name = "intent_detected")
    val intentDetected: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    val metadata: String? = null
)
