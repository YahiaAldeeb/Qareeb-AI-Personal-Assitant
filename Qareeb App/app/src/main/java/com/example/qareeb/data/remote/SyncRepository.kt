package com.example.qareeb.data.remote

import android.content.SharedPreferences
import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.data.entity.Task
import com.example.qareeb.data.dao.TaskDao_Impl

class SyncRepository(
    private val taskDao: TaskDao,
    private val api: SyncApi,
    private val prefs: SharedPreferences  // to store last sync time
) {
    companion object {
        private const val LAST_SYNC_KEY = "last_sync_time"
    }

    suspend fun sync(userId: String) {
        try {
            push(userId)   // push local changes first
            pull(userId)   // then pull server changes
        } catch (e: Exception) {
            e.printStackTrace() // handle no internet gracefully
        }
    }

    // ── Push local changes to server ──
    private suspend fun push(userId: String) {
        val localTasks = taskDao.getTasksByUserOneShot(userId)  // one-shot, not Flow
        val payload = PushPayload(
            records = localTasks.map { task ->
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
    }

    // ── Pull server changes into Room ──
    private suspend fun pull(userId: String) {
        val lastSync = prefs.getString(LAST_SYNC_KEY, "2000-01-01T00:00:00") ?: "2000-01-01T00:00:00"
        val response = api.pull(userId, lastSync)

        response.records.forEach { remote ->
            if (remote.is_deleted) {
                taskDao.deleteTaskById(remote.taskID)
            } else {
                taskDao.upsertTask(
                    Task(
                        taskId = remote.taskID,
                        userId = remote.userID,
                        title = remote.title,
                        description = remote.description,
                        updatedAt = remote.updated_at
                    )
                )
            }
        }

        // save last sync time
        prefs.edit().putString(LAST_SYNC_KEY, response.server_time).apply()
    }
}