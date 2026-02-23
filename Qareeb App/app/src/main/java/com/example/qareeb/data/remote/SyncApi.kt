package com.example.qareeb.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class TaskSync(
    val taskID: String,
    val userID: String,
    val title: String,
    val description: String? = null,
    val updated_at: String,
    val is_deleted: Boolean = false,
    val dueDate: String? = null
)

data class TransactionSync(
    val transactionID: String,
    val userID: String,
    val title: String?,
    val amount: Double,
    val date: String,
    val state: String? = null,
    val income: Boolean? = false,        // ← add this
    val source: String? = null,          // ← add this
    val description: String? = null,     // ← add this
    val is_deleted: Boolean = false,
    val updated_at: String
)

data class PushPayload(
    val records: List<TaskSync>
)

data class TransactionPushPayload(
    val records: List<TransactionSync>
)

data class PullResponse(
    val records: List<TaskSync>,
    val server_time: String
)

data class TransactionPullResponse(
    val records: List<TransactionSync>,
    val server_time: String
)

interface SyncApi {
    @GET("sync/pull")
    suspend fun pull(
        @Query("userID") userId: String,
        @Query("last_sync") lastSync: String
    ): PullResponse

    @POST("sync/push")
    suspend fun push(@Body payload: PushPayload)

    @GET("sync/pull/transactions")
    suspend fun pullTransactions(
        @Query("userID") userId: String,
        @Query("last_sync") lastSync: String
    ): TransactionPullResponse

    @POST("sync/push/transactions")
    suspend fun pushTransactions(@Body payload: TransactionPushPayload)
}