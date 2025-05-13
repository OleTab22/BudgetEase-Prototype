package com.budgetease.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Create DataStore instance for app settings
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Repository for managing app settings using DataStore
@Singleton
class SettingsRepository @Inject constructor(private val context: Context) {

    // Stream of theme mode changes, defaults to light theme
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map {
            val themeString = it[ThemePreferences.THEME_MODE_KEY] ?: ThemeMode.LIGHT.name
            try {
                ThemeMode.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                ThemeMode.LIGHT
            }
        }

    // Saves the selected theme mode to persistent storage
    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit {
            it[ThemePreferences.THEME_MODE_KEY] = themeMode.name
        }
    }
} 