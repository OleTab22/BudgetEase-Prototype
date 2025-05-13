package com.budgetease.ui.wallet

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
import javax.inject.Inject

// Data class for displaying account information in the UI
data class AccountDisplay(val id: String, val name: String, val balance: String, val type: String)

// ViewModel for the Wallet screen, managing accounts and recent transactions data
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // UI state for the list of recent transactions
    private val _transactionsState = MutableStateFlow<UiState<List<TransactionDisplayable>>>(UiState.Idle)
    val transactionsState: StateFlow<UiState<List<TransactionDisplayable>>> = _transactionsState

    // UI state for the list of accounts
    private val _accountsState = MutableStateFlow<UiState<List<AccountDisplay>>>(UiState.Idle)
    val accountsState: StateFlow<UiState<List<AccountDisplay>>> = _accountsState

    // Initiates loading of all data required for the wallet screen
    fun loadWalletData() {
        val username = sessionManager.getSessionUsername()
        if (username != null) {
            loadTransactions(username)
            loadPlaceholderAccounts() // Loads placeholder accounts for now
        } else {
            // Emit error state if user session is not found
            _transactionsState.value = UiState.Error("User session not found.")
            _accountsState.value = UiState.Error("User session not found.")
        }
    }

    // Loads the 5 most recent transactions for the given user
    private fun loadTransactions(username: String) {
        viewModelScope.launch {
            _transactionsState.value = UiState.Loading
            transactionRepository.getTransactionsByUser(username)
                .combine(categoryRepository.getCategoriesByUser(username)) { transactions, categories ->
                    // Map transactions to displayable format with category details
                    transactions.mapNotNull { transaction ->
                        categories.find { it.id == transaction.categoryId }?.let { category ->
                            TransactionDisplayable(transaction, category.name, category.icon, category.color)
                        }
                    }.sortedByDescending { it.transaction.date } // Sort by date to get latest first
                     .take(5) // Limit to the 5 most recent transactions
                }
                .catch { exception ->
                    _transactionsState.value = UiState.Error(exception.message ?: "Failed to load transactions")
                }
                .collect { displayableTransactions ->
                    _transactionsState.value = UiState.Success(displayableTransactions)
                }
        }
    }

    // Loads placeholder account data for UI display
    private fun loadPlaceholderAccounts() {
        _accountsState.value = UiState.Loading
        // Currently uses a static list of accounts for demonstration
        val placeholderAccounts = listOf(
            AccountDisplay("1", "Main Bank Account", "R 12,345.67", "Savings"),
            AccountDisplay("2", "Credit Card", "- R 1,234.56", "Credit"),
            AccountDisplay("3", "Cash Wallet", "R 500.00", "Cash")
        )
        _accountsState.value = UiState.Success(placeholderAccounts)
    }

    // Deletes a transaction and reloads the recent transactions list
    fun deleteTransaction(transaction: com.budgetease.data.model.Transaction) {
        val username = sessionManager.getSessionUsername() ?: return // Guard clause if username is null
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
                .onSuccess {
                    loadTransactions(username) // Reload recent transactions after deletion
                }
                .onFailure {
                    _transactionsState.value = UiState.Error(it.message ?: "Failed to delete transaction")
                }
        }
    }
} 