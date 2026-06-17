package com.example.qareeb.data.entity

import androidx.room.*
import java.util.UUID

@Entity(tableName = "memory")
data class Memory(
    @PrimaryKey
    @ColumnInfo(name = "memory_id")
    val memoryId :String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "userId")
    val userId: String,

    val fact: String,
    val embedding: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
