package com.budgetease.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a transaction category in the database
@Entity(tableName = "categories")
data class Category(
    // Unique identifier for the category
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
    // Display name of the category
    val name: String,
    
    // Color used for visual representation
    val color: Int,
    
    // Icon identifier for the category
    val icon: String,
    
    // Username of who created this category
    val createdBy: String,
    
    // Timestamp when this category was created
    val createdAt: Long = System.currentTimeMillis()
) {
    // Returns category name for string representation
    override fun toString(): String {
        return name
    }
} 