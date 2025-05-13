package com.budgetease.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.TransactionDisplayable
import com.budgetease.data.preferences.SessionManager
import com.budgetease.data.repository.CategoryRepository
import com.budgetease.data.repository.TransactionRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ViewModel responsible for managing transactions list with filtering capabilities
@HiltViewModel
class AllTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // UI state for the list of transactions with their display information
    private val _allTransactionsState = MutableStateFlow<UiState<List<TransactionDisplayable>>>(UiState.Idle)
    val allTransactionsState: StateFlow<UiState<List<TransactionDisplayable>>> = _allTransactionsState

    // Date range filter states
    private val _filterStartDate = MutableStateFlow<Long?>(null)
    private val _filterEndDate = MutableStateFlow<Long?>(null)

    // Updates date filter and reloads transactions
    fun setTransactionPeriod(startDate: Long?, endDate: Long?) {
        _filterStartDate.value = startDate
        _filterEndDate.value = endDate
        loadAllTransactions()
    }

    // Loads transactions based on current filter settings
    fun loadAllTransactions() {
        val username = sessionManager.getSessionUsername()
        if (username == null) {
            _allTransactionsState.value = UiState.Error("User session not found. Please log in again.")
            return
        }

        viewModelScope.launch {
            _allTransactionsState.value = UiState.Loading
            val startDate = _filterStartDate.value
            val endDate = _filterEndDate.value

            val transactionsFlow = if (startDate != null && endDate != null) {
                transactionRepository.getTransactionsByPeriod(username, startDate, endDate)
            } else {
                transactionRepository.getTransactionsByUser(username)
            }

            // Combine transactions with their categories and map to displayable format
            transactionsFlow.combine(categoryRepository.getCategoriesByUser(username)) { transactions, categories ->
                    transactions.mapNotNull { transaction ->
                        categories.find { it.id == transaction.categoryId }?.let { category ->
                            TransactionDisplayable(transaction, category.name, category.icon, category.color)
                        }
                    }.sortedByDescending { it.transaction.date }
                }
                .catch { exception ->
                    _allTransactionsState.value = UiState.Error(exception.message ?: "Failed to load transactions")
                }
                .collect { displayableTransactions ->
                    _allTransactionsState.value = UiState.Success(displayableTransactions)
                }
        }
    }

    // Deletes a transaction and reloads the list on success
    fun deleteTransaction(transaction: com.budgetease.data.model.Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
                .onSuccess {
                    loadAllTransactions()
                }
                .onFailure {
                    _allTransactionsState.value = UiState.Error(it.message ?: "Failed to delete transaction")
                }
        }
    }
} 