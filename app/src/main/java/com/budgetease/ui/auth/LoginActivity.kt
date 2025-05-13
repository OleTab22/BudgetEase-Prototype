package com.budgetease.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.budgetease.R
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.ActivityLoginBinding
import com.budgetease.ui.common.UiState
import com.budgetease.ui.dashboard.DashboardActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// Handles user login screen and authentication
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var isDarkMode = false
    private var isPasswordVisible = false
    private val viewModel: AuthViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputValidation()
        setupClickListeners()
        observeLoginState()
    }

    // Sets up real-time input validation
    private fun setupInputValidation() {
        binding.email.addTextChangedListener { text ->
            if (isValidEmail(text.toString())) {
                binding.email.error = null
            }
        }

        binding.password.addTextChangedListener { text ->
            if (isValidPassword(text.toString())) {
                binding.password.error = null
            }
        }
    }

    // Sets up all button click handlers
    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            if (validateInputs()) {
                val email = binding.email.text.toString()
                val password = binding.password.text.toString()
                viewModel.login(email, password)
            }
        }

        binding.googleLogin.setOnClickListener {
            showMessage("Google login coming soon")
        }

        binding.appleLogin.setOnClickListener {
            showMessage("Apple login coming soon")
        }

        binding.signupButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.darkModeToggle.setOnClickListener {
            toggleDarkMode()
        }
    }

    // Toggles password field visibility
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.password.transformationMethod = if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
        binding.password.setSelection(binding.password.text?.length ?: 0)
    }

    // Validates all input fields
    private fun validateInputs(): Boolean {
        val isEmailValid = isValidEmail(binding.email.text.toString())
        val isPasswordValid = isValidPassword(binding.password.text.toString())

        if (!isEmailValid) binding.email.error = getString(R.string.email_error)
        if (!isPasswordValid) binding.password.error = getString(R.string.password_error)

        return isEmailValid && isPasswordValid
    }

    // Checks if email format is valid
    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Checks if password meets requirements
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    // Handles login state changes
    private fun observeLoginState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        binding.loginButton.isEnabled = true
                    }
                    is UiState.Loading -> {
                        binding.loginButton.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.loginButton.isEnabled = true
                        showMessage("Login successful! Welcome back ${state.data.username}")
                        sessionManager.setUserLoggedIn(true)
                        sessionManager.saveSessionUsername(state.data.username)

                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    is UiState.Error -> {
                        binding.loginButton.isEnabled = true
                        showMessage(state.message)
                    }
                }
            }
        }
    }

    // Toggles between light and dark theme
    private fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Shows a snackbar message
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
} 