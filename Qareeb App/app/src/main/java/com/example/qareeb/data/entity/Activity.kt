package com.example.qareeb.data.entity

import androidx.room.*

@Entity(
    tableName = "activity",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userID"],
            childColumns = ["userID"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["task_id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userID"), Index("task_id")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "activity_id")
    val activityId: Long = 0,

    @ColumnInfo(name = "userID")
    val userId: Long,

    @ColumnInfo(name = "task_id")
    val taskId: Long? = null,

    @ColumnInfo(name = "action_type")
    val actionType: String,

    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
