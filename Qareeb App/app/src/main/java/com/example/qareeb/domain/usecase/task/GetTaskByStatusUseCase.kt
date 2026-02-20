package com.example.qareeb.domain.usecase.task
import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTaskByStatusUseCase(private val taskRepo: TaskRepository) {
     operator fun invoke(userId: String, status: TaskStatus): Flow<List<TaskDomain>> = taskRepo.getTasksByStatus(userId,status)
}