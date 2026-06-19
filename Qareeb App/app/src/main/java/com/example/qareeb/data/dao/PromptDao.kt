package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Prompt
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {

    // Insert a new conversation turn
    @Insert
    suspend fun insertPrompt(prompt: Prompt): Long

    // Get last N messages for short-term memory (ordered oldest → newest)
    @Query("""
        SELECT * FROM prompts 
        WHERE userID = :userId 
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentPrompts(userId: String, limit: Int = 20): List<Prompt>

    // Get last N messages from a specific module
    @Query("""
        SELECT * FROM prompts 
        WHERE userID = :userId AND module = :module
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentPromptsByModule(
        userId: String,
        module: String,
        limit: Int = 10
    ): List<Prompt>

    // Clear old messages (keep only last 50 to save space)
    @Query("""
        DELETE FROM prompts 
        WHERE userID = :userId 
        AND prompt_id NOT IN (
            SELECT prompt_id FROM prompts 
            WHERE userID = :userId 
            ORDER BY created_at DESC 
            LIMIT 50
        )
    """)
    suspend fun pruneOldPrompts(userId: String)
}