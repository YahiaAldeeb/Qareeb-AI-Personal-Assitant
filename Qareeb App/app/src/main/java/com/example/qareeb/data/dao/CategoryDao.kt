package com.example.qareeb.data.dao

import androidx.room.*
import com.example.qareeb.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}