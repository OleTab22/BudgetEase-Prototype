package com.budgetease.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetease.R
import com.budgetease.data.model.BudgetGoalDisplayable
import com.budgetease.data.model.Category
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.DialogSetBudgetGoalBinding
import com.budgetease.databinding.FragmentBudgetBinding
import com.budgetease.ui.common.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log

// Manages budget goals and their display for different time periods
@AndroidEntryPoint
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var budgetGoalAdapter: BudgetGoalAdapter
    private var availableCategories: List<Category> = emptyList()

    @Inject
    lateinit var sessionManager: SessionManager

    private val currentCalendar: Calendar = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupPeriodSelectors()
        observeViewModel()
        loadInitialData()

        binding.addBudgetGoalFab.setOnClickListener {
            showSetBudgetGoalDialog(null)
        }

        binding.addBudgetGoalButton.setOnClickListener {
            showSetBudgetGoalDialog(null)
        }
    }

    // Configures the toolbar with title and menu items
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.title_budget)
        binding.toolbar.setOnMenuItemClickListener {
            false
        }
    }

    // Sets up the RecyclerView with adapter and layout manager
    private fun setupRecyclerView() {
        budgetGoalAdapter = BudgetGoalAdapter(requireContext(),
            onEditClick = { budgetGoalDisplayable ->
                showSetBudgetGoalDialog(budgetGoalDisplayable)
            },
            onDeleteClick = { budgetGoalDisplayable ->
                showDeleteConfirmationDialog(budgetGoalDisplayable)
            }
        )
        binding.budgetGoalsRecyclerView.apply {
            adapter = budgetGoalAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // Initializes month/year navigation controls
    private fun setupPeriodSelectors() {
        updateMonthYearText()
        binding.previousMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthYearText()
            loadBudgetGoalsForCurrentPeriod()
        }
        binding.nextMonthButton.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthYearText()
            loadBudgetGoalsForCurrentPeriod()
        }
        binding.monthYearText.setOnClickListener { 
            Snackbar.make(binding.root, "Month/Year picker coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Updates the displayed month and year text
    private fun updateMonthYearText() {
        binding.monthYearText.text = monthYearFormat.format(currentCalendar.time)
    }

    // Loads initial categories and budget goals data
    private fun loadInitialData() {
        val username = sessionManager.getSessionUsername()
        if (username != null) {
            viewModel.loadCategories(username)
            loadBudgetGoalsForCurrentPeriod()
        } else {
            Snackbar.make(binding.root, "User session not found.", Snackbar.LENGTH_LONG).show()
        }
    }

    // Loads budget goals for the selected month and year
    private fun loadBudgetGoalsForCurrentPeriod() {
        val username = sessionManager.getSessionUsername()
        if (username != null) {
            val month = currentCalendar.get(Calendar.MONTH) + 1
            val year = currentCalendar.get(Calendar.YEAR)
            viewModel.loadBudgetGoals(username, month, year)
        } else {
             binding.emptyViewContainer.visibility = View.VISIBLE
             binding.budgetGoalsRecyclerView.visibility = View.GONE
             binding.progressBar.visibility = View.GONE
        }
    }

    // Observes changes in budget goals, categories, and UI states
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.budgetGoalsState.collect { state ->
                Log.d("BudgetFragment", "BudgetGoals State Received: ${state::class.simpleName}")
                when (state) {
                    is UiState.Loading -> {
                        Log.d("BudgetFragment", "Setting visibility: ProgressBar=VISIBLE, Empty=GONE, RecyclerView=GONE")
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyViewContainer.visibility = View.GONE
                        binding.budgetGoalsRecyclerView.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        Log.d("BudgetFragment", "Setting visibility: ProgressBar=GONE, Data empty? ${state.data.isEmpty()}")
                        binding.progressBar.visibility = View.GONE
                        if (state.data.isEmpty()) {
                            Log.d("BudgetFragment", "Setting visibility: Empty=VISIBLE, RecyclerView=GONE")
                            binding.emptyViewContainer.visibility = View.VISIBLE
                            binding.budgetGoalsRecyclerView.visibility = View.GONE
                        } else {
                            Log.d("BudgetFragment", "Setting visibility: Empty=GONE, RecyclerView=VISIBLE")
                            binding.emptyViewContainer.visibility = View.GONE
                            binding.budgetGoalsRecyclerView.visibility = View.VISIBLE
                            budgetGoalAdapter.submitList(state.data)
                        }
                    }
                    is UiState.Error -> {
                        Log.d("BudgetFragment", "Setting visibility: ProgressBar=GONE, Empty=VISIBLE, RecyclerView=GONE, Message: ${state.message}")
                        binding.progressBar.visibility = View.GONE
                        binding.emptyViewContainer.visibility = View.VISIBLE
                        binding.budgetGoalsRecyclerView.visibility = View.GONE
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    is UiState.Idle -> {
                        Log.d("BudgetFragment", "Setting visibility: ProgressBar=GONE, Empty=GONE, RecyclerView=GONE (Idle state)")
                        binding.progressBar.visibility = View.GONE
                        binding.emptyViewContainer.visibility = View.GONE
                        binding.budgetGoalsRecyclerView.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoriesState.collect { state ->
                if (state is UiState.Success) {
                    availableCategories = state.data
                } else if (state is UiState.Error) {
                    Snackbar.make(binding.root, "Failed to load categories for dialog: ${state.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setBudgetGoalState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        Snackbar.make(binding.root, "Budget goal saved successfully", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetSetBudgetGoalState()
                        loadBudgetGoalsForCurrentPeriod()
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetSetBudgetGoalState()
                    }
                    is UiState.Loading -> { }
                    is UiState.Idle -> { }
                }
            }
        }
    }

    // Shows dialog for creating or editing a budget goal
    private fun showSetBudgetGoalDialog(existingGoalDisplayable: BudgetGoalDisplayable?) {
        if (availableCategories.isEmpty()) {
            Snackbar.make(binding.root, "No categories available to set a budget goal.", Snackbar.LENGTH_LONG).show()
            return
        }

        val dialogBinding = DialogSetBudgetGoalBinding.inflate(LayoutInflater.from(requireContext()))
        val dialogTitle = if (existingGoalDisplayable != null) "Edit Budget Goal" else "Set Budget Goal"

        val categoryArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, availableCategories)
        dialogBinding.categoryAutoCompleteTextView.setAdapter(categoryArrayAdapter)

        existingGoalDisplayable?.let {
            dialogBinding.minAmountEditText.setText(it.budgetGoal.minAmount.toString())
            dialogBinding.maxAmountEditText.setText(it.budgetGoal.maxAmount.toString())
            val categoryIndex = availableCategories.indexOfFirst { cat -> cat.id == it.budgetGoal.categoryId }
            if (categoryIndex != -1) {
                dialogBinding.categoryAutoCompleteTextView.setText(availableCategories[categoryIndex].name, false)
            }
            dialogBinding.categoryAutoCompleteTextView.isEnabled = false
        }

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setPositiveButton(if (existingGoalDisplayable != null) "Save" else "Add") { _, _ ->
                val selectedCategoryText = dialogBinding.categoryAutoCompleteTextView.text.toString()
                val selectedCategory = availableCategories.find { it.name == selectedCategoryText }

                if (selectedCategory == null && existingGoalDisplayable == null) {
                    Snackbar.make(binding.root, "Please select a valid category.", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val categoryIdToSave = selectedCategory?.id ?: existingGoalDisplayable!!.budgetGoal.categoryId

                val minAmountString = dialogBinding.minAmountEditText.text.toString()
                val maxAmountString = dialogBinding.maxAmountEditText.text.toString()

                val minAmount = minAmountString.toDoubleOrNull()
                val maxAmount = maxAmountString.toDoubleOrNull()

                if (minAmount == null) {
                    dialogBinding.minAmountLayout.error = "Min amount required"
                    return@setPositiveButton
                } else {
                    dialogBinding.minAmountLayout.error = null
                }
                if (maxAmount == null) {
                    dialogBinding.maxAmountLayout.error = "Max amount required"
                    return@setPositiveButton
                } else {
                    dialogBinding.maxAmountLayout.error = null
                }
                if (minAmount > maxAmount) {
                     dialogBinding.minAmountLayout.error = "Min amount cannot exceed max amount"
                     dialogBinding.maxAmountLayout.error = "Max amount cannot be less than min amount"
                    return@setPositiveButton
                }

                val username = sessionManager.getSessionUsername()
                val month = currentCalendar.get(Calendar.MONTH) + 1
                val year = currentCalendar.get(Calendar.YEAR)

                if (username != null) {
                    viewModel.setBudgetGoal(
                        existingGoalId = existingGoalDisplayable?.budgetGoal?.id,
                        categoryId = categoryIdToSave,
                        minAmount = minAmount,
                        maxAmount = maxAmount,
                        month = month,
                        year = year,
                        username = username
                    )
                } else {
                    Snackbar.make(binding.root, "User session not found.", Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Shows confirmation dialog before deleting a budget goal
    private fun showDeleteConfirmationDialog(item: BudgetGoalDisplayable) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Budget Goal")
            .setMessage("Are you sure you want to delete the budget goal for '${item.categoryName}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteBudgetGoal(item.budgetGoal)
                Snackbar.make(binding.root, "Budget goal for '${item.categoryName}' deleted.", Snackbar.LENGTH_SHORT).show()
                loadBudgetGoalsForCurrentPeriod()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 