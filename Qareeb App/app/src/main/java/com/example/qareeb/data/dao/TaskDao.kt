package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Task

import com.example.qareeb.domain.model.enums.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task WHERE userID = :userId ORDER BY created_at DESC")
    fun getTasksByUser(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE userID = :userId AND status = :status")
    fun getTasksByStatus(userId: String, status: TaskStatus): Flow<List<Task>>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM task WHERE userID = :userId AND is_deleted = 0")
    suspend fun getTasksByUserOneShot(userId: String): List<Task>

    // upsert — insert or replace if exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: Task)

    // delete by id for soft deletes
    @Query("DELETE FROM task WHERE task_id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM task WHERE is_synced = 0")
    suspend fun getUnsyncedTasks(): List<Task>

    @Query("UPDATE task SET is_synced = 1 WHERE task_id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}