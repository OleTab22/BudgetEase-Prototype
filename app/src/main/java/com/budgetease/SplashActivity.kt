package com.budgetease

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.budgetease.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen visible for this Activity
        splashScreen.setKeepOnScreenCondition { true }

        // Navigate to OnboardingActivity (or your main activity)
        // This navigation should happen once your app is ready (e.g., initial data loaded)
        // For a simple timed splash, you could use a Handler, but installSplashScreen handles it better.
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
} 