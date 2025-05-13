package com.budgetease.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.User
import com.budgetease.data.preferences.SessionManager
import com.budgetease.data.preferences.SettingsRepository
import com.budgetease.data.preferences.ThemeMode
import com.budgetease.data.repository.UserRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// Manages user profile data, theme settings, and logout operations
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // State holders for UI updates
    private val _userState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val userState: StateFlow<UiState<User>> = _userState

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState

    private val _themeModeState = MutableStateFlow<ThemeMode>(ThemeMode.SYSTEM)
    val themeModeState: StateFlow<ThemeMode> = _themeModeState

    init {
        loadCurrentTheme()
    }

    // Load saved theme preference
    private fun loadCurrentTheme() {
        viewModelScope.launch {
            _themeModeState.value = settingsRepository.themeModeFlow.first()
        }
    }

    // Load user profile data
    fun loadUserProfile() {
        viewModelScope.launch {
            _userState.value = UiState.Loading
            val username = sessionManager.getSessionUsername()
            if (username != null) {
                userRepository.getUserByUsername(username)
                    .onSuccess { user ->
                        _userState.value = UiState.Success(user)
                    }
                    .onFailure { exception ->
                        _userState.value = UiState.Error(exception.message ?: "Failed to load user profile")
                    }
            } else {
                _userState.value = UiState.Error("User session not found. Please log in again.")
            }
        }
    }

    // Handle user logout
    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading
            try {
                sessionManager.clearSession()
                _logoutState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _logoutState.value = UiState.Error(e.message ?: "Logout failed")
            }
        }
    }

    // Reset logout state after navigation
    fun resetLogoutState() {
        _logoutState.value = UiState.Idle
    }

    // Save and update theme selection
    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.saveThemeMode(themeMode)
            _themeModeState.value = themeMode
        }
    }
} 