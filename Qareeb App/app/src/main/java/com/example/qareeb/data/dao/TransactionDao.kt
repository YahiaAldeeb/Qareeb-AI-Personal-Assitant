package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `transaction` WHERE user_id = :userId ORDER BY date DESC")
    fun getTransactionsByUser(userId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM `transaction` WHERE user_id = :userId AND category_id = :categoryId")
    fun getTransactionsByCategory(userId: Long, categoryId: Long): Flow<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}