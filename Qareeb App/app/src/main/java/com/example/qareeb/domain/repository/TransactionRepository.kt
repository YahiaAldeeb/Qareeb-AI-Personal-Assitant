package com.example.qareeb.domain.repository

import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.model.enums.TransactionState
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    fun getTransactionsByUser(userId: Long): Flow<List<TransactionDomain>>
    suspend fun insertTransaction(transaction: TransactionDomain): Long
    suspend fun updateTransaction(transaction: TransactionDomain)
    suspend fun deleteTransaction(transaction: TransactionDomain)

    suspend fun getTransactionsByState(userId:Long,state: TransactionState): Flow<List<TransactionDomain>>
}