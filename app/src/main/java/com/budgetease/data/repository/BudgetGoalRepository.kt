package com.budgetease.data.repository

import com.budgetease.data.local.BudgetGoalDao
import com.budgetease.data.model.BudgetGoal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Repository for managing budget goals with error handling
class BudgetGoalRepository @Inject constructor(
    private val budgetGoalDao: BudgetGoalDao
) {
    // Creates a new budget goal and returns its ID
    suspend fun setBudgetGoal(
        categoryId: Long,
        minAmount: Double,
        maxAmount: Double,
        month: Int,
        year: Int,
        username: String
    ): Result<Long> {
        return try {
            val budgetGoal = BudgetGoal(
                categoryId = categoryId,
                minAmount = minAmount,
                maxAmount = maxAmount,
                month = month,
                year = year,
                createdBy = username
            )
            val id = budgetGoalDao.insertBudgetGoal(budgetGoal)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updates an existing budget goal
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal): Result<Unit> {
        return try {
            budgetGoalDao.updateBudgetGoal(budgetGoal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deletes a budget goal
    suspend fun deleteBudgetGoal(budgetGoal: BudgetGoal): Result<Unit> {
        return try {
            budgetGoalDao.deleteBudgetGoal(budgetGoal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets all budget goals for a specific month and year
    fun getBudgetGoalsByPeriod(
        username: String,
        month: Int,
        year: Int
    ): Flow<List<BudgetGoal>> {
        return budgetGoalDao.getBudgetGoalsByPeriod(username, month, year)
    }

    // Gets a specific category's budget goal for a given month and year
    fun getBudgetGoalForCategory(
        categoryId: Long,
        username: String,
        month: Int,
        year: Int
    ): Flow<BudgetGoal?> {
        return budgetGoalDao.getBudgetGoalForCategory(categoryId, username, month, year)
    }
} 