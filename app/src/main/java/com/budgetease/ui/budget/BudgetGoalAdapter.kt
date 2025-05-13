package com.budgetease.ui.budget

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.budgetease.R
import com.budgetease.data.model.BudgetGoalDisplayable
import com.budgetease.databinding.ItemBudgetGoalBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Displays budget goals with spending progress and category information
class BudgetGoalAdapter(
    private val context: Context,
    private val onEditClick: (BudgetGoalDisplayable) -> Unit,
    private val onDeleteClick: (BudgetGoalDisplayable) -> Unit
) : ListAdapter<BudgetGoalDisplayable, BudgetGoalAdapter.BudgetGoalViewHolder>(BudgetGoalDiffCallback()) {

    // Formatters for currency and date display
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetGoalViewHolder {
        val binding = ItemBudgetGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetGoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetGoalViewHolder, position: Int) {
        val budgetGoalDisplayable = getItem(position)
        holder.bind(budgetGoalDisplayable)
    }

    // ViewHolder for displaying individual budget goal items
    inner class BudgetGoalViewHolder(private val binding: ItemBudgetGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BudgetGoalDisplayable) {
            binding.categoryNameText.text = item.categoryName

            // Format and display budget range
            val minFormatted = currencyFormat.format(item.budgetGoal.minAmount).replace("ZAR", "R")
            val maxFormatted = currencyFormat.format(item.budgetGoal.maxAmount).replace("ZAR", "R")
            binding.budgetAmountText.text = String.format(Locale.getDefault(),
                "Budget: %s - %s",
                minFormatted,
                maxFormatted
            )

            // Calculate and display spending progress
            val maxAmount = item.budgetGoal.maxAmount
            val actualSpending = item.actualSpending
            val progressPercent = if (maxAmount > 0) (actualSpending / maxAmount * 100).toInt() else 0

            binding.spendingProgressBar.progress = progressPercent.coerceAtMost(100)
            binding.spendingProgressText.text = String.format(Locale.getDefault(),
                "Spent: %s of %s",
                currencyFormat.format(actualSpending).replace("ZAR", "R"),
                currencyFormat.format(maxAmount).replace("ZAR", "R")
            )

            // Update progress bar color based on budget status
            val progressDrawable = if (actualSpending > maxAmount) {
                ContextCompat.getDrawable(context, R.drawable.progress_bar_over_budget)?.mutate()
            } else {
                ContextCompat.getDrawable(context, R.drawable.progress_bar_budget)?.mutate()
            }
            binding.spendingProgressBar.progressDrawable = progressDrawable

            // Set budget period display
            val calendar = Calendar.getInstance().apply {
                clear()
                set(Calendar.MONTH, item.budgetGoal.month -1)
                set(Calendar.YEAR, item.budgetGoal.year)
            }
            binding.goalPeriodText.text = monthYearFormat.format(calendar.time)

            // Load and set category icon
            val iconResId = try {
                context.resources.getIdentifier(item.categoryIcon, "drawable", context.packageName)
            } catch (e: Resources.NotFoundException) {
                context.resources.getIdentifier("ic_default_category", "drawable", context.packageName)
            }
            if (iconResId != 0) {
                binding.categoryIcon.setImageResource(iconResId)
            } else {
                binding.categoryIcon.setImageResource(R.drawable.ic_default_category)
            }

            // Apply category color to icon
            try {
                binding.categoryIcon.colorFilter = PorterDuffColorFilter(item.categoryColor, PorterDuff.Mode.SRC_IN)
            } catch (e: Exception) {
                binding.categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            }

            // Set up action buttons
            binding.editGoalButton.setOnClickListener { onEditClick(item) }
            binding.deleteGoalButton.setOnClickListener { onDeleteClick(item) }
        }
    }
}

// Handles budget goal item comparisons for efficient RecyclerView updates
class BudgetGoalDiffCallback : DiffUtil.ItemCallback<BudgetGoalDisplayable>() {
    override fun areItemsTheSame(oldItem: BudgetGoalDisplayable, newItem: BudgetGoalDisplayable): Boolean {
        return oldItem.budgetGoal.id == newItem.budgetGoal.id
    }

    override fun areContentsTheSame(oldItem: BudgetGoalDisplayable, newItem: BudgetGoalDisplayable): Boolean {
        return oldItem == newItem
    }
} 