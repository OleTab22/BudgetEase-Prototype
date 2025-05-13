package com.budgetease

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.budgetease.data.preferences.SettingsRepository
import com.budgetease.data.preferences.ThemeMode
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// Custom Application class for BudgetEase
@HiltAndroidApp
class BudgetEaseApplication : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository // Manages app settings, including theme.
    // Coroutine scope for application-level tasks that should live as long as the application.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // Applies the saved theme preference when the application starts.
        applyThemePreference()
    }

    // Fetches the current theme preference and applies it.
    // Also observes future theme changes to update the app theme dynamically.
    private fun applyThemePreference() {
        // Apply the initial theme based on the stored preference.
        applicationScope.launch {
            val themeMode = settingsRepository.themeModeFlow.first() // Get the current theme preference.
            applyTheme(themeMode)
        }

        // Observe the theme preference flow for any subsequent changes.
        // This allows the theme to update if changed while the app is running (may require activity recreation).
        applicationScope.launch {
            settingsRepository.themeModeFlow.collect { themeMode ->
                applyTheme(themeMode)
            }
        }
    }

    // Sets the application's night mode based on the provided ThemeMode.
    private fun applyTheme(themeMode: ThemeMode) {
        val mode = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode) // Applies the chosen theme mode globally.
    }
} 