package com.budgetease.data.local

import androidx.room.*
import com.budgetease.data.model.Category
import kotlinx.coroutines.flow.Flow

// Data Access Object for categories table
@Dao
interface CategoryDao {
    // Creates or replaces a category
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    // Updates an existing category
    @Update
    suspend fun updateCategory(category: Category)

    // Removes a category
    @Delete
    suspend fun deleteCategory(category: Category)

    // Gets all categories for a specific user
    @Query("SELECT * FROM categories WHERE createdBy = :username")
    fun getCategoriesByUser(username: String): Flow<List<Category>>

    // Gets a category by its ID
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?
} 