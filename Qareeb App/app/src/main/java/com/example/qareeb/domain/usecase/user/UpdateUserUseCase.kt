package com.example.qareeb.domain.usecase.user
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.UserRepository
class UpdateUserUseCase(private val userRepo: UserRepository) {
    suspend operator fun invoke(user: UserDomain) = userRepo.updateUser(user)
}