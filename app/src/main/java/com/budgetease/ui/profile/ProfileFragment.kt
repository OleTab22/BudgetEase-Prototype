package com.budgetease.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.budgetease.R
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.FragmentProfileBinding
import com.budgetease.ui.auth.LoginActivity
import com.budgetease.ui.common.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.budgetease.data.preferences.ThemeMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

// User profile screen with settings and account management
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observeViewModel()
        setupClickListeners()
        viewModel.loadUserProfile()
    }

    // Configure toolbar title
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.title_profile)
    }

    // Setup state observers for UI updates
    private fun observeViewModel() {
        // User profile observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userState.collect { state ->
                binding.profileProgressBar.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
                when (state) {
                    is UiState.Success -> {
                        binding.fullNameTextView.text = state.data.fullName
                        binding.usernameTextView.text = "@${state.data.username}"
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        binding.fullNameTextView.text = "N/A"
                        binding.usernameTextView.text = "N/A"
                    }
                    is UiState.Loading -> {}
                    is UiState.Idle -> {}
                }
            }
        }

        // Logout state observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logoutState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                        viewModel.resetLogoutState()
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetLogoutState()
                    }
                    is UiState.Loading -> {}
                    is UiState.Idle -> {}
                }
            }
        }

        // Theme mode observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.themeModeState.collect { themeMode ->
                updateThemeButtonText(themeMode)
            }
        }
    }

    // Setup button click handlers
    private fun setupClickListeners() {
        binding.editProfileButton.setOnClickListener {
            Snackbar.make(binding.root, "Edit Profile coming soon!", Snackbar.LENGTH_SHORT).show()
        }

        binding.settingsButton.setOnClickListener {
            Snackbar.make(binding.root, "Settings screen coming soon!", Snackbar.LENGTH_SHORT).show()
        }

        binding.notificationsButton.setOnClickListener {
            Snackbar.make(binding.root, "Notifications screen coming soon!", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.manageCategoriesButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_categoryManagement)
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.themeSelectionButton.setOnClickListener {
            showThemeSelectionDialog()
        }
    }

    // Show theme selection dialog
    private fun showThemeSelectionDialog() {
        val themes = ThemeMode.values()
        val themeNames = themes.map { it.name.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }}.toTypedArray()
        val currentTheme = viewModel.themeModeState.value
        val currentChoice = themes.indexOf(currentTheme)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themeNames, currentChoice) { dialog, which ->
                viewModel.setThemeMode(themes[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Update theme button text based on selected mode
    private fun updateThemeButtonText(themeMode: ThemeMode) {
        val themeText = when (themeMode) {
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
            ThemeMode.SYSTEM -> "System Default"
        }
        binding.themeSelectionButton.text = "Theme: $themeText"
    }

    // Show logout confirmation dialog
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.logout_title))
            .setMessage(getString(R.string.logout_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 