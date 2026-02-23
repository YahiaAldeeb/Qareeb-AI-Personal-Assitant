package com.example.qareeb.data.entity

import androidx.room.*

@Entity(tableName = "memory")
data class Memory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "memory_id")
    val memoryId: Long = 0,

    @ColumnInfo(name = "userID")
    val userId: Long,

    val fact: String,
    val embedding: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
