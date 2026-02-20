package com.example.qareeb.domain.usecase.task

import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class DeleteTaskUseCase(private val taskRepo: TaskRepository) {
    suspend operator fun invoke(task:TaskDomain) = taskRepo.deleteTask(task)
}
