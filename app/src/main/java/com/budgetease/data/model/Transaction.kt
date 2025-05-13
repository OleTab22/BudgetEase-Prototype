package com.budgetease.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Represents a financial transaction in the database
@Entity(
    tableName = "transactions",
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
data class Transaction(
    // Unique identifier for the transaction
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
    // Transaction amount (positive for income, negative for expense)
    val amount: Double,
    
    // Description of the transaction
    val description: String,
    
    // Reference to associated category
    val categoryId: Long,
    
    // Date when transaction occurred
    val date: Long,
    
    // Optional path to receipt image
    val imagePath: String?,
    
    // Transaction type: "EXPENSE" or "INCOME"
    val type: String,
    
    // Username of who created this transaction
    val createdBy: String,
    
    // Timestamp when this transaction was created
    val createdAt: Long = System.currentTimeMillis()
) 