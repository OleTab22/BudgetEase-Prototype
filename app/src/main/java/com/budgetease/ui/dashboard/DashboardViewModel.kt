package com.budgetease.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.repository.TransactionRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import com.budgetease.data.model.Transaction
import com.budgetease.data.model.TransactionDisplayable
import com.budgetease.data.repository.CategoryRepository
import com.budgetease.data.repository.UserRepository
import com.budgetease.data.preferences.SessionManager
import com.budgetease.data.repository.BudgetGoalRepository

// Manages dashboard data and UI state
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val budgetGoalRepository: BudgetGoalRepository
) : ViewModel() {

    // UI state holders for dashboard data
    private val _displayNameState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val displayNameState: StateFlow<UiState<String>> = _displayNameState

    private val _balanceState = MutableStateFlow<UiState<Double>>(UiState.Loading)
    val balanceState: StateFlow<UiState<Double>> = _balanceState

    private val _incomeState = MutableStateFlow<UiState<Double>>(UiState.Loading)
    val incomeState: StateFlow<UiState<Double>> = _incomeState

    private val _expenseState = MutableStateFlow<UiState<Double>>(UiState.Loading)
    val expenseState: StateFlow<UiState<Double>> = _expenseState

    private val _remainingBudgetState = MutableStateFlow<UiState<Double>>(UiState.Loading)
    val remainingBudgetState: StateFlow<UiState<Double>> = _remainingBudgetState

    private val _transactionsState = MutableStateFlow<UiState<List<TransactionDisplayable>>>(UiState.Idle)
    val transactionsState: StateFlow<UiState<List<TransactionDisplayable>>> = _transactionsState

    // Transaction filter date range
    private val _transactionFilterStartDate = MutableStateFlow<Long?>(null)
    private val _transactionFilterEndDate = MutableStateFlow<Long?>(null)

    // Load all dashboard data for the current user
    fun loadDashboardData() {
        val username = sessionManager.getSessionUsername()
        if (username == null) {
            _displayNameState.value = UiState.Error("User session not found")
            _balanceState.value = UiState.Error("User session not found")
            _incomeState.value = UiState.Error("User session not found")
            _expenseState.value = UiState.Error("User session not found")
            _remainingBudgetState.value = UiState.Error("User session not found")
            _transactionsState.value = UiState.Error("User session not found")
            return
        }

        loadDisplayName(username)
        loadBalance(username)
        loadIncome(username)
        loadExpenses(username)
        loadRemainingBudget(username)

        // Set default transaction period to last 30 days
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val startDate = calendar.timeInMillis
        setTransactionPeriod(startDate, endDate)
        loadTransactions(username)
    }

    // Load user's display name
    private fun loadDisplayName(username: String) {
        viewModelScope.launch {
            _displayNameState.value = UiState.Loading
            userRepository.getUserByUsername(username)
                .onSuccess {
                    _displayNameState.value = UiState.Success(it.fullName.ifEmpty { username })
                }
                .onFailure {
                    _displayNameState.value = UiState.Success(username)
                }
        }
    }

    // Calculate current month's balance
    private fun loadBalance(username: String) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            // Set start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.timeInMillis

            // Set end of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.timeInMillis

            // Calculate income minus expenses
            transactionRepository.getTransactionsByType("INCOME", username, startDate, endDate)
                .combine(
                    transactionRepository.getTransactionsByType("EXPENSE", username, startDate, endDate)
                ) { incomeList, expenseList ->
                    val totalIncome = incomeList.sumOf { it.amount }
                    val totalExpense = expenseList.sumOf { it.amount }
                    totalIncome - totalExpense
                }
                .catch { exception ->
                    _balanceState.value = UiState.Error(exception.message ?: "Failed to load balance")
                }
                .collect { balance ->
                    _balanceState.value = UiState.Success(balance)
                }
        }
    }

    // Load current month's income
    private fun loadIncome(username: String) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            // Set start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.timeInMillis

            // Set end of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.timeInMillis

            transactionRepository.getTransactionsByType("INCOME", username, startDate, endDate)
                .map { transactions -> transactions.sumOf { it.amount } }
                .catch { exception ->
                    _incomeState.value = UiState.Error(exception.message ?: "Failed to load income")
                }
                .collect { totalIncome ->
                    _incomeState.value = UiState.Success(totalIncome)
                }
        }
    }

    // Load current month's expenses
    private fun loadExpenses(username: String) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            // Set start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.timeInMillis

            // Set end of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.timeInMillis

            transactionRepository.getTransactionsByType("EXPENSE", username, startDate, endDate)
                .map { transactions -> transactions.sumOf { it.amount } }
                .catch { exception ->
                    _expenseState.value = UiState.Error(exception.message ?: "Failed to load expenses")
                }
                .collect { totalExpenses ->
                    _expenseState.value = UiState.Success(totalExpenses)
                }
        }
    }

    // Calculate remaining budget for current month
    private fun loadRemainingBudget(username: String) {
        viewModelScope.launch {
            _remainingBudgetState.value = UiState.Loading
            try {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                // Set start of month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                // Set end of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis

                // Get budget goals and expenses
                val goalsFlow = budgetGoalRepository.getBudgetGoalsByPeriod(username, currentMonth, currentYear)
                val expensesFlow = transactionRepository.getTransactionsByType("EXPENSE", username, startDate, endDate)
                    .map { transactions -> transactions.sumOf { it.amount } }

                // Calculate remaining budget
                goalsFlow.combine(expensesFlow) { goals, totalExpenses ->
                    val totalBudget = goals.sumOf { it.maxAmount }
                    totalBudget - totalExpenses
                }.collect { remaining ->
                    _remainingBudgetState.value = UiState.Success(remaining)
                }
            } catch (e: Exception) {
                _remainingBudgetState.value = UiState.Error(e.message ?: "Failed to load remaining budget")
            }
        }
    }

    // Update transaction filter period
    fun setTransactionPeriod(startDate: Long?, endDate: Long?) {
        _transactionFilterStartDate.value = startDate
        _transactionFilterEndDate.value = endDate
    }

    // Load transactions for selected period
    fun loadTransactions(username: String) {
        viewModelScope.launch {
            _transactionsState.value = UiState.Loading
            val startDate = _transactionFilterStartDate.value
            val endDate = _transactionFilterEndDate.value

            val transactionsFlow = if (startDate != null && endDate != null) {
                transactionRepository.getTransactionsByPeriod(username, startDate, endDate)
            } else {
                transactionRepository.getTransactionsByUser(username)
            }

            // Combine transactions with category data
            transactionsFlow.combine(categoryRepository.getCategoriesByUser(username)) { transactions, categories ->
                transactions.mapNotNull { transaction ->
                    categories.find { it.id == transaction.categoryId }?.let {
                        TransactionDisplayable(transaction, it.name, it.icon, it.color)
                    }
                }
            }
            .catch { exception ->
                _transactionsState.value = UiState.Error(exception.message ?: "Failed to load transactions")
            }
            .collect { displayableTransactions ->
                _transactionsState.value = UiState.Success(displayableTransactions)
            }
        }
    }

    // Format amount to local currency
    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(amount)
            .replace("ZAR", "R") 
    }

    // Delete transaction and update state
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
                .onSuccess {
                    Log.d("ViewModelDelete", "Transaction deleted successfully: ${transaction.id}")
                }
                .onFailure {
                    Log.e("ViewModelDelete", "Failed to delete transaction: ${transaction.id}", it)
                }
        }
    }
} 