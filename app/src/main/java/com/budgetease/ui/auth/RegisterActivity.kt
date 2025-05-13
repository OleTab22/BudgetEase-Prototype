package com.budgetease.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.budgetease.R
import com.budgetease.databinding.ActivityRegisterBinding
import com.budgetease.ui.common.UiState
import com.budgetease.ui.dashboard.DashboardActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Handles user registration and account creation
@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputValidation()
        setupClickListeners()
        observeRegistrationState()
    }

    // Sets up real-time validation for all input fields
    private fun setupInputValidation() {
        binding.fullName.doAfterTextChanged { editable ->
            if (isValidName(editable.toString())) {
                binding.fullName.error = null
            }
        }

        binding.email.doAfterTextChanged { editable ->
            if (isValidEmail(editable.toString())) {
                binding.email.error = null
            }
        }

        binding.password.doAfterTextChanged { editable ->
            if (isValidPassword(editable.toString())) {
                binding.password.error = null
            }
        }

        binding.confirmPassword.doAfterTextChanged { editable ->
            if (isValidConfirmPassword(editable.toString(), binding.password.text.toString())) {
                binding.confirmPassword.error = null
            }
        }
    }

    // Sets up click handlers for registration and navigation
    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
                val fullName = binding.fullName.text.toString()
                val email = binding.email.text.toString()
                val password = binding.password.text.toString()
                viewModel.register(email, password, fullName)
            }
        }

        binding.loginText.setOnClickListener {
            finish() // Return to login screen
        }
    }

    // Toggles visibility of password fields
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.password.transformationMethod = if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
        binding.confirmPassword.transformationMethod = if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
        // Maintain cursor position
        binding.password.setSelection(binding.password.text?.length ?: 0)
        binding.confirmPassword.setSelection(binding.confirmPassword.text?.length ?: 0)
    }

    // Validates all registration form inputs
    private fun validateInputs(): Boolean {
        val isNameValid = isValidName(binding.fullName.text.toString())
        val isEmailValid = isValidEmail(binding.email.text.toString())
        val isPasswordValid = isValidPassword(binding.password.text.toString())
        val isConfirmPasswordValid = isValidConfirmPassword(
            binding.confirmPassword.text.toString(),
            binding.password.text.toString()
        )

        if (!isNameValid) binding.fullName.error = getString(R.string.name_error)
        if (!isEmailValid) binding.email.error = getString(R.string.email_error)
        if (!isPasswordValid) binding.password.error = getString(R.string.password_error)
        if (!isConfirmPasswordValid) binding.confirmPassword.error = getString(R.string.confirm_password_error)

        return isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    }

    // Checks if name meets minimum length requirement
    private fun isValidName(name: String): Boolean {
        return name.length >= 2
    }

    // Validates email format using Android patterns
    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Checks if password meets minimum length requirement
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    // Verifies that password confirmation matches
    private fun isValidConfirmPassword(confirmPassword: String, password: String): Boolean {
        return confirmPassword == password
    }

    // Handles registration state changes and UI updates
    private fun observeRegistrationState() {
        lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        binding.registerButton.isEnabled = true
                    }
                    is UiState.Loading -> {
                        binding.registerButton.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.registerButton.isEnabled = true
                        showMessage("Registration successful! Welcome ${state.data.username}")
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                    is UiState.Error -> {
                        binding.registerButton.isEnabled = true
                        showMessage(state.message)
                    }
                }
            }
        }
    }

    // Displays a snackbar message to the user
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}