package com.budgetease.data.local

import androidx.room.*
import com.budgetease.data.model.Transaction
import kotlinx.coroutines.flow.Flow

// Data Access Object for transactions table
@Dao
interface TransactionDao {
    // Creates or replaces a transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    // Updates an existing transaction
    @Update
    suspend fun updateTransaction(transaction: Transaction)

    // Removes a transaction
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Gets all transactions for a user, sorted by date
    @Query("SELECT * FROM transactions WHERE createdBy = :username ORDER BY date DESC")
    fun getTransactionsByUser(username: String): Flow<List<Transaction>>

    // Gets transactions within a date range
    @Query("SELECT * FROM transactions WHERE createdBy = :username AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByPeriod(username: String, startDate: Long, endDate: Long): Flow<List<Transaction>>

    // Gets transactions for a specific category within a date range
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND createdBy = :username AND date BETWEEN :startDate AND :endDate")
    fun getTransactionsByCategory(categoryId: Long, username: String, startDate: Long, endDate: Long): Flow<List<Transaction>>

    // Gets total expenses for a category within a date range
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND createdBy = :username AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesByCategory(categoryId: Long, username: String, startDate: Long, endDate: Long): Flow<Double?>

    // Gets total income for a category within a date range
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND categoryId = :categoryId AND createdBy = :username AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncomeByCategory(categoryId: Long, username: String, startDate: Long, endDate: Long): Flow<Double?>

    // Gets transactions by type (INCOME/EXPENSE) within a date range
    @Query("SELECT * FROM transactions WHERE type = :type AND createdBy = :username AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByType(type: String, username: String, startDate: Long, endDate: Long): Flow<List<Transaction>>
} 