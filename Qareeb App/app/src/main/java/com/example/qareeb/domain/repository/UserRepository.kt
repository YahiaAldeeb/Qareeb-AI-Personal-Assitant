package com.example.qareeb.domain.repository

import com.example.qareeb.domain.model.UserDomain
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<UserDomain?>
    suspend fun insertUser(User: UserDomain): Long
    suspend fun updateUser(User: UserDomain)
    suspend fun deleteUser(User: UserDomain)
}