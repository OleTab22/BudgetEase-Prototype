package com.budgetease.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetease.data.model.BudgetGoal
import com.budgetease.data.model.BudgetGoalDisplayable
import com.budgetease.data.model.Category
import com.budgetease.data.repository.BudgetGoalRepository
import com.budgetease.data.repository.CategoryRepository
import com.budgetease.data.repository.TransactionRepository
import com.budgetease.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// Manages budget goals data and operations for the UI
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetGoalRepository: BudgetGoalRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // State flows for UI updates
    private val _budgetGoalsState = MutableStateFlow<UiState<List<BudgetGoalDisplayable>>>(UiState.Loading)
    val budgetGoalsState: StateFlow<UiState<List<BudgetGoalDisplayable>>> = _budgetGoalsState

    private val _categoriesState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categoriesState: StateFlow<UiState<List<Category>>> = _categoriesState

    private val _setBudgetGoalState = MutableStateFlow<UiState<Long>>(UiState.Idle)
    val setBudgetGoalState: StateFlow<UiState<Long>> = _setBudgetGoalState

    // Loads budget goals with spending data for a specific period
    fun loadBudgetGoals(username: String, month: Int, year: Int) {
        viewModelScope.launch {
            _budgetGoalsState.value = UiState.Loading
            try {
                budgetGoalRepository.getBudgetGoalsByPeriod(username, month, year)
                    .combine(categoryRepository.getCategoriesByUser(username)) { goals, categories ->
                        // Calculate period boundaries
                        val calendar = Calendar.getInstance().apply {
                            clear()
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month - 1)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val startDate = calendar.timeInMillis
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                        val endDate = calendar.timeInMillis

                        // Fetch spending data for all goals concurrently
                        val spendingDeferreds = goals.map { goal ->
                            async {
                                val spending = transactionRepository.getTotalExpensesByCategory(
                                    goal.categoryId, username, startDate, endDate
                                ).first()
                                goal.id to (spending ?: 0.0)
                            }
                        }
                        val spendingMap = spendingDeferreds.awaitAll().toMap()

                        // Combine goals with category info and spending data
                        goals.mapNotNull { goal ->
                            categories.find { it.id == goal.categoryId }?.let { category ->
                                BudgetGoalDisplayable(
                                    budgetGoal = goal,
                                    categoryName = category.name,
                                    categoryIcon = category.icon,
                                    categoryColor = category.color,
                                    actualSpending = spendingMap[goal.id] ?: 0.0
                                )
                            }
                        }
                    }
                    .catch { exception ->
                        _budgetGoalsState.value = UiState.Error(exception.message ?: "Failed to load budget goals or spending")
                    }
                    .collect { displayableGoals ->
                        _budgetGoalsState.value = UiState.Success(displayableGoals)
                    }
            } catch (e: Exception) {
                _budgetGoalsState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Loads available categories for budget goal creation
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

    // Creates or updates a budget goal
    fun setBudgetGoal(
        existingGoalId: Long? = null,
        categoryId: Long,
        minAmount: Double,
        maxAmount: Double,
        month: Int,
        year: Int,
        username: String
    ) {
        viewModelScope.launch {
            _setBudgetGoalState.value = UiState.Loading
            val budgetGoal = BudgetGoal(
                id = existingGoalId ?: 0,
                categoryId = categoryId,
                minAmount = minAmount,
                maxAmount = maxAmount,
                month = month,
                year = year,
                createdBy = username
            )

            val result = if (existingGoalId != null) {
                budgetGoalRepository.updateBudgetGoal(budgetGoal).map { budgetGoal.id }
            } else {
                budgetGoalRepository.setBudgetGoal(
                    categoryId, minAmount, maxAmount, month, year, username
                )
            }

            result.onSuccess {
                _setBudgetGoalState.value = UiState.Success(it)
            }.onFailure {
                _setBudgetGoalState.value = UiState.Error(it.message ?: "Failed to set budget goal")
            }
        }
    }
    
    // Deletes a budget goal
    fun deleteBudgetGoal(budgetGoal: BudgetGoal) {
        viewModelScope.launch {
            budgetGoalRepository.deleteBudgetGoal(budgetGoal)
                .onSuccess {
                }
                .onFailure { exception ->
                     _budgetGoalsState.value = UiState.Error(exception.message ?: "Failed to delete budget goal")
                }
        }
    }

    // Resets the budget goal operation state
    fun resetSetBudgetGoalState() {
        _setBudgetGoalState.value = UiState.Idle
    }
} 