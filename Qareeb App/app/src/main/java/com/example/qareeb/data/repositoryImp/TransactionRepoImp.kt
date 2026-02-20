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

    override fun getTransactionsByUser(userId: Long): Flow<List<TransactionDomain>> {
        return transactionDao.getTransactionsByUser(userId).map { list ->
            list.map { it.toDomain() }  // ← map each item in the list
        }
    }

    override suspend fun insertTransaction(transaction: TransactionDomain): Long {
        return transactionDao.insertTransaction(transaction.toEntity())  // ← convert to entity
    }

    override suspend fun updateTransaction(transaction: TransactionDomain) {
        transactionDao.updateTransaction(transaction.toEntity())  // ← convert to entity
    }

    override suspend fun deleteTransaction(transaction: TransactionDomain) {
        transactionDao.deleteTransaction(transaction.toEntity())  // ← convert to entity
    }

    override suspend fun getTransactionsByState(
        userId: Long,
        state: TransactionState
    ): Flow<List<TransactionDomain>> {
        return transactionDao.getTransactionsByState(userId, state).map { list ->
            list.map { it.toDomain() }  // ← convert to domain
        }
    }
}