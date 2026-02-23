package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Memory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory WHERE userID = :userId ORDER BY created_at DESC")
    fun getMemoriesByUser(userId: String): Flow<List<Memory>>

    @Insert
    suspend fun insertMemory(memory: Memory): Long

    @Delete
    suspend fun deleteMemory(memory: Memory)
}