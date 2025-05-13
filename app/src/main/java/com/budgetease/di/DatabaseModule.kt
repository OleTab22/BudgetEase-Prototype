package com.budgetease.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetease.data.local.*
import com.budgetease.data.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Color
import javax.inject.Provider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Dagger Hilt module for database-related dependency injection
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Provides singleton instance of the Room database
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>
    ): BudgetEaseDatabase {
        return Room.databaseBuilder(
            context,
            BudgetEaseDatabase::class.java,
            "budgetease.db"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val categoryDao = categoryDaoProvider.get()
                    prePopulateCategories(categoryDao)
                }
            }
        })
        .build()
    }

    // Creates default categories when database is first created
    private suspend fun prePopulateCategories(categoryDao: CategoryDao) {
        val defaultCategories = listOf(
            Category(name = "Salary", color = Color.parseColor("#4CAF50"), icon = "ic_salary", createdBy = "system_default"),
            Category(name = "Food", color = Color.parseColor("#FF9800"), icon = "ic_food", createdBy = "system_default"),
            Category(name = "Transport", color = Color.parseColor("#2196F3"), icon = "ic_transport", createdBy = "system_default"),
            Category(name = "Groceries", color = Color.parseColor("#8BC34A"), icon = "ic_shopping", createdBy = "system_default"),
            Category(name = "Utilities", color = Color.parseColor("#00BCD4"), icon = "ic_default_category", createdBy = "system_default"),
            Category(name = "Rent/Mortgage", color = Color.parseColor("#795548"), icon = "ic_default_category", createdBy = "system_default"),
            Category(name = "Entertainment", color = Color.parseColor("#E91E63"), icon = "ic_entertainment", createdBy = "system_default"),
            Category(name = "Health", color = Color.parseColor("#F44336"), icon = "ic_health", createdBy = "system_default"),
            Category(name = "Education", color = Color.parseColor("#3F51B5"), icon = "ic_education", createdBy = "system_default"),
            Category(name = "Savings", color = Color.parseColor("#FFC107"), icon = "ic_saving", createdBy = "system_default"),
            Category(name = "Other", color = Color.parseColor("#607D8B"), icon = "ic_other", createdBy = "system_default")
        )
        defaultCategories.forEach { categoryDao.insertCategory(it) }
    }

    // Provides access to user-related database operations
    @Provides
    fun provideUserDao(database: BudgetEaseDatabase): UserDao = database.userDao()

    // Provides access to category-related database operations
    @Provides
    fun provideCategoryDao(database: BudgetEaseDatabase): CategoryDao = database.categoryDao()

    // Provides access to transaction-related database operations
    @Provides
    fun provideTransactionDao(database: BudgetEaseDatabase): TransactionDao = database.transactionDao()

    // Provides access to budget goal-related database operations
    @Provides
    fun provideBudgetGoalDao(database: BudgetEaseDatabase): BudgetGoalDao = database.budgetGoalDao()
} 