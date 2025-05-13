package com.budgetease.data.repository

import com.budgetease.data.local.CategoryDao
import com.budgetease.data.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Repository for managing transaction categories with error handling
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    // Creates a new category and returns its ID
    suspend fun createCategory(
        name: String,
        color: Int,
        icon: String,
        username: String
    ): Result<Long> {
        return try {
            val category = Category(
                name = name,
                color = color,
                icon = icon,
                createdBy = username
            )
            val id = categoryDao.insertCategory(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updates an existing category
    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            categoryDao.updateCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deletes a category and its associated budget goals
    suspend fun deleteCategory(category: Category): Result<Unit> {
        return try {
            categoryDao.deleteCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets all categories created by a specific user
    fun getCategoriesByUser(username: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByUser(username)
    }

    // Gets a category by its ID, returns error if not found
    suspend fun getCategoryById(categoryId: Long): Result<Category> {
        return try {
            val category = categoryDao.getCategoryById(categoryId)
            if (category != null) {
                Result.success(category)
            } else {
                Result.failure(Exception("Category not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 