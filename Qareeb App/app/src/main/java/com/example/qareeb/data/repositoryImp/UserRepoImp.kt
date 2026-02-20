package com.example.qareeb.data.repositoryImp

import com.example.qareeb.data.dao.UserDao
import com.example.qareeb.data.mapper.toDomain
import com.example.qareeb.data.mapper.toEntity
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(private val userDao: UserDao) : UserRepository {

    override fun getUserById(userId: String): Flow<UserDomain?> {
        return userDao.getUserById(userId).map { it?.toDomain() }
    }

    override suspend fun insertUser(user: UserDomain): Long {
        return userDao.insertUser(user.toEntity())
    }

    override suspend fun updateUser(user: UserDomain) {
        userDao.updateUser(user.toEntity())
    }

    override suspend fun deleteUser(user: UserDomain) {
        userDao.deleteUser(user.toEntity())
    }
}