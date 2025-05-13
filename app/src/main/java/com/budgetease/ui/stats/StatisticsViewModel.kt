package com.budgetease.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.Category
import com.budgetease.data.repository.CategoryRepository
import com.budgetease.data.repository.TransactionRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.budgetease.data.model.Transaction
import java.util.Calendar

// Data models for statistics
data class CategorySpending(val categoryName: String, val totalAmount: Double, val categoryId: Long)
data class IncomeExpenseDetails(val totalIncome: Double, val totalExpenses: Double)
data class MonthlyNetSavings(val monthEpochStart: Long, val netSavings: Double)

// Manages financial statistics data and calculations
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // UI states for different chart data
    private val _categorySpendingState = MutableStateFlow<UiState<List<CategorySpending>>>(UiState.Idle)
    val categorySpendingState: StateFlow<UiState<List<CategorySpending>>> = _categorySpendingState

    private val _incomeExpenseDetailsState = MutableStateFlow<UiState<IncomeExpenseDetails>>(UiState.Idle)
    val incomeExpenseDetailsState: StateFlow<UiState<IncomeExpenseDetails>> = _incomeExpenseDetailsState

    private val _monthlyNetSavingsState = MutableStateFlow<UiState<List<MonthlyNetSavings>>>(UiState.Idle)
    val monthlyNetSavingsState: StateFlow<UiState<List<MonthlyNetSavings>>> = _monthlyNetSavingsState

    private val _currentStartDate = MutableStateFlow<Long>(0L)
    private val _currentEndDate = MutableStateFlow<Long>(0L)

    init {
        updateSelectedPeriodToCurrentMonth()
    }

    // Update date range for statistics
    fun updateSelectedPeriod(startDate: Long, endDate: Long) {
        _currentStartDate.value = startDate
        _currentEndDate.value = endDate
    }
    
    // Set date range to current month
    fun updateSelectedPeriodToCurrentMonth(){
        val calendar = java.util.Calendar.getInstance()
        
        // Set start date to first day of month
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        _currentStartDate.value = calendar.timeInMillis

        // Set end date to last day of month
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        _currentEndDate.value = calendar.timeInMillis
    }

    // Load all statistics data for the selected period
    fun loadCategorySpending(username: String) {
        viewModelScope.launch {
            _categorySpendingState.value = UiState.Loading
            _incomeExpenseDetailsState.value = UiState.Loading
            _monthlyNetSavingsState.value = UiState.Loading

            try {
                val categories = categoryRepository.getCategoriesByUser(username).first()
                
                if (categories.isEmpty()) {
                    _categorySpendingState.value = UiState.Success(emptyList())
                    _incomeExpenseDetailsState.value = UiState.Success(IncomeExpenseDetails(0.0, 0.0))
                    _monthlyNetSavingsState.value = UiState.Success(emptyList())
                    return@launch
                }

                // Calculate spending by category
                val spendingDataDeferred = categories.map { category ->
                    async {
                        val total = transactionRepository.getTotalExpensesByCategory(
                            category.id, username, _currentStartDate.value, _currentEndDate.value
                        ).first()
                        CategorySpending(category.name, total ?: 0.0, category.id)
                    }
                }
                
                val spendingData = spendingDataDeferred.awaitAll().filter { it.totalAmount > 0 }
                _categorySpendingState.value = UiState.Success(spendingData)

                // Calculate total income and expenses
                val incomeDeferred = async {
                    transactionRepository.getTransactionsByType("INCOME", username, _currentStartDate.value, _currentEndDate.value)
                        .map { transactions -> transactions.sumOf { it.amount } }
                        .first()
                }
                val expenseDeferred = async {
                    transactionRepository.getTransactionsByType("EXPENSE", username, _currentStartDate.value, _currentEndDate.value)
                        .map { transactions -> transactions.sumOf { it.amount } }
                        .first()
                }

                val totalIncome = incomeDeferred.await()
                val totalExpenses = expenseDeferred.await()
                _incomeExpenseDetailsState.value = UiState.Success(IncomeExpenseDetails(totalIncome, totalExpenses))

                // Calculate monthly savings trend
                val monthlySavingsData = calculateMonthlyNetSavings(username, _currentStartDate.value, _currentEndDate.value)
                if (monthlySavingsData.size < 2) {
                    _monthlyNetSavingsState.value = UiState.Error("Please select a longer period to view savings trend.")
                } else {
                    _monthlyNetSavingsState.value = UiState.Success(monthlySavingsData)
                }

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to load chart data"
                _categorySpendingState.value = UiState.Error(errorMessage)
                _incomeExpenseDetailsState.value = UiState.Error(errorMessage)
                _monthlyNetSavingsState.value = UiState.Error(errorMessage)
            }
        }
    }

    // Calculate net savings for each month in the selected period
    private suspend fun calculateMonthlyNetSavings(username: String, overallStartDate: Long, overallEndDate: Long): List<MonthlyNetSavings> {
        val savingsList = mutableListOf<MonthlyNetSavings>()
        val calendar = Calendar.getInstance()

        // Initialize start date to beginning of first month
        calendar.timeInMillis = overallStartDate
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Initialize end date to end of last month
        val endCalendar = Calendar.getInstance()
        endCalendar.timeInMillis = overallEndDate
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)

        // Process each month in the range
        while (calendar.timeInMillis <= endCalendar.timeInMillis) {
            val monthStartDate = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val monthEndDate = calendar.timeInMillis

            if (monthEndDate > endCalendar.timeInMillis) {
                // Process partial month if it starts within range
            }

            // Calculate net savings for the month
            val totalIncomeThisMonth = transactionRepository
                .getTransactionsByType("INCOME", username, monthStartDate, monthEndDate)
                .map { transactions -> transactions.sumOf { it.amount } }
                .first()

            val totalExpensesThisMonth = transactionRepository
                .getTransactionsByType("EXPENSE", username, monthStartDate, monthEndDate)
                .map { transactions -> transactions.sumOf { it.amount } }
                .first()
            
            savingsList.add(MonthlyNetSavings(monthStartDate, totalIncomeThisMonth - totalExpensesThisMonth))

            // Move to next month
            calendar.timeInMillis = monthEndDate
            calendar.add(Calendar.MILLISECOND, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            if (calendar.timeInMillis > endCalendar.timeInMillis && savingsList.isNotEmpty()) break
        }
        return savingsList
    }

    fun getCurrentStartDate(): Long = _currentStartDate.value
    fun getCurrentEndDate(): Long = _currentEndDate.value
} 