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
        private const val TAG = "SYNC"
    }

    suspend fun sync(userId: String) {
        try {
            Log.d(TAG, "========== SYNC START ==========")
            Log.d(TAG, "User ID: $userId")
            push(userId)
            pull(userId)
            Log.d(TAG, "========== SYNC COMPLETE ==========")
        } catch (e: Exception) {
            Log.e(TAG, "========== SYNC FAILED ==========")
            Log.e(TAG, "Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun push(userId: String) {
        Log.d(TAG, "--- PUSH START ---")

        val unsynced = taskDao.getUnsyncedTasks()
        if (unsynced.isEmpty()) {
            Log.d(TAG, "Nothing to push")
            Log.d(TAG, "--- PUSH COMPLETE ---")
            return
        }

        Log.d(TAG, "Pushing ${unsynced.size} records to server")

        val payload = PushPayload(
            records = unsynced.map { task ->
                TaskSync(
                    taskID = task.taskId,
                    userID = task.userId,
                    title = task.title,
                    description = task.description,
                    updated_at = task.updatedAt,
                    is_deleted = task.isDeleted,
                    dueDate = task.dueDate?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toOffsetDateTime()
                            .toString()
                    }
                )
            }
        )

        try {
            api.push(payload)
            taskDao.markSynced(unsynced.map { it.taskId })
            Log.d(TAG, "Successfully pushed ${unsynced.size} tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Push failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "--- PUSH COMPLETE ---")
    }

    private suspend fun pull(userId: String) {
        Log.d(TAG, "--- PULL START ---")

        val lastSync = prefs.getString(LAST_SYNC_KEY, "2000-01-01T00:00:00")
            ?: "2000-01-01T00:00:00"
        Log.d(TAG, "Last sync: $lastSync")

        val response = try {
            api.pull(userId, lastSync)
        } catch (e: Exception) {
            Log.e(TAG, "Pull API call failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "Received ${response.records.size} tasks from server")

        // Ensure user exists in local Room before inserting tasks
        try {
            userDao.insertUser(
                User(
                    userId = userId,
                    name = "User",
                    email = "",
                    password = ""
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "User already exists (OK)")
        }

        val tasksBeforeSync = taskDao.getTasksByUserOneShot(userId).size
        Log.d(TAG, "Tasks in DB BEFORE sync: $tasksBeforeSync")

        var deletedCount = 0
        var upsertedCount = 0
        var errorCount = 0

        response.records.forEach { remote ->
            try {
                Log.d(TAG, "Processing: ${remote.title}, is_deleted=${remote.is_deleted}")

                if (remote.is_deleted) {
                    taskDao.deleteTaskById(remote.taskID)
                    deletedCount++
                    Log.d(TAG, "  ✓ DELETED: ${remote.title}")
                } else {
                    val dueDateMillis = remote.dueDate?.let { dueDateStr ->
                        try {
                            java.time.OffsetDateTime.parse(dueDateStr)
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: Exception) {
                            try {
                                java.time.LocalDateTime.parse(dueDateStr)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                            } catch (e2: Exception) {
                                Log.w(TAG, "  Could not parse dueDate: $dueDateStr")
                                null
                            }
                        }
                    }

                    val task = Task(
                        taskId = remote.taskID,
                        userId = userId,          // ← FIX: use local userId, not remote.userID
                        title = remote.title,
                        description = remote.description,
                        updatedAt = remote.updated_at,
                        dueDate = dueDateMillis,
                        isDeleted = false,
                        is_synced = true
                    )

                    Log.d(TAG, "  Upserting: taskId=${task.taskId}, userId=${task.userId}, title=${task.title}")
                    taskDao.upsertTask(task)
                    upsertedCount++
                    Log.d(TAG, "  ✓ UPSERTED: ${remote.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "  ✗ Failed to process: ${remote.title}")
                Log.e(TAG, "    Error: ${e.message}")
                e.printStackTrace()
                errorCount++
            }
        }

        val tasksAfterSync = taskDao.getTasksByUserOneShot(userId).size
        Log.d(TAG, "Tasks in DB AFTER sync: $tasksAfterSync")
        Log.d(TAG, "Results: $upsertedCount upserted, $deletedCount deleted, $errorCount errors")

        prefs.edit().putString(LAST_SYNC_KEY, response.server_time).apply()
        Log.d(TAG, "Updated last_sync to: ${response.server_time}")

        Log.d(TAG, "--- PULL COMPLETE ---")
    }
}