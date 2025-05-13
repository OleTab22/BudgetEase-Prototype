package com.budgetease.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.budgetease.R
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.ActivityDashboardBinding
import com.budgetease.ui.auth.LoginActivity
import com.budgetease.ui.common.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.view.View
import android.view.MotionEvent
import android.content.Context
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import android.util.Log

// Main dashboard activity managing navigation and UI components
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Enable edge-to-edge display

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupNavigation()
        setupClickListeners()
        observeData()
        loadData()
    }

    private fun applyWindowInsets() {
        // Handle root view insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = insets.left, top = 0, right = insets.right, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Handle app bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainAppBarLayout) { view, windowInsets ->
             val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
             view.updatePadding(top = insets.top)
             windowInsets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // Define main navigation destinations
        val topLevelDestinations = setOf(
            R.id.navigation_home,
            R.id.navigation_budget,
            R.id.navigation_stats,
            R.id.navigation_wallet,
            R.id.navigation_profile
        )

        // Update toolbar title based on current screen
        fun updateTitle(destinationId: Int) {
            binding.toolbarTitleDashboard.text = when (destinationId) {
                R.id.navigation_home -> getString(R.string.title_dashboard)
                R.id.navigation_budget -> getString(R.string.title_budget)
                R.id.navigation_stats -> getString(R.string.title_statistics)
                R.id.navigation_wallet -> getString(R.string.title_wallet)
                R.id.navigation_profile -> getString(R.string.title_profile)
                else -> ""
            }
        }

        // Handle navigation UI updates
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in topLevelDestinations) {
                binding.bottomNav.visibility = View.VISIBLE

                when (destination.id) {
                    R.id.navigation_home -> {
                        binding.mainAppBarLayout.visibility = View.VISIBLE
                        binding.addTransactionFab.visibility = View.VISIBLE
                        updateTitle(destination.id)
                    }
                    R.id.navigation_budget -> {
                        binding.mainAppBarLayout.visibility = View.GONE
                        binding.addTransactionFab.visibility = View.GONE
                    }
                    R.id.navigation_stats -> {
                        binding.mainAppBarLayout.visibility = View.VISIBLE
                        binding.addTransactionFab.visibility = View.GONE
                        updateTitle(destination.id)
                    }
                    R.id.navigation_wallet -> {
                        binding.mainAppBarLayout.visibility = View.VISIBLE
                        binding.addTransactionFab.visibility = View.VISIBLE
                        updateTitle(destination.id)
                    }
                    R.id.navigation_profile -> {
                        binding.mainAppBarLayout.visibility = View.GONE
                        binding.addTransactionFab.visibility = View.GONE
                    }
                    else -> {
                        binding.mainAppBarLayout.visibility = View.GONE
                        binding.addTransactionFab.visibility = View.GONE
                    }
                }
            } else {
                // Hide navigation elements for detail screens
                binding.mainAppBarLayout.visibility = View.GONE
                binding.bottomNav.visibility = View.GONE
                binding.addTransactionFab.visibility = View.GONE
            }
        }
    }

    private fun loadData() {
        if (sessionManager.isUserLoggedIn()) {
            viewModel.loadDashboardData()
        } else {
            // Redirect to login if session invalid
            showSessionInvalidError()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun observeData() {
        // UI updates now handled in DashboardFragment
    }

    private fun setupClickListeners() {
        val fab = binding.addTransactionFab
        val sharedPreferences = getSharedPreferences("FabSettings", Context.MODE_PRIVATE)

        // Restore or set default FAB position
        val initialX = sharedPreferences.getFloat("fabX", -1f)
        val initialY = sharedPreferences.getFloat("fabY", -1f)

        if (initialX != -1f && initialY != -1f) {
            fab.x = initialX
            fab.y = initialY
        } else {
            // Set default position in bottom-right corner
            fab.post {
                val parent = fab.parent as View
                fab.x = parent.width - fab.width - (16 * resources.displayMetrics.density)
                fab.y = parent.height - fab.height - binding.bottomNav.height - (32 * resources.displayMetrics.density)
            }
        }

        // Setup FAB drag and click handling
        var dX = 0f
        var dY = 0f
        var moved = false

        fab.setOnTouchListener { view, event ->
            val parent = view.parent as View
            val newX = event.rawX + dX
            val newY = event.rawY + dY

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    moved = false
                    view.bringToFront()
                }
                MotionEvent.ACTION_MOVE -> {
                    val clampedX = newX.coerceIn(0f, (parent.width - view.width).toFloat())
                    val clampedY = newY.coerceIn(0f, (parent.height - view.height).toFloat())
                    view.x = clampedX
                    view.y = clampedY
                    moved = true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        navigateToTransactionAdd()
                    } else {
                        // Save new FAB position
                        with(sharedPreferences.edit()) {
                            putFloat("fabX", view.x)
                            putFloat("fabY", view.y)
                            apply()
                        }
                    }
                }
                else -> return@setOnTouchListener false
            }
            true
        }
    }

    // Navigate to transaction creation screen
    private fun navigateToTransactionAdd() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (navController.currentDestination?.id != R.id.addTransactionFragment) {
            try {
                navController.navigate(R.id.action_dashboard_to_addTransaction)
            } catch (e: IllegalArgumentException) {
                Snackbar.make(binding.root, "Could not navigate to Add Transaction.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSessionInvalidError() {
        Snackbar.make(binding.root, "Session invalid. Please login again.", Snackbar.LENGTH_SHORT).show()
    }
} 