package com.budgetease.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.budgetease.data.model.BudgetGoal
import com.budgetease.data.model.Category
import com.budgetease.data.model.Transaction
import com.budgetease.data.model.User

// Main database class for the BudgetEase application.
// Contains tables for users, categories, transactions and budget goals.
@Database(
    entities = [
        User::class,
        Category::class,
        Transaction::class,
        BudgetGoal::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BudgetEaseDatabase : RoomDatabase() {
    // Returns DAO for accessing and modifying user data
    abstract fun userDao(): UserDao

    // Returns DAO for accessing and modifying category data
    abstract fun categoryDao(): CategoryDao

    // Returns DAO for accessing and modifying transaction data
    abstract fun transactionDao(): TransactionDao

    // Returns DAO for accessing and modifying budget goals data
    abstract fun budgetGoalDao(): BudgetGoalDao
}