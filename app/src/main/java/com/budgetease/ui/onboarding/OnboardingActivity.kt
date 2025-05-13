package com.budgetease.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.ActivityOnboardingBinding
import com.budgetease.ui.auth.LoginActivity
import com.budgetease.ui.auth.RegisterActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Initial app screen for first-time users
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    // Setup navigation to register and login screens
    private fun setupClickListeners() {
        // Navigate to registration
        binding.getStartedButton.setOnClickListener {
            sessionManager.setOnboardingCompleted(true)
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to login
        binding.loginText.setOnClickListener {
            sessionManager.setOnboardingCompleted(true)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
} 