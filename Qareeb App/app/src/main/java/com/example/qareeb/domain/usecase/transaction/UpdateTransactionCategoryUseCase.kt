package com.example.qareeb.domain.usecase.transaction

import com.example.qareeb.domain.repository.TransactionRepository

class UpdateTransactionCategoryUseCase(
    private val transactionRepo: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String, categoryId: String) {
        transactionRepo.updateTransactionCategoryId(transactionId, categoryId)
    }
}