package com.example.qareeb.domain.usecase.transaction

import com.example.qareeb.data.entity.Task
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.repository.TaskRepository
import com.example.qareeb.domain.repository.TransactionRepository

class DeleteTransactionUseCase(private val transactionRepo: TransactionRepository) {
    suspend operator fun invoke(transaction: TransactionDomain): Long = transactionRepo.insertTransaction(transaction)
}
