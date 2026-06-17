package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Memory

@Dao
interface MemoryDao {

    @Insert
    suspend fun insertMemory(memory: Memory)

    // Replace old fact with same key to avoid duplicates
    @Query("DELETE FROM memory WHERE userId = :userId AND fact LIKE :keyPattern")
    suspend fun deleteByKeyPattern(userId: String, keyPattern: String)

    @Query("SELECT * FROM memory WHERE userId = :userId ORDER BY created_at DESC")
    suspend fun getAllMemories(userId: String): List<Memory>

    @Query("DELETE FROM memory WHERE memory_id = :id")
    suspend fun deleteMemory(id: String)
}