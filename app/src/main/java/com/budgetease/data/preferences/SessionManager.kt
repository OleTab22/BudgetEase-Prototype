package com.budgetease.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Manages user session data using SharedPreferences
@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    // SharedPreferences instance for storing session data
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        // Preference file name
        private const val PREFS_NAME = "budget_ease_prefs"
        // Keys for storing session data
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
        private const val KEY_USERNAME = "username"
    }

    // Updates the user's login status
    fun setUserLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    // Checks if a user is currently logged in
    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Updates the onboarding completion status
    fun setOnboardingCompleted(hasCompleted: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, hasCompleted).apply()
    }

    // Checks if user has completed onboarding
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
    }

    // Saves the username of the current session
    fun saveSessionUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    // Gets the username of the current session
    fun getSessionUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    // Clears all session data
    fun clearSession() {
        prefs.edit().clear().apply()
    }
} 