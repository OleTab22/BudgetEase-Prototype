package com.budgetease.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetease.databinding.FragmentAllTransactionsBinding
import com.budgetease.ui.common.UiState
import com.budgetease.ui.dashboard.TransactionAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.util.Pair
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import android.widget.ImageView
import android.widget.TextView
import android.net.Uri
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import com.budgetease.R
import com.budgetease.data.model.TransactionDisplayable

// Fragment for displaying and managing all transactions with filtering capabilities
@AndroidEntryPoint
class AllTransactionsFragment : Fragment() {

    private var _binding: FragmentAllTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllTransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    // Date formatters for UI display
    private val selectedDatesFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        viewModel.loadAllTransactions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetailsDialog(transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        binding.allTransactionsRecyclerView.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.filterTransactionsButton.setOnClickListener {
            showDateRangePicker()
        }
    }

    // Shows date range picker for transaction filtering
    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Transaction Period")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            val startDate = selection.first
            val endDate = selection.second
            viewModel.setTransactionPeriod(startDate, endDate)

            val startDateFormatted = selectedDatesFormat.format(Date(startDate))
            val endDateFormatted = selectedDatesFormat.format(Date(endDate))
            binding.selectedPeriodText.text = "Period: $startDateFormatted - $endDateFormatted"
            binding.selectedPeriodText.visibility = View.VISIBLE
        }

        datePicker.show(parentFragmentManager, "ALL_TRANSACTIONS_DATE_PICKER")
    }

    // Displays detailed transaction information in a dialog
    private fun showTransactionDetailsDialog(item: TransactionDisplayable) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_details, null)
        val categoryTextView = dialogView.findViewById<TextView>(R.id.detailCategory)
        val amountTextView = dialogView.findViewById<TextView>(R.id.detailAmount)
        val dateTextView = dialogView.findViewById<TextView>(R.id.detailDate)
        val notesTextView = dialogView.findViewById<TextView>(R.id.detailNotes)
        val notesLabelTextView = dialogView.findViewById<TextView>(R.id.detailNotesLabel)
        val receiptImageView = dialogView.findViewById<ImageView>(R.id.detailReceiptImage)

        val transaction = item.transaction

        // Set basic transaction details
        categoryTextView.text = item.categoryName
        dateTextView.text = fullDateFormat.format(Date(transaction.date))

        // Format and style amount based on transaction type
        val formattedAmount = String.format(Locale.getDefault(), "R %.2f", transaction.amount)
        if (transaction.type == "EXPENSE") {
            amountTextView.text = "- $formattedAmount"
            amountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_primary))
        } else {
            amountTextView.text = "+ $formattedAmount"
            amountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_primary))
        }

        // Handle optional transaction notes
        if (transaction.description.isNotEmpty()) {
            notesTextView.text = transaction.description
            notesTextView.visibility = View.VISIBLE
            notesLabelTextView.visibility = View.VISIBLE
        } else {
            notesTextView.visibility = View.GONE
            notesLabelTextView.visibility = View.GONE
        }

        // Load receipt image if available
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

    // Handles UI state changes based on transaction loading status
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allTransactionsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyViewContainer.visibility = View.GONE
                        binding.allTransactionsRecyclerView.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        if (state.data.isEmpty()) {
                            binding.emptyViewContainer.visibility = View.VISIBLE
                            binding.allTransactionsRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyViewContainer.visibility = View.GONE
                            binding.allTransactionsRecyclerView.visibility = View.VISIBLE
                            transactionAdapter.submitList(state.data)
                        }
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyViewContainer.visibility = View.VISIBLE
                        binding.emptyViewText.text = state.message
                        binding.allTransactionsRecyclerView.visibility = View.GONE
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    is UiState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyViewContainer.visibility = View.VISIBLE
                        binding.allTransactionsRecyclerView.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 