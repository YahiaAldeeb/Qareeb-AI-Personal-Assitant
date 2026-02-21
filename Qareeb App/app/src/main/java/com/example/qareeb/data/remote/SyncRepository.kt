package com.example.qareeb.data.remote

import android.content.SharedPreferences
import android.util.Log
import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.data.dao.UserDao
import com.example.qareeb.data.entity.Task
import com.example.qareeb.data.entity.User

class SyncRepository(
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val api: SyncApi,
    private val prefs: SharedPreferences
) {
    companion object {
        private const val LAST_SYNC_KEY = "last_sync_time"
    }

    suspend fun sync(userId: String) {
        try {
            Log.d("SYNC", "Starting sync for user: $userId")
            push(userId)
            pull(userId)
            Log.d("SYNC", "Sync completed successfully!")
        } catch (e: Exception) {
            Log.e("SYNC", "Sync failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun push(userId: String) {
        val unsynced = taskDao.getUnsyncedTasks()
        if (unsynced.isEmpty()) {
            Log.d("SYNC", "Nothing to push")
            return
        }

        Log.d("SYNC", "Pushing ${unsynced.size} records to server")

        val payload = PushPayload(
            records = unsynced.map { task ->
                TaskSync(
                    taskID = task.taskId,
                    userID = task.userId,
                    title = task.title,
                    description = task.description,
                    updated_at = task.updatedAt,
                    is_deleted = task.isDeleted
                )
            }
        )
        api.push(payload)
        taskDao.markSynced(unsynced.map { it.taskId })
        Log.d("SYNC", "Push completed!")
    }

    private suspend fun pull(userId: String) {
        val lastSync = prefs.getString(LAST_SYNC_KEY, "2000-01-01T00:00:00") ?: "2000-01-01T00:00:00"
        Log.d("SYNC", "Pulling changes since: $lastSync")

        val response = api.pull(userId, lastSync)
        Log.d("SYNC", "Pulled ${response.records.size} records from server")

        // Ensure user exists in local Room before inserting tasks
        userDao.insertUser(
            User(
                userId = userId,
                name = "User",
                email = "",
                password = ""
            )
        )

        response.records.forEach { remote ->
            Log.d("SYNC_DATE", "Processing: ${remote.title}, dueDate: ${remote.dueDate}")

            if (remote.is_deleted) {
                taskDao.deleteTaskById(remote.taskID)
            } else {
                val dueDateMillis = remote.dueDate?.let {
                    try {
                        java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        try {
                            java.time.LocalDateTime.parse(it)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        } catch (e2: Exception) {
                            Log.e("SYNC_DATE", "Failed to parse dueDate: $it")
                            null
                        }
                    }
                }

                Log.d("SYNC_DATE", "Task: ${remote.title}, converts to: ${
                    dueDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                }, today is: ${java.time.LocalDate.now()}")

                try {
                    taskDao.upsertTask(
                        Task(
                            taskId = remote.taskID,
                            userId = remote.userID,
                            title = remote.title,
                            description = remote.description,
                            updatedAt = remote.updated_at,
                            dueDate = dueDateMillis
                        )
                    )
                    Log.d("SYNC_DATE", "Upserted successfully: ${remote.title}")
                } catch (e: Exception) {
                    Log.e("SYNC_DATE", "Failed to upsert: ${remote.title}, error: ${e.message}")
                }
            }
        }

        prefs.edit().putString(LAST_SYNC_KEY, response.server_time).apply()
    }
}