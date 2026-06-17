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

data class PromptSync(
    val prompt_id: String,      // ✅ match backend field name & type
    val userId: String,       // ✅ match backend: userId not userID
    val userMessage: String,
    val qareebResponse: String,
    val promptType: String,
    val module: String,
    val intentDetected: String? = null,
    val createdAt: Long,
    val metadata: String? = null
)

data class MemorySync(
    val memory_id: String,
    val userId: String,
    val fact: String,
    val createdAt: Long
)

data class PromptPushPayload(val records: List<PromptSync>)
data class MemoryPushPayload(val records: List<MemorySync>)

data class PromptPullResponse(val records: List<PromptSync>, val server_time: String)
data class MemoryPullResponse(val records: List<MemorySync>, val server_time: String)
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

    @GET("sync/pull/prompts")
    suspend fun pullPrompts(
        @Query("userID") userId: String,
        @Query("last_sync") lastSync: String
    ): PromptPullResponse

    @POST("sync/push/prompts")
    suspend fun pushPrompts(@Body payload: PromptPushPayload)

    @GET("sync/pull/memory")
    suspend fun pullMemory(@Query("userID") userId: String): MemoryPullResponse

    @POST("sync/push/memory")
    suspend fun pushMemory(@Body payload: MemoryPushPayload)
    data class FCMTokenRequest(
        val userID: String,
        val fcm_token: String
    )

    @POST("notifications/register-token")
    suspend fun registerFcmToken(@Body request: FCMTokenRequest)
}