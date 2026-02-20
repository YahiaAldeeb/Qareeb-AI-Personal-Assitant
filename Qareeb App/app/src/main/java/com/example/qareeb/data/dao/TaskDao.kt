package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Task
import com.example.qareeb.domain.model.enums.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task WHERE user_id = :userId ORDER BY created_at DESC")
    fun getTasksByUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE user_id = :userId AND status = :status")
    fun getTasksByStatus(userId: String, status: TaskStatus): Flow<List<Task>>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}