package com.budgetease.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetease.data.model.User
import kotlinx.coroutines.flow.Flow

// Data Access Object for users table
@Dao
interface UserDao {
    // Creates a new user, fails if username exists
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    // Gets a user by their username
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    // Validates user credentials and returns user if valid
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    // Checks if a username is already taken
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    suspend fun isUsernameExists(username: String): Boolean
} 