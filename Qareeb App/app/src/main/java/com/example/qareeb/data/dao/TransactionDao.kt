package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.domain.model.enums.TransactionState
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM `transaction` WHERE userID = :userId AND is_deleted = 0 ORDER BY date DESC")
    fun getTransactionsByUser(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM `transaction` WHERE userID = :userId AND is_deleted = 0 ORDER BY date DESC")
    suspend fun getTransactionsByUserOneShot(userId: String): List<Transaction>

    @Query("SELECT * FROM `transaction` WHERE userID = :userId AND category_id = :categoryId AND is_deleted = 0")
    fun getTransactionsByCategory(userId: String, categoryId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM `transaction` WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("UPDATE `transaction` SET is_synced = 1 WHERE transaction_id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM `transaction` WHERE transaction_id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: Transaction)

    @Query("SELECT * FROM `transaction` WHERE userID = :userId AND income = :isIncome AND is_deleted = 0")
    fun getTransactionsByType(userId: String, isIncome: Boolean): Flow<List<Transaction>>

    @Query("SELECT * FROM `transaction` WHERE userID = :userId AND state = :state AND is_deleted = 0")
    fun getTransactionsByState(userId: String, state: TransactionState): Flow<List<Transaction>>
}