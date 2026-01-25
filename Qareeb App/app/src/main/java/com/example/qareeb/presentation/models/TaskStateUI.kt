package com.example.qareeb.presentation.models

import androidx.compose.ui.graphics.Color

sealed class TaskStateUI(
    val displayName: String,
    val color: Color
) {
    object Completed : TaskStateUI("Completed", Color(0xFF38A169))
    object Postponed : TaskStateUI("Postponed", Color(0xFFE53E3E))
    object Pending : TaskStateUI("Pending", Color(0xFF5E10FA))
    object InProgress : TaskStateUI("In Progress", Color(0xFF6B46C1))
}
