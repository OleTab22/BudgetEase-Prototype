package com.budgetease.data.model

// Represents a BudgetGoal with its associated Category name for display purposes
data class BudgetGoalDisplayable(
    // The underlying budget goal data
    val budgetGoal: BudgetGoal,
    
    // Name of the associated category
    val categoryName: String,
    
    // Icon identifier for the category
    val categoryIcon: String,
    
    // Color code for the category
    val categoryColor: Int,
    
    // Current amount spent against this budget
    val actualSpending: Double = 0.0
) 