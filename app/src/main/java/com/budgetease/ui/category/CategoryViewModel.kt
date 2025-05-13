package com.budgetease.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.Category
import com.budgetease.data.repository.CategoryRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Manages category data operations and UI state
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // State flows for UI updates
    private val _categoriesState = MutableStateFlow<UiState<List<Category>>>(UiState.Idle)
    val categoriesState: StateFlow<UiState<List<Category>>> = _categoriesState.asStateFlow()

    private val _saveCategoryState = MutableStateFlow<UiState<Long>>(UiState.Idle)
    val saveCategoryState: StateFlow<UiState<Long>> = _saveCategoryState.asStateFlow()

    // Loads categories for a specific user
    fun loadCategories(username: String) {
        viewModelScope.launch {
            _categoriesState.value = UiState.Loading
            categoryRepository.getCategoriesByUser(username).collect {
                _categoriesState.value = UiState.Success(it)
            }
        }
    }

    // Creates new or updates existing category
    fun saveCategory(
        existingCategory: Category?,
        newCategoryName: String?,
        newCategoryColor: Int?,
        newCategoryIcon: String?,
        username: String
    ) {
        viewModelScope.launch {
            _saveCategoryState.value = UiState.Loading
            try {
                if (existingCategory != null) {
                    categoryRepository.updateCategory(existingCategory)
                        .onSuccess { _saveCategoryState.value = UiState.Success(-1L) }
                        .onFailure { throw it }
                } else if (newCategoryName != null && newCategoryColor != null && newCategoryIcon != null) {
                    categoryRepository.createCategory(
                        name = newCategoryName,
                        color = newCategoryColor,
                        icon = newCategoryIcon,
                        username = username
                    )
                        .onSuccess { newId -> _saveCategoryState.value = UiState.Success(newId) }
                        .onFailure { throw it }
                } else {
                    throw IllegalArgumentException("Invalid parameters for saveCategory")
                }
            } catch (e: Exception) {
                _saveCategoryState.value = UiState.Error(e.message ?: "Failed to save category")
            }
        }
    }

    // Deletes a category and updates state
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
                    .onFailure { 
                        _categoriesState.value = UiState.Error(it.message ?: "Failed to delete category") 
                    }
            } catch (e: Exception) {
                _categoriesState.value = UiState.Error(e.message ?: "Failed to delete category")
            }
        }
    }

    // Resets category save operation state
    fun resetSaveCategoryState() {
        _saveCategoryState.value = UiState.Idle
    }
} 