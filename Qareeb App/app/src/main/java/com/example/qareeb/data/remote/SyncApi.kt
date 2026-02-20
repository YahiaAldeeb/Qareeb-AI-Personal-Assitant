package com.example.qareeb.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {

    @GET("/sync/pull")
    suspend fun pull(
        @Query("userID") userId: String,
        @Query("last_sync") lastSync: String
    ): PullResponse

    @POST("/sync/push")
    suspend fun push(@Body payload: PushPayload): PushResponse
}

data class TaskSync(
    val taskID: String,
    val userID: String,
    val title: String,
    val description: String? = null,
    val status: String = "PENDING",        // send as String for JSON compatibility
    val progressPercentage: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: String? = null,
    val dueDate: Long? = null,
    val updated_at: String,
    val is_deleted: Boolean = false
)

data class PushPayload(val records: List<TaskSync>)
data class PullResponse(val records: List<TaskSync>, val server_time: String)
data class PushResponse(val status: String)