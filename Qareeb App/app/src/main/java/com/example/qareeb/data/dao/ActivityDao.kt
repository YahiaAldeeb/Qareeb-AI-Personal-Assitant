package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivities(userId: String, limit: Int = 50): Flow<List<Activity>>

    @Query("SELECT * FROM activity WHERE task_id = :taskId ORDER BY timestamp DESC")
    fun getActivitiesByTask(taskId: String): Flow<List<Activity>>

    @Insert
    suspend fun insertActivity(activity: Activity): Long
}