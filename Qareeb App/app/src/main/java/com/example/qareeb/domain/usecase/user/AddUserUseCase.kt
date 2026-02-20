package com.example.qareeb.domain.usecase.user

import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.TransactionRepository
import com.example.qareeb.domain.repository.UserRepository

class AddUserUseCase(private val userRepo: UserRepository) {
    suspend operator fun invoke(user: UserDomain): Long = userRepo.insertUser(user)
}
