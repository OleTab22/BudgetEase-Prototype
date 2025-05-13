package com.budgetease.data.preferences

import androidx.datastore.preferences.core.stringPreferencesKey

// Constants for theme-related preferences
object ThemePreferences {
    // Key for storing the selected theme mode
    val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
}

// Available theme modes for the app
enum class ThemeMode {
    LIGHT,  // Light theme
    DARK,   // Dark theme
    SYSTEM  // Follow system theme
} 