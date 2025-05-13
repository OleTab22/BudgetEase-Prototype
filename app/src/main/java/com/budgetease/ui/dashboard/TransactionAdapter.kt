package com.budgetease.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.budgetease.R
import com.budgetease.data.model.TransactionDisplayable
import com.budgetease.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

// Adapter for displaying transactions in a RecyclerView with DiffUtil support
class TransactionAdapter(
    private val onItemClick: (TransactionDisplayable) -> Unit,
    private val onDeleteClick: (TransactionDisplayable) -> Unit
) : ListAdapter<TransactionDisplayable, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transactionDisplayable = getItem(position)
        holder.bind(transactionDisplayable)
    }

    // ViewHolder for binding and displaying transaction data
    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionDisplayable) {
            val transaction = item.transaction
            binding.transactionDescription.text = transaction.description
            binding.transactionCategory.text = item.categoryName

            // Format amount and set color based on transaction type
            val formattedAmount = String.format(itemView.context.getString(R.string.amount_format), "%.2f".format(transaction.amount))
            if (transaction.type == "EXPENSE") {
                binding.transactionAmount.text = "- ${formattedAmount}"
                binding.transactionAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.expense_primary))
            } else {
                binding.transactionAmount.text = "+ ${formattedAmount}"
                binding.transactionAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.income_primary))
            }

            // Set category icon with fallback
            val iconResId = try {
                itemView.context.resources.getIdentifier(item.categoryIcon, "drawable", itemView.context.packageName)
            } catch (e: Resources.NotFoundException) {
                itemView.context.resources.getIdentifier("ic_default_category", "drawable", itemView.context.packageName)
            }
            if (iconResId != 0) {
                binding.transactionIcon.setImageResource(iconResId)
            } else {
                binding.transactionIcon.setImageResource(R.drawable.ic_default_category)
            }

            // Apply category color to icon
            try {
                binding.transactionIcon.colorFilter = PorterDuffColorFilter(item.categoryColor, PorterDuff.Mode.SRC_IN)
            } catch (e: Exception) {
                binding.transactionIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.primary))
            }

            // Set formatted date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.transactionDate.text = dateFormat.format(Date(transaction.date))
            
            // Show receipt indicator if image exists
            binding.receiptIndicator.visibility = if (!transaction.imagePath.isNullOrEmpty()) View.VISIBLE else View.GONE

            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(item)
            }

            binding.deleteTransactionButton.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
}

// Handles efficient updates by comparing old and new items
class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionDisplayable>() {
    override fun areItemsTheSame(oldItem: TransactionDisplayable, newItem: TransactionDisplayable): Boolean {
        return oldItem.transaction.id == newItem.transaction.id
    }

    override fun areContentsTheSame(oldItem: TransactionDisplayable, newItem: TransactionDisplayable): Boolean {
        return oldItem == newItem
    }
} 