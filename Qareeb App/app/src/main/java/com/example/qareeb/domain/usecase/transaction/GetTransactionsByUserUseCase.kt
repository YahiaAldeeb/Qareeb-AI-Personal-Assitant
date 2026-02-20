package com.example.qareeb.domain.usecase.transaction


import com.example.qareeb.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsByUserUseCase(private val transactionRepo: TransactionRepository) {
     operator fun invoke(userId: String) = transactionRepo.getTransactionsByUser(userId)
}
