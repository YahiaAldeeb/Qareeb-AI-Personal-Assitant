package com.example.qareeb.data.mapper

import com.example.qareeb.data.entity.Task
import com.example.qareeb.domain.model.TaskDomain


fun Task.toDomain(): TaskDomain {
    return TaskDomain(
        taskId = taskId,
        userId = userId,
        title = title,
        description = description,
        status = status,
        progressPercentage = progressPercentage,
        createdAt = createdAt,
        priority = priority,
        dueDate = dueDate
    )
}

fun TaskDomain.toEntity(): Task{
    return Task(
        taskId = taskId,
        userId = userId,
        title = title,
        description = description,
        status = status,
        progressPercentage = progressPercentage,
        createdAt = createdAt,
        priority = priority,
        dueDate = dueDate
    )
}