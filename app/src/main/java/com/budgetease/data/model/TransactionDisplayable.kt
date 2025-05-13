package com.budgetease.data.model

// Represents a Transaction with its associated Category details for display purposes.
data class TransactionDisplayable(
    val transaction: Transaction,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Int
) 