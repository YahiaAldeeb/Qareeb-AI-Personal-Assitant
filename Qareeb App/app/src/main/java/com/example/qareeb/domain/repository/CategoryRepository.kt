package com.example.qareeb.domain.repository

import com.example.qareeb.data.entity.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun getCategoryByName(name: String): Category?
}