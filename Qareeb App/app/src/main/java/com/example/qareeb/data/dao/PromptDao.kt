package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Prompt
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts WHERE user_id = :userId ORDER BY created_at DESC")
    fun getPromptsByUser(userId: String): Flow<List<Prompt>>

    @Insert
    suspend fun insertPrompt(prompt: Prompt): Long

    @Query("DELETE FROM prompts WHERE user_id = :userId")
    suspend fun deletePromptsByUser(userId: String)
}