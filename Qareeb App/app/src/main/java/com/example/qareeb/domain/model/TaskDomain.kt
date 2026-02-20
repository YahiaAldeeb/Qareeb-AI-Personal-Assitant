package com.example.qareeb.domain.model
import com.example.qareeb.domain.model.enums.TaskStatus

data class TaskDomain(
    val taskId: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val progressPercentage: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: String? = null,
    val dueDate: Long? = null
)