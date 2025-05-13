package com.budgetease.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgetease.R
import com.budgetease.databinding.ActivityForgotPasswordBinding
import com.budgetease.ui.common.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Handles password reset request screen
@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    // Sets up button click handlers
    private fun setupClickListeners() {
        binding.resetButton.setOnClickListener {
            val emailText = binding.email.text.toString().trim()
            if (isValidEmail(emailText)) {
                binding.email.error = null
                viewModel.resetPassword(emailText)
            } else {
                binding.email.error = getString(R.string.email_error)
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    // Watches for password reset state changes
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.resetPasswordState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.resetButton.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.resetButton.isEnabled = true
                        showMessage(getString(R.string.reset_email_sent))
                    }
                    is UiState.Error -> {
                        binding.resetButton.isEnabled = true
                        binding.email.error = state.message
                    }
                    is UiState.Idle -> {
                        binding.resetButton.isEnabled = true
                        binding.email.error = null
                    }
                }
            }
        }
    }

    // Checks if email format is valid
    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Shows a snackbar message
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
} 