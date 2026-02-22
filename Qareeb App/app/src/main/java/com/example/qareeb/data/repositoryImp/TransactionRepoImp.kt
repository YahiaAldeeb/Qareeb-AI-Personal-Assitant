package com.example.qareeb.data.repositoryImp

import com.example.qareeb.data.dao.TransactionDao
import com.example.qareeb.data.mapper.toDomain
import com.example.qareeb.data.mapper.toEntity
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(private val transactionDao: TransactionDao) : TransactionRepository {

    override fun getTransactionsByUser(userId: String): Flow<List<TransactionDomain>> {
        return transactionDao.getTransactionsByUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertTransaction(transaction: TransactionDomain): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: TransactionDomain) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: TransactionDomain) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun getTransactionsByState(
        userId: String,          // ← changed Long to String
        state: TransactionState
    ): Flow<List<TransactionDomain>> {
        return transactionDao.getTransactionsByState(userId, state).map { list ->
            list.map { it.toDomain() }
        }
    }
}