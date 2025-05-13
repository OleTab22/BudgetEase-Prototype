package com.budgetease.ui.common

// Handles different states of UI data loading with generic type T
sealed class UiState<out T> {
    // Loading state while waiting for data
    object Loading : UiState<Nothing>()

    // Default state before any data operations
    object Idle : UiState<Nothing>()

    // Success state containing loaded data
    data class Success<T>(val data: T) : UiState<T>()

    // Error state with failure message
    data class Error(val message: String) : UiState<Nothing>()
} 