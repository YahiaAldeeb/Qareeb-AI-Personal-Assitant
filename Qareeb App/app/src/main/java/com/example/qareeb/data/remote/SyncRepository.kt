package com.example.qareeb.data.remote

import android.content.SharedPreferences
import android.util.Log
import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.data.dao.TransactionDao
import com.example.qareeb.data.dao.UserDao
import com.example.qareeb.data.entity.Task
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.data.entity.User
import com.example.qareeb.domain.model.enums.TransactionState

class SyncRepository(
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val api: SyncApi,
    private val prefs: SharedPreferences
) {
    companion object {
        private const val LAST_SYNC_KEY = "last_sync_time"
        private const val LAST_TRANSACTION_SYNC_KEY = "last_transaction_sync_time"
        private const val TAG = "SYNC"
    }

    suspend fun sync(userId: String) {
        try {
            Log.d(TAG, "========== SYNC START ==========")
            Log.d(TAG, "User ID: $userId")
            pushTasks(userId)
            pullTasks(userId)
            pushTransactions(userId)
            pullTransactions(userId)
            Log.d(TAG, "========== SYNC COMPLETE ==========")
        } catch (e: Exception) {
            Log.e(TAG, "========== SYNC FAILED ==========")
            Log.e(TAG, "Error: ${e.message}")
            e.printStackTrace()
        }
    }

    // ══════════════════════════════════════════
    // TASKS
    // ══════════════════════════════════════════

    private suspend fun pushTasks(userId: String) {
        Log.d(TAG, "--- TASK PUSH START ---")
        val unsynced = taskDao.getUnsyncedTasks()
        if (unsynced.isEmpty()) {
            Log.d(TAG, "Nothing to push")
            Log.d(TAG, "--- TASK PUSH COMPLETE ---")
            return
        }

        Log.d(TAG, "Pushing ${unsynced.size} tasks to server")
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
                            .toOffsetDateTime().toString()
                    }
                )
            }
        )

        try {
            api.push(payload)
            taskDao.markSynced(unsynced.map { it.taskId })
            Log.d(TAG, "Successfully pushed ${unsynced.size} tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Task push failed: ${e.message}")
            throw e
        }
        Log.d(TAG, "--- TASK PUSH COMPLETE ---")
    }

    private suspend fun pullTasks(userId: String) {
        Log.d(TAG, "--- TASK PULL START ---")

        val localTaskCount = taskDao.getTasksByUserOneShot(userId).size
        val lastSync = if (localTaskCount == 0) {
            Log.d(TAG, "DB is empty → forcing full sync")
            "2000-01-01T00:00:00"
        } else {
            prefs.getString(LAST_SYNC_KEY, "2000-01-01T00:00:00") ?: "2000-01-01T00:00:00"
        }

        Log.d(TAG, "Last sync: $lastSync")

        val response = try {
            api.pull(userId, lastSync)
        } catch (e: Exception) {
            Log.e(TAG, "Task pull failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "Received ${response.records.size} tasks from server")

        try {
            userDao.insertUser(User(userId = userId, name = "User", email = "", password = ""))
        } catch (e: Exception) {
            Log.d(TAG, "User already exists (OK)")
        }

        var upsertedCount = 0
        var deletedCount = 0
        var errorCount = 0

        response.records.forEach { remote ->
            try {
                if (remote.is_deleted) {
                    taskDao.deleteTaskById(remote.taskID)
                    deletedCount++
                    Log.d(TAG, "  ✓ DELETED task: ${remote.title}")
                } else {
                    val dueDateMillis = remote.dueDate?.let {
                        try {
                            java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            try {
                                java.time.LocalDateTime.parse(it)
                                    .atZone(java.time.ZoneId.of("UTC"))
                                    .toInstant().toEpochMilli()
                            } catch (e2: Exception) { null }
                        }
                    }

                    taskDao.upsertTask(
                        Task(
                            taskId = remote.taskID,
                            userId = userId,
                            title = remote.title,
                            description = remote.description,
                            updatedAt = remote.updated_at,
                            dueDate = dueDateMillis,
                            isDeleted = false,
                            is_synced = true
                        )
                    )
                    upsertedCount++
                    Log.d(TAG, "  ✓ UPSERTED task: ${remote.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process task: ${e.message}")
                errorCount++
            }
        }

        Log.d(TAG, "Tasks: $upsertedCount upserted, $deletedCount deleted, $errorCount errors")

        if (upsertedCount > 0 || deletedCount > 0) {
            prefs.edit().putString(LAST_SYNC_KEY, response.server_time).apply()
        }

        Log.d(TAG, "--- TASK PULL COMPLETE ---")
    }

    // ══════════════════════════════════════════
    // TRANSACTIONS
    // ══════════════════════════════════════════

    private suspend fun pushTransactions(userId: String) {
        Log.d(TAG, "--- TRANSACTION PUSH START ---")
        val unsynced = transactionDao.getUnsyncedTransactions()
        if (unsynced.isEmpty()) {
            Log.d(TAG, "Nothing to push")
            Log.d(TAG, "--- TRANSACTION PUSH COMPLETE ---")
            return
        }

        Log.d(TAG, "Pushing ${unsynced.size} transactions to server")
        val payload = TransactionPushPayload(
            records = unsynced.map { t ->
                TransactionSync(
                    transactionID = t.transactionId,
                    userID = t.userId,
                    title = t.title,
                    amount = t.amount,
                    date = java.time.Instant.ofEpochMilli(t.date)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toOffsetDateTime().toString(),
                    state = t.state.name,
                    income = t.income,
                    source = t.source ?: "",
                    description = t.description ?: "",
                    is_deleted = t.isDeleted,
                    updated_at = t.updatedAt
                )
            }
        )

        try {
            api.pushTransactions(payload)
            transactionDao.markSynced(unsynced.map { it.transactionId })
            Log.d(TAG, "Successfully pushed ${unsynced.size} transactions")
        } catch (e: Exception) {
            Log.e(TAG, "Transaction push failed: ${e.message}")
            throw e
        }
        Log.d(TAG, "--- TRANSACTION PUSH COMPLETE ---")
    }

    private suspend fun pullTransactions(userId: String) {
        Log.d(TAG, "--- TRANSACTION PULL START ---")

        val localCount = transactionDao.getTransactionsByUserOneShot(userId).size
        val lastSync = if (localCount == 0) {
            Log.d(TAG, "No local transactions → forcing full sync")
            "2000-01-01T00:00:00"
        } else {
            prefs.getString(LAST_TRANSACTION_SYNC_KEY, "2000-01-01T00:00:00")
                ?: "2000-01-01T00:00:00"
        }

        Log.d(TAG, "Last transaction sync: $lastSync")

        val response = try {
            api.pullTransactions(userId, lastSync)
        } catch (e: Exception) {
            Log.e(TAG, "Transaction pull failed: ${e.message}")
            throw e
        }

        Log.d(TAG, "Received ${response.records.size} transactions from server")

        var upsertedCount = 0
        var deletedCount = 0
        var errorCount = 0

        response.records.forEach { remote ->
            try {
                if (remote.is_deleted) {
                    transactionDao.deleteTransactionById(remote.transactionID)
                    deletedCount++
                    Log.d(TAG, "  ✓ DELETED transaction: ${remote.title}")
                } else {
                    // ← TIMEZONE FIX: parse with UTC then convert to system timezone
                    val dateMillis = try {
                        java.time.OffsetDateTime.parse(remote.date)
                            .toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        try {
                            java.time.LocalDateTime.parse(remote.date)
                                .atZone(java.time.ZoneId.of("UTC"))  // ← force UTC
                                .toInstant().toEpochMilli()
                        } catch (e2: Exception) {
                            Log.w(TAG, "Could not parse date: ${remote.date}, using now()")
                            System.currentTimeMillis()
                        }
                    }

                    val localDate = java.time.Instant.ofEpochMilli(dateMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    Log.d(TAG, "  Parsed date: ${remote.date} → $localDate")

                    val state = try {
                        TransactionState.valueOf(remote.state?.uppercase() ?: "PENDING")
                    } catch (e: Exception) {
                        TransactionState.PENDING
                    }

                    transactionDao.upsertTransaction(
                        Transaction(
                            transactionId = remote.transactionID,
                            userId = userId,
                            title = remote.title ?: "",
                            amount = remote.amount,
                            date = dateMillis,
                            source = remote.source,
                            description = remote.description,
                            income = remote.income ?: false,
                            state = state,
                            isDeleted = false,
                            is_synced = true,
                            updatedAt = remote.updated_at
                        )
                    )
                    upsertedCount++
                    Log.d(TAG, "  ✓ UPSERTED transaction: ${remote.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process transaction: ${e.message}")
                e.printStackTrace()
                errorCount++
            }
        }

        val afterSync = transactionDao.getTransactionsByUserOneShot(userId).size
        Log.d(TAG, "Transactions in DB after sync: $afterSync")
        Log.d(TAG, "Transactions: $upsertedCount upserted, $deletedCount deleted, $errorCount errors")

        if (upsertedCount > 0 || deletedCount > 0) {
            prefs.edit().putString(LAST_TRANSACTION_SYNC_KEY, response.server_time).apply()
        }

        Log.d(TAG, "--- TRANSACTION PULL COMPLETE ---")
    }
}