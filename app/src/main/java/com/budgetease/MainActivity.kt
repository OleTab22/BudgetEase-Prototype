package com.budgetease

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.budgetease.data.preferences.SessionManager
import com.budgetease.ui.auth.LoginActivity
import com.budgetease.ui.dashboard.DashboardActivity
import com.budgetease.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Main entry point of the application. Handles initial navigation based on user session and onboarding status.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager // Manages user session and onboarding status.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Determine the appropriate starting activity.
        if (sessionManager.isUserLoggedIn()) {
            // If user is logged in, navigate to Dashboard.
            navigateTo(DashboardActivity::class.java)
        } else if (!sessionManager.hasCompletedOnboarding()) {
            // If not logged in and onboarding is not complete, navigate to Onboarding.
            navigateTo(OnboardingActivity::class.java)
        } else {
            // If not logged in but onboarding is complete, navigate to Login.
            navigateTo(LoginActivity::class.java)
        }
        finish() // Finish MainActivity to remove it from the back stack.
    }

    // Helper function to start an activity and clear the task stack.
    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        // Flags to clear the back stack and start a new task, common for entry points.
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK 
        startActivity(intent)
    }
}
