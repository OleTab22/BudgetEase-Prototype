package com.budgetease.ui.transaction

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.Transaction
import com.budgetease.data.repository.TransactionRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.budgetease.data.model.Category
import com.budgetease.data.repository.CategoryRepository
import kotlinx.coroutines.flow.catch

// ViewModel for handling transaction creation operations
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    // Holds the URI of the receipt image selected by user
    private val _receiptImageUri = MutableLiveData<Uri?>()
    val receiptImageUri: LiveData<Uri?> = _receiptImageUri

    // Tracks the state of transaction addition operation
    private val _addTransactionState = MutableStateFlow<UiState<Long>>(UiState.Idle)
    val addTransactionState: StateFlow<UiState<Long>> = _addTransactionState

    // Tracks the state of categories loading operation
    private val _categoriesState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categoriesState: StateFlow<UiState<List<Category>>> = _categoriesState

    // Updates the receipt image URI
    fun setReceiptImageUri(uri: Uri?) {
        _receiptImageUri.value = uri
    }

    // Loads categories for a specific user
    fun loadCategories(username: String) {
        viewModelScope.launch {
            _categoriesState.value = UiState.Loading
            categoryRepository.getCategoriesByUser(username)
                .catch { exception ->
                    _categoriesState.value = UiState.Error(exception.message ?: "Failed to load categories")
                }
                .collect { categories ->
                    _categoriesState.value = UiState.Success(categories)
                }
        }
    }

    // Creates a new transaction with the provided details
    fun addTransaction(
        amount: Double,
        description: String,
        categoryId: Long,
        date: Long,
        imagePath: String?,
        type: String,
        username: String
    ) {
        viewModelScope.launch {
            _addTransactionState.value = UiState.Loading
            transactionRepository.addTransaction(
                amount = amount,
                description = description,
                categoryId = categoryId,
                date = date,
                imagePath = imagePath,
                type = type,
                username = username
            ).onSuccess { id ->
                _addTransactionState.value = UiState.Success(id)
            }.onFailure { exception ->
                _addTransactionState.value = UiState.Error(exception.message ?: "Failed to add transaction")
            }
        }
    }

    // Resets the transaction addition state to idle
    fun resetAddTransactionState() {
        _addTransactionState.value = UiState.Idle
    }

    companion object {
        // Constants for transaction types
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
    }
} 