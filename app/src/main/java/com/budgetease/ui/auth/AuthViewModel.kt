package com.budgetease.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.User
import com.budgetease.data.repository.UserRepository
import com.budgetease.ui.common.UiState
import com.budgetease.data.preferences.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow

// Handles user authentication operations
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Login state
    private val _loginState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val loginState: StateFlow<UiState<User>> = _loginState

    // Registration state
    private val _registrationState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val registrationState: StateFlow<UiState<User>> = _registrationState

    // Password reset state
    private val _resetPasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resetPasswordState: StateFlow<UiState<Unit>> = _resetPasswordState.asStateFlow()

    // Logs in user with credentials
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            userRepository.loginUser(username, password)
                .onSuccess { user ->
                    _loginState.value = UiState.Success(user)
                }
                .onFailure { exception ->
                    _loginState.value = UiState.Error(exception.message ?: "Login failed")
                }
        }
    }

    // Registers new user
    fun register(username: String, password: String, fullName: String) {
        viewModelScope.launch {
            _registrationState.value = UiState.Loading
            userRepository.registerUser(username, password, fullName)
                .onSuccess { user ->
                    _registrationState.value = UiState.Success(user)
                }
                .onFailure { exception ->
                    _registrationState.value = UiState.Error(exception.message ?: "Registration failed")
                }
        }
    }

    // Resets login state
    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    // Resets registration state
    fun resetRegistrationState() {
        _registrationState.value = UiState.Idle
    }

    // Logs out user
    fun logout() {
        sessionManager.clearSession()
        _loginState.value = UiState.Idle
        _registrationState.value = UiState.Idle
    }

    // Simulates password reset
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = UiState.Loading
            if (email.isBlank()) {
                _resetPasswordState.value = UiState.Error("Email cannot be empty.")
                return@launch
            }
            delay(1500)
            _resetPasswordState.value = UiState.Success(Unit)
        }
    }
} 