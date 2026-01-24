package com.example.qareeb.presentation.mapper

import com.example.qareeb.data.entity.TaskStatus
import com.example.qareeb.presentation.models.TaskStateUI

fun TaskStatus.toUI(): TaskStateUI {
    return when (this) {
        TaskStatus.COMPLETED -> TaskStateUI.Completed
        TaskStatus.POSTPONED -> TaskStateUI.Postponed
        TaskStatus.PENDING -> TaskStateUI.Pending
        TaskStatus.IN_PROGRESS -> TaskStateUI.InProgress
    }
}