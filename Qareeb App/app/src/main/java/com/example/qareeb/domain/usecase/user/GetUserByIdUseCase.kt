package com.example.qareeb.domain.usecase.user
import com.example.qareeb.domain.repository.UserRepository

class GetUserByIdUseCase(private val userRepo: UserRepository) {
     operator fun invoke(id: Long) = userRepo.getUserById(id)
}
