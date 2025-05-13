package com.budgetease.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.FragmentWalletBinding
import com.budgetease.ui.auth.LoginActivity
import com.budgetease.ui.common.UiState
import com.budgetease.ui.dashboard.TransactionAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.fragment.findNavController
import com.budgetease.R
import androidx.appcompat.app.AlertDialog
import android.widget.ImageView
import android.widget.TextView
import android.net.Uri
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import com.budgetease.data.model.TransactionDisplayable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fragment to display user's wallet information including accounts and recent transactions
@AndroidEntryPoint
class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var accountAdapter: AccountAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    // Date formatter for transaction details dialog
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        viewModel.loadWalletData() // Initial data load
    }

    // Sets up RecyclerViews for accounts and transactions
    private fun setupRecyclerViews() {
        // Accounts RecyclerView
        accountAdapter = AccountAdapter()
        binding.accountsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = accountAdapter
        }

        // Transactions RecyclerView
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetailsDialog(transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    // Observes LiveData from the ViewModel to update UI states
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        accountAdapter.submitList(state.data)
                        binding.accountsProgressBar.visibility = View.GONE
                        binding.emptyAccountsContainer.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        binding.accountsProgressBar.visibility = View.GONE
                        binding.emptyAccountsContainer.visibility = View.VISIBLE
                    }
                    is UiState.Loading -> {
                        binding.accountsProgressBar.visibility = View.VISIBLE
                        binding.emptyAccountsContainer.visibility = View.GONE
                    }
                    is UiState.Idle -> {
                        binding.accountsProgressBar.visibility = View.GONE
                        binding.emptyAccountsContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactionsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        transactionAdapter.submitList(state.data)
                        binding.transactionsProgressBar.visibility = View.GONE
                        binding.emptyTransactionsContainerWallet.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        binding.transactionsProgressBar.visibility = View.GONE
                        binding.emptyTransactionsContainerWallet.visibility = View.VISIBLE
                    }
                    is UiState.Loading -> {
                        binding.transactionsProgressBar.visibility = View.VISIBLE
                        binding.emptyTransactionsContainerWallet.visibility = View.GONE
                    }
                    is UiState.Idle -> {
                        binding.transactionsProgressBar.visibility = View.GONE
                        binding.emptyTransactionsContainerWallet.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // Sets up click listeners for various UI elements
    private fun setupClickListeners() {
        binding.addAccountButton.setOnClickListener {
            // Placeholder for future add account functionality
            Snackbar.make(binding.root, "Add Account feature coming soon!", Snackbar.LENGTH_SHORT).show()
        }

        binding.seeAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_wallet_to_allTransactionsFragment)
        }
    }

    // Displays a dialog with detailed information about a transaction
    private fun showTransactionDetailsDialog(item: TransactionDisplayable) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_details, null)
        val categoryTextView = dialogView.findViewById<TextView>(R.id.detailCategory)
        val amountTextView = dialogView.findViewById<TextView>(R.id.detailAmount)
        val dateTextView = dialogView.findViewById<TextView>(R.id.detailDate)
        val notesTextView = dialogView.findViewById<TextView>(R.id.detailNotes)
        val notesLabelTextView = dialogView.findViewById<TextView>(R.id.detailNotesLabel)
        val receiptImageView = dialogView.findViewById<ImageView>(R.id.detailReceiptImage)

        val transaction = item.transaction

        categoryTextView.text = item.categoryName
        dateTextView.text = fullDateFormat.format(Date(transaction.date))

        val formattedAmount = String.format(Locale.getDefault(), "R %.2f", transaction.amount)
        if (transaction.type == "EXPENSE") {
            amountTextView.text = "- $formattedAmount"
            amountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_primary))
        } else {
            amountTextView.text = "+ $formattedAmount"
            amountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_primary))
        }

        if (transaction.description.isNotEmpty()) {
            notesTextView.text = transaction.description
            notesTextView.visibility = View.VISIBLE
            notesLabelTextView.visibility = View.VISIBLE
        } else {
            notesTextView.visibility = View.GONE
            notesLabelTextView.visibility = View.GONE
        }

        if (!transaction.imagePath.isNullOrEmpty()) {
            receiptImageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(Uri.parse(transaction.imagePath))
                .placeholder(R.drawable.ic_receipt_placeholder)
                .error(R.drawable.ic_error_placeholder)
                .into(receiptImageView)
        } else {
            receiptImageView.visibility = View.GONE
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Transaction Details")
            .setPositiveButton(getString(R.string.close_button)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Shows a confirmation dialog before deleting a transaction
    private fun showDeleteConfirmationDialog(item: TransactionDisplayable) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_transaction))
            .setMessage(getString(R.string.delete_transaction_confirmation, item.transaction.description, String.format(Locale.getDefault(), "R %.2f", item.transaction.amount)))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTransaction(item.transaction)
                Snackbar.make(binding.root, "Transaction deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }
} 