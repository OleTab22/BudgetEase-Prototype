package com.budgetease.ui.category

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetease.R
import com.budgetease.data.model.Category
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.DialogAddCategoryBinding
import com.budgetease.databinding.DialogChooseIconBinding
import com.budgetease.databinding.DialogChooseColorBinding
import com.budgetease.databinding.FragmentCategoryManagementBinding
import com.budgetease.ui.common.UiState
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

// Manages expense categories creation, editing, and deletion
@AndroidEntryPoint
class CategoryManagementFragment : Fragment() {

    private var _binding: FragmentCategoryManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadCategories()

        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
        }

        binding.addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    // Sets up toolbar with title and navigation
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.title_category_management)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // Initializes RecyclerView with category adapter
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            context = requireContext(),
            onEditClick = { category ->
                showAddCategoryDialog(category)
            },
            onDeleteClick = { category ->
                showDeleteConfirmationDialog(category)
            }
        )
        binding.categoriesRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // Loads user's categories from database
    private fun loadCategories() {
        val username = sessionManager.getSessionUsername()
        if (username != null) {
            viewModel.loadCategories(username)
        } else {
            Snackbar.make(binding.root, "User session not found.", Snackbar.LENGTH_LONG).show()
        }
    }

    // Observes category list and save operation states
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoriesState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyViewContainer.visibility = View.GONE
                        binding.categoriesRecyclerView.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        if (state.data.isEmpty()) {
                            binding.emptyViewContainer.visibility = View.VISIBLE
                            binding.categoriesRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyViewContainer.visibility = View.GONE
                            binding.categoriesRecyclerView.visibility = View.VISIBLE
                            categoryAdapter.submitList(state.data)
                        }
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyViewContainer.visibility = View.VISIBLE
                        binding.categoriesRecyclerView.visibility = View.GONE
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    is UiState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyViewContainer.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveCategoryState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        val message = if (state.data == -1L) "Category updated successfully" else "Category added successfully"
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                        viewModel.resetSaveCategoryState()
                        loadCategories()
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetSaveCategoryState()
                    }
                    is UiState.Loading -> { }
                    is UiState.Idle -> { }
                }
            }
        }
    }

    // Shows dialog for adding or editing a category
    private fun showAddCategoryDialog(categoryToEdit: Category? = null) {
        val dialogBinding = DialogAddCategoryBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = categoryToEdit != null
        val dialogTitle = if (isEditing) "Edit Category" else "Add New Category"

        var selectedIconName: String = categoryToEdit?.icon ?: "ic_default_category"
        var selectedColorHex: String =
            categoryToEdit?.color?.let { String.format("#%06X", 0xFFFFFF and it) } ?: "#FF104D4F"

        // Initialize icon and color previews
        val initialIconResId = getIconResId(requireContext(), selectedIconName)
        dialogBinding.categoryIconPreview.setImageResource(initialIconResId)
        try {
            val color = Color.parseColor(selectedColorHex)
            (dialogBinding.categoryColorPreview.background as? GradientDrawable)?.setColor(color)
                ?: dialogBinding.categoryColorPreview.setBackgroundColor(color)
        } catch (e: IllegalArgumentException) {
            val defaultColor = Color.parseColor("#FF104D4F")
            (dialogBinding.categoryColorPreview.background as? GradientDrawable)?.setColor(defaultColor)
                ?: dialogBinding.categoryColorPreview.setBackgroundColor(defaultColor)
        }

        if (isEditing) {
            dialogBinding.categoryNameEditText.setText(categoryToEdit!!.name)
        }

        // Set up icon selection
        dialogBinding.chooseIconButton.setOnClickListener {
            showIconPickerDialog(selectedIconName) { iconName ->
                selectedIconName = iconName
                val iconResId = getIconResId(requireContext(), iconName)
                dialogBinding.categoryIconPreview.setImageResource(iconResId)
            }
        }

        // Set up color selection
        val colorClickListener = View.OnClickListener {
            showColorPickerDialog(selectedColorHex) { colorHex ->
                selectedColorHex = colorHex
                try {
                    val color = Color.parseColor(selectedColorHex)
                    (dialogBinding.categoryColorPreview.background as? GradientDrawable)?.setColor(color)
                        ?: dialogBinding.categoryColorPreview.setBackgroundColor(color)
                } catch (e: IllegalArgumentException) { }
            }
        }
        dialogBinding.chooseColorButton.setOnClickListener(colorClickListener)
        dialogBinding.categoryColorCardView.setOnClickListener(colorClickListener)

        // Show dialog and handle save action
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEditing) "Save" else "Add") { _, _ ->
                val name = dialogBinding.categoryNameEditText.text.toString().trim()
                if (name.isEmpty()) {
                    dialogBinding.categoryNameLayout.error = "Category name cannot be empty"
                    return@setPositiveButton
                }
                dialogBinding.categoryNameLayout.error = null

                val username = sessionManager.getSessionUsername()
                if (username != null) {
                    viewModel.saveCategory(
                        existingCategory = categoryToEdit?.copy(
                            name = name,
                            color = Color.parseColor(selectedColorHex),
                            icon = selectedIconName
                        ),
                        newCategoryName = if (!isEditing) name else null,
                        newCategoryColor = if (!isEditing) Color.parseColor(selectedColorHex) else null,
                        newCategoryIcon = if (!isEditing) selectedIconName else null,
                        username = username
                    )
                } else {
                    Snackbar.make(binding.root, "User session not found.", Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Gets resource ID for category icon with fallback
    private fun getIconResId(context: Context, iconName: String): Int {
        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        return if (resId != 0) resId else context.resources.getIdentifier("ic_default_category", "drawable", context.packageName)
    }

    // Shows dialog for selecting category icon
    private fun showIconPickerDialog(currentIconName: String, onIconSelected: (String) -> Unit) {
        val iconPickerDialogBinding = DialogChooseIconBinding.inflate(LayoutInflater.from(requireContext()))
        val availableIcons = listOf("ic_default_category", "ic_food", "ic_transport", "ic_shopping", "ic_salary", "ic_entertainment", "ic_health", "ic_education", "ic_saving", "ic_other", "ic_rent")

        val iconPickerAlertDialog = AlertDialog.Builder(requireContext())
            .setView(iconPickerDialogBinding.root)
            .setNegativeButton("Cancel", null)
            .create()

        val iconAdapter = IconPickerAdapter(requireContext(), availableIcons, currentIconName) { iconName ->
            onIconSelected(iconName)
            iconPickerAlertDialog.dismiss()
        }
        iconPickerDialogBinding.iconsRecyclerView.adapter = iconAdapter
        iconPickerDialogBinding.iconsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

        iconPickerAlertDialog.show()
    }

    // Shows dialog for selecting category color
    private fun showColorPickerDialog(currentColorHex: String, onColorSelected: (String) -> Unit) {
        val colorDialogBinding = DialogChooseColorBinding.inflate(LayoutInflater.from(requireContext()))
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B", "#FF104D4F"
        )

        val adapter = ColorPickerAdapter(requireContext(), colors, currentColorHex) { selectedHex ->
            onColorSelected(selectedHex)
        }
        colorDialogBinding.colorsRecyclerView.adapter = adapter
        colorDialogBinding.colorsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

        val colorDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose a Color")
            .setView(colorDialogBinding.root)
            .setPositiveButton("Select") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
        colorDialog.show()
    }

    // Shows confirmation dialog before deleting a category
    private fun showDeleteConfirmationDialog(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This will also delete associated budget goals and potentially affect transactions. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(category)
                Snackbar.make(binding.root, "Category '${category.name}' deleted", Snackbar.LENGTH_SHORT).show()
                loadCategories()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 