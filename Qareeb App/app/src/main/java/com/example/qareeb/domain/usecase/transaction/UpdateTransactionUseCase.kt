package com.example.qareeb.domain.usecase.transaction
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.repository.TransactionRepository

class UpdateTransactionUseCase(private val transactionRepo: TransactionRepository) {
    suspend operator fun invoke(transaction: TransactionDomain) = transactionRepo.updateTransaction(transaction)
}