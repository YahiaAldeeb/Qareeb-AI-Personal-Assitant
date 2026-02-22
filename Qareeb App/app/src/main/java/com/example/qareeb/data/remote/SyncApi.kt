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

data class PushPayload(
    val records: List<TaskSync>
)

data class PullResponse(
    val records: List<TaskSync>,
    val server_time: String
)

interface SyncApi {
    @GET("sync/pull")
    suspend fun pull(
        @Query("user_id") userId: String,
        @Query("last_sync") lastSync: String
    ): PullResponse

    @POST("sync/push")
    suspend fun push(@Body payload: PushPayload)
}