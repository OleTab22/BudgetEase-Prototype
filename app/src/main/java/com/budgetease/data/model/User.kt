package com.budgetease.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a user account in the database
@Entity(tableName = "users")
data class User(
    // Unique username that identifies the user
    @PrimaryKey val username: String,
    
    // User's full display name
    val fullName: String,
    
    // User's account password
    val password: String,
    
    // Timestamp when this account was created
    val createdAt: Long = System.currentTimeMillis()
) 