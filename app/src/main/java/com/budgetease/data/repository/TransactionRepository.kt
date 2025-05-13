package com.budgetease.data.repository

import com.budgetease.data.local.TransactionDao
import com.budgetease.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Repository for managing financial transactions with error handling
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    // Creates a new transaction and returns its ID
    suspend fun addTransaction(
        amount: Double,
        description: String,
        categoryId: Long,
        date: Long,
        imagePath: String?,
        type: String,
        username: String
    ): Result<Long> {
        return try {
            val transaction = Transaction(
                amount = amount,
                description = description,
                categoryId = categoryId,
                date = date,
                imagePath = imagePath,
                type = type,
                createdBy = username
            )
            val id = transactionDao.insertTransaction(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updates an existing transaction
    suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            transactionDao.updateTransaction(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deletes a transaction
    suspend fun deleteTransaction(transaction: Transaction): Result<Unit> {
        return try {
            transactionDao.deleteTransaction(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets all transactions for a user, sorted by date
    fun getTransactionsByUser(username: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByUser(username)
    }

    // Gets transactions within a date range
    fun getTransactionsByPeriod(
        username: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByPeriod(username, startDate, endDate)
    }

    // Gets transactions by type (INCOME/EXPENSE) within a date range
    fun getTransactionsByType(
        type: String,
        username: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type, username, startDate, endDate)
    }

    // Gets transactions for a specific category within a date range
    fun getTransactionsByCategory(
        categoryId: Long,
        username: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId, username, startDate, endDate)
    }

    // Gets total expenses for a category within a date range
    fun getTotalExpensesByCategory(
        categoryId: Long,
        username: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> {
        return transactionDao.getTotalExpensesByCategory(categoryId, username, startDate, endDate)
    }

    // Gets total income for a category within a date range
    fun getTotalIncomeByCategory(
        categoryId: Long,
        username: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> {
        return transactionDao.getTotalIncomeByCategory(categoryId, username, startDate, endDate)
    }
} 