package com.budgetease.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.budgetease.R
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.FragmentDashboardBinding
import com.budgetease.ui.auth.LoginActivity
import com.budgetease.ui.common.UiState
import com.budgetease.data.model.Transaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.core.util.Pair
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.fragment.findNavController
import java.util.Calendar
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.widget.ImageView
import android.net.Uri
import com.bumptech.glide.Glide
import android.widget.TextView
import com.budgetease.data.model.TransactionDisplayable

// Main dashboard screen showing user's financial overview and recent transactions
@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager
    private lateinit var transactionAdapter: TransactionAdapter

    // Date formatters for transaction display
    private val selectedDatesFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()

        // Setup budget shortcut navigation
        binding.dashboardShortcutBudgetGoalButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_budget)
        }

        // Load dashboard data if user is logged in
        val username = sessionManager.getSessionUsername()
        if (sessionManager.isUserLoggedIn() && username != null) {
            viewModel.loadDashboardData()
        } else {
            navigateToLogin()
        }
    }

    // Initialize transaction list
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetailsDialog(transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        binding.transactionsRecycler.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    // Redirect to login screen on session expiry
    private fun navigateToLogin() {
        showError("Session invalid. Please login again.")
        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    // Setup state observers for UI updates
    private fun observeViewModel() {
        // Display name and greeting observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayNameState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.nameText.text = state.data
                        binding.greetingText.text = getGreeting()
                    }
                    is UiState.Error -> {
                        binding.nameText.text = "User" 
                        binding.greetingText.text = getGreeting()
                    }
                    is UiState.Loading -> {
                        binding.nameText.text = "Loading..."
                        binding.greetingText.text = "Hello,"
                    }
                    is UiState.Idle -> {
                        binding.nameText.text = "User"
                        binding.greetingText.text = getGreeting()
                    }
                }
            }
        }

        // Balance observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.balanceState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.balanceAmount.text = viewModel.formatCurrency(state.data)
                    }
                    is UiState.Error -> {
                        showError(state.message)
                        binding.balanceAmount.text = viewModel.formatCurrency(0.0)
                    }
                    is UiState.Loading -> {
                        binding.balanceAmount.text = "Loading..."
                    }
                    is UiState.Idle -> {
                        binding.balanceAmount.text = viewModel.formatCurrency(0.0)
                    }
                }
            }
        }

        // Income observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.incomeState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.incomeAmount.text = viewModel.formatCurrency(state.data)
                    }
                    is UiState.Error -> {
                        showError(state.message)
                        binding.incomeAmount.text = viewModel.formatCurrency(0.0)
                    }
                    is UiState.Loading -> {
                        binding.incomeAmount.text = "Loading..."
                    }
                    is UiState.Idle -> {
                        binding.incomeAmount.text = viewModel.formatCurrency(0.0)
                    }
                }
            }
        }

        // Expense observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenseState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.expensesAmount.text = viewModel.formatCurrency(state.data)
                    }
                    is UiState.Error -> {
                        showError(state.message)
                        binding.expensesAmount.text = viewModel.formatCurrency(0.0)
                    }
                    is UiState.Loading -> {
                        binding.expensesAmount.text = "Loading..."
                    }
                    is UiState.Idle -> {
                        binding.expensesAmount.text = viewModel.formatCurrency(0.0)
                    }
                }
            }
        }

        // Transactions list observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactionsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.transactionsRecycler.visibility = View.VISIBLE
                        binding.emptyTransactionsContainer.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                        binding.transactionsProgressBar.visibility = View.GONE
                        transactionAdapter.submitList(state.data)
                    }
                    is UiState.Error -> {
                        binding.transactionsRecycler.visibility = View.GONE
                        binding.emptyTransactionsContainer.visibility = View.VISIBLE
                        binding.emptyTransactionsView.text = state.message
                        binding.transactionsProgressBar.visibility = View.GONE
                        showError(state.message)
                    }
                    is UiState.Loading -> {
                        binding.transactionsRecycler.visibility = View.GONE
                        binding.emptyTransactionsContainer.visibility = View.GONE
                        binding.transactionsProgressBar.visibility = View.VISIBLE
                    }
                    is UiState.Idle -> {
                        binding.transactionsRecycler.visibility = View.VISIBLE
                        binding.emptyTransactionsContainer.visibility = View.GONE
                        binding.transactionsProgressBar.visibility = View.GONE
                    }
                }
            }
        }

        // Remaining budget observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.remainingBudgetState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.remainingBudgetAmount.text = viewModel.formatCurrency(state.data)
                        val colorRes = if (state.data >= 0) R.color.income_primary else R.color.expense_primary
                        binding.remainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
                    }
                    is UiState.Error -> {
                        showError(state.message)
                        binding.remainingBudgetAmount.text = "--"
                    }
                    is UiState.Loading -> {
                        binding.remainingBudgetAmount.text = "Loading..."
                    }
                    is UiState.Idle -> {
                        binding.remainingBudgetAmount.text = "--"
                    }
                }
            }
        }
    }

    // Setup button click handlers
    private fun setupClickListeners() {
        binding.seeAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_allTransactionsFragment)
        }

        binding.filterTransactionsButton.setOnClickListener {
            showDateRangePicker()
        }

        binding.addTransactionButtonDashboard.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboard_to_addTransaction)
            } catch (e: IllegalArgumentException) {
                Snackbar.make(binding.root, "Could not navigate to Add Transaction.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // Show date picker for transaction filtering
    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Transaction Period")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            val startDate = selection.first
            val endDate = selection.second
            viewModel.setTransactionPeriod(startDate, endDate)
            val username = sessionManager.getSessionUsername()
            if (username != null) {
                viewModel.loadTransactions(username)
                val startDateFormatted = selectedDatesFormat.format(Date(startDate))
                val endDateFormatted = selectedDatesFormat.format(Date(endDate))
                binding.selectedPeriodText.text = "Period: $startDateFormatted - $endDateFormatted"
                binding.selectedPeriodText.visibility = View.VISIBLE
            } else {
                navigateToLogin()
            }
        }
        datePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    // Get time-based greeting message
    private fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> getString(R.string.good_morning)
            in 12..17 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }
    }

    // Show error message
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // Show transaction details in dialog
    private fun showTransactionDetailsDialog(item: TransactionDisplayable) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_details, null)
        val categoryTextView = dialogView.findViewById<TextView>(R.id.detailCategory)
        val amountTextView = dialogView.findViewById<TextView>(R.id.detailAmount)
        val dateTextView = dialogView.findViewById<TextView>(R.id.detailDate)
        val notesTextView = dialogView.findViewById<TextView>(R.id.detailNotes)
        val notesLabelTextView = dialogView.findViewById<TextView>(R.id.detailNotesLabel)
        val receiptImageView = dialogView.findViewById<ImageView>(R.id.detailReceiptImage)

        val transaction = item.transaction

        // Set basic transaction info
        categoryTextView.text = item.categoryName
        dateTextView.text = fullDateFormat.format(Date(transaction.date))

        // Format amount and set color
        val formattedAmount = viewModel.formatCurrency(transaction.amount)
        if (transaction.type == "EXPENSE") {
            amountTextView.text = "- $formattedAmount"
            amountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_primary))
        } else {
            amountTextView.text = "+ $formattedAmount"
            amountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_primary))
        }

        // Show/hide notes section
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

        // Show dialog
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.close_button)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Show delete confirmation dialog
    private fun showDeleteConfirmationDialog(item: TransactionDisplayable) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_transaction))
            .setMessage(getString(R.string.delete_transaction_confirmation, item.transaction.description, viewModel.formatCurrency(item.transaction.amount)))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteTransaction(item.transaction)
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(300)
                    val username = sessionManager.getSessionUsername()
                    if (username != null) {
                        viewModel.loadTransactions(username)
                    }
                }
                Snackbar.make(binding.root, "Transaction deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 