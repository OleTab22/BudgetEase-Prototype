package com.budgetease.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Represents a monthly spending target for a specific category
@Entity(
    tableName = "budget_goals",
    indices = [Index(value = ["categoryId"])],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetGoal(
    // Unique identifier for the budget goal
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
    // Reference to the associated category
    val categoryId: Long,
    
    // Minimum target amount for this budget
    val minAmount: Double,
    
    // Maximum target amount for this budget
    val maxAmount: Double,
    
    // Month (1-12) this budget applies to
    val month: Int,
    
    // Year this budget applies to
    val year: Int,
    
    // Username of who created this budget
    val createdBy: String,
    
    // Timestamp when this budget was created
    val createdAt: Long = System.currentTimeMillis()
) 