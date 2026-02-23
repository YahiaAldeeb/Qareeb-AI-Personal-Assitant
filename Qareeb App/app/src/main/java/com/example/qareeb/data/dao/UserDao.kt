package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE userID = :userId")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM user WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUserByEmail(email: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM user WHERE userID = :userId AND voice_embedding IS NOT NULL")
    suspend fun getUserWithVoiceBiometric(userId: String): User?

    @Query("UPDATE user SET voice_embedding = :embedding WHERE userID = :userId")
    suspend fun updateVoiceEmbedding(userId: String, embedding: String)
}