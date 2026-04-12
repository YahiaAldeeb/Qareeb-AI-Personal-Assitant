package com.example.qareeb.domain.usecase

import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.UserRepository

class UpdateVoiceEmbeddingUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: UserDomain, embedding: ByteArray) {
        userRepository.updateVoice(user, embedding)
    }
}