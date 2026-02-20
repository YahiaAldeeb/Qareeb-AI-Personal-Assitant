package com.example.qareeb.data.repositoryImp

import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.data.mapper.toDomain
import com.example.qareeb.data.mapper.toEntity
import com.example.qareeb.domain.repository.TaskRepository
import com.example.qareeb.domain.model.enums.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(private val taskDao: TaskDao) : TaskRepository {

    override fun getTasksByUser(userId: String): Flow<List<TaskDomain>> {
        return taskDao.getTasksByUser(userId).map { list ->
            list.map { it.toDomain() }  // TaskEntity → domain Task
        }
    }

    override suspend fun insertTask(task: TaskDomain): Long {
        return taskDao.insertTask(task.toEntity())  // domain Task → TaskEntity
    }

    override suspend fun updateTask(task: TaskDomain) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: TaskDomain) {
        taskDao.deleteTask(task.toEntity())

    }

    override fun getTasksByStatus(
        userId: String,
        status: TaskStatus
    ): Flow<List<TaskDomain>> {
        return taskDao.getTasksByStatus(userId,status).map { list ->
            list.map { it.toDomain() }
        }
    }
}