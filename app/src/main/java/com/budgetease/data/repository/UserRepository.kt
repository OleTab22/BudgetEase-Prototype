package com.budgetease.data.repository

import com.budgetease.data.local.UserDao
import com.budgetease.data.model.User
import javax.inject.Inject

// Repository for managing user accounts with error handling
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    // Creates a new user account, fails if username exists
    suspend fun registerUser(username: String, password: String, fullName: String): Result<User> {
        return try {
            if (userDao.isUsernameExists(username)) {
                Result.failure(Exception("Username already exists"))
            } else {
                val user = User(username = username, password = password, fullName = fullName)
                userDao.insertUser(user)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Validates credentials and returns user if valid
    suspend fun loginUser(username: String, password: String): Result<User> {
        return try {
            val user = userDao.login(username, password)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Gets a user by their username, returns error if not found
    suspend fun getUserByUsername(username: String): Result<User> {
        return try {
            val user = userDao.getUserByUsername(username)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 