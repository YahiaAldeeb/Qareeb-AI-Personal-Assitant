package com.example.qareeb.data.repositoryImp

import com.example.qareeb.data.dao.CategoryDao
import com.example.qareeb.data.entity.Category
import com.example.qareeb.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    override suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }
}