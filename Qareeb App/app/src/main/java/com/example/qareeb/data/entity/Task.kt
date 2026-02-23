package com.example.qareeb.data.entity

import androidx.room.*
import com.example.qareeb.domain.model.enums.TaskStatus
import java.util.UUID

@Entity(
    tableName = "task",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userID"],
            childColumns = ["userID"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userID")]
)
data class Task(
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "userID")
    val userId: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,

    @ColumnInfo(name = "progress_percentage")
    val progressPercentage: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    val priority: String? = null,

    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString(),
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "is_synced")
    val is_synced: Boolean = false  // local only, never sent to server
)