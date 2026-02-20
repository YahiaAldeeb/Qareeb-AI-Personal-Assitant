package com.example.qareeb.domain.repository

import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.model.enums.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksByUser(userId: String): Flow<List<TaskDomain>>
    fun getTasksByStatus(userId: String, status: TaskStatus): Flow<List<TaskDomain>>
    suspend fun insertTask(task: TaskDomain): Long
    suspend fun updateTask(task: TaskDomain)
    suspend fun deleteTask(task: TaskDomain)
}