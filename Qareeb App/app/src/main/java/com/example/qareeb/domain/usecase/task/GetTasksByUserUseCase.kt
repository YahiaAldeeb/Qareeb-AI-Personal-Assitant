package com.example.qareeb.domain.usecase.task
import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksByUserUseCase(private val taskRepo: TaskRepository) {
    operator fun invoke(userId: String): Flow<List<TaskDomain>> = taskRepo.getTasksByUser(userId)
}
