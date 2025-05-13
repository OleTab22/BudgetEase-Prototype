package com.budgetease.ui.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.budgetease.R
import com.budgetease.databinding.ItemAccountBinding

// Adapter for displaying a list of accounts in a RecyclerView
class AccountAdapter : ListAdapter<AccountDisplay, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Provides a reference to the views for each data item
    inner class AccountViewHolder(private val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        // Binds account data to the views in the item layout
        fun bind(account: AccountDisplay) {
            binding.accountName.text = account.name
            binding.accountBalance.text = account.balance
            binding.accountType.text = account.type
            // Set a default icon for the account
            binding.accountIcon.setImageResource(R.drawable.ic_account_balance_wallet)
        }
    }

    // Callback for calculating the diff between two non-null items in a list.
    // Used by ListAdapter to efficiently update the RecyclerView.
    class AccountDiffCallback : DiffUtil.ItemCallback<AccountDisplay>() {
        // Checks if two items represent the same object
        override fun areItemsTheSame(oldItem: AccountDisplay, newItem: AccountDisplay): Boolean {
            return oldItem.id == newItem.id
        }

        // Checks if the data of two items is the same
        override fun areContentsTheSame(oldItem: AccountDisplay, newItem: AccountDisplay): Boolean {
            return oldItem == newItem
        }
    }
} 