package com.budgetease.data.local

import androidx.room.*
import com.budgetease.data.model.BudgetGoal
import kotlinx.coroutines.flow.Flow

// Data Access Object for budget goals table
@Dao
interface BudgetGoalDao {
    // Creates or replaces a budget goal
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal): Long

    // Updates an existing budget goal
    @Update
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal)

    // Removes a budget goal
    @Delete
    suspend fun deleteBudgetGoal(budgetGoal: BudgetGoal)

    // Gets all budget goals for a specific month and year by username
    @Query("SELECT * FROM budget_goals WHERE createdBy = :username AND month = :month AND year = :year")
    fun getBudgetGoalsByPeriod(username: String, month: Int, year: Int): Flow<List<BudgetGoal>>

    // Gets a specific category's budget goal for a given month and year
    @Query("SELECT * FROM budget_goals WHERE categoryId = :categoryId AND createdBy = :username AND month = :month AND year = :year LIMIT 1")
    fun getBudgetGoalForCategory(categoryId: Long, username: String, month: Int, year: Int): Flow<BudgetGoal?>
} 