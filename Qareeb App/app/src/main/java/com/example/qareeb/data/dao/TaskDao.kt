package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Task
import com.example.qareeb.domain.model.enums.
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

    @Query("SELECT * FROM task WHERE user_id = :userId AND is_deleted = 0")
    suspend fun getTasksByUserOneShot(userId: String): List<Task>

    // upsert — insert or replace if exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: Task)

    // delete by id for soft deletes
    @Query("DELETE FROM task WHERE task_id = :taskId")
    suspend fun deleteTaskById(taskId: String)
}