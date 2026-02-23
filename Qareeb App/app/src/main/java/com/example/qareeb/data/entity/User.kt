package com.example.qareeb.data.entity

import androidx.room.*
import java.util.UUID


@Entity(tableName = "user")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "userID")
    val userId: String = UUID.randomUUID().toString(),

    val name: String,
    val email: String,
    val password: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_login")
    val lastLogin: Long? = null,

    // Voice biometric authentication
    //Ma3moul Nullable 3shan momken yet7at fy JSON aw base64 encoded data
    @ColumnInfo(name = "voice_embedding")
    val voiceEmbedding: String? = null
)

