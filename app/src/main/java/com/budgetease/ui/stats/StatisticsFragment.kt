package com.budgetease.ui.stats

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.budgetease.R
import com.budgetease.data.preferences.SessionManager
import com.budgetease.databinding.FragmentStatisticsBinding
import com.budgetease.ui.common.UiState
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import androidx.navigation.fragment.findNavController

// Fragment for displaying financial statistics charts
@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager

    private val selectedDatesFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        setupPeriodSelector()
        observeViewModel()
        loadInitialData()
        setupExpensePieChartStyle()
        setupIncomeExpenseBarChartStyle()
        setupSavingsProgressLineChartStyle()

        binding.statisticsShortcutBudgetGoalButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_budget)
        }
    }

    private fun setupPeriodSelector(){
        updateSelectedPeriodText()
        binding.selectPeriodButton.setOnClickListener {
            showDateRangePicker()
        }
    }
    
    private fun updateSelectedPeriodText() {
        val startDateFormatted = selectedDatesFormat.format(Date(viewModel.getCurrentStartDate()))
        val endDateFormatted = selectedDatesFormat.format(Date(viewModel.getCurrentEndDate()))
        binding.selectedPeriodText.text = "Period: $startDateFormatted - $endDateFormatted"
    }

    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Statistics Period")
            .setSelection(Pair(viewModel.getCurrentStartDate(), viewModel.getCurrentEndDate()))
            .build()

        datePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long> ->
            viewModel.updateSelectedPeriod(selection.first, selection.second)
            updateSelectedPeriodText()
            loadCategorySpendingData()
        }
        datePicker.show(parentFragmentManager, "STATS_DATE_RANGE_PICKER")
    }

    private fun loadInitialData() {
         loadCategorySpendingData()
    }

    private fun loadCategorySpendingData(){
        val username = sessionManager.getSessionUsername()
        if (username != null) {
            viewModel.loadCategorySpending(username)
        } else {
            Snackbar.make(binding.root, "User session not found.", Snackbar.LENGTH_LONG).show()
            binding.expensePieChart.visibility = View.GONE
            binding.emptyPieChartContainer.visibility = View.VISIBLE
            binding.emptyChartText.text = "Login to view statistics."
            binding.statsProgressBar.visibility = View.GONE
        }
    }

    // Handle UI state changes for all charts
    private fun observeViewModel() {
        // Category spending pie chart
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorySpendingState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.statsProgressBar.visibility = View.VISIBLE
                        binding.expensePieChart.visibility = View.GONE
                        binding.emptyPieChartContainer.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.expensePieChart.visibility = View.VISIBLE
                        binding.emptyPieChartContainer.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                        if (state.data.isEmpty()) {
                            binding.emptyChartText.text = "No spending data for this period."
                        }
                        binding.statsProgressBar.visibility = View.GONE
                        updateExpensePieChart(state.data)
                    }
                    is UiState.Error -> {
                        binding.expensePieChart.visibility = View.GONE
                        binding.emptyPieChartContainer.visibility = View.VISIBLE
                        binding.emptyChartText.text = state.message
                        binding.statsProgressBar.visibility = View.GONE
                    }
                    is UiState.Idle -> {
                        binding.statsProgressBar.visibility = View.GONE
                        binding.expensePieChart.visibility = View.VISIBLE
                        binding.emptyPieChartContainer.visibility = View.GONE
                        binding.statsProgressBar.visibility = View.GONE
                    }
                }
            }
        }

        // Income/expense bar chart
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.incomeExpenseDetailsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.incomeExpenseChart.visibility = View.VISIBLE
                        binding.emptyBarChartContainer.visibility = if (state.data.totalIncome == 0.0 && state.data.totalExpenses == 0.0) View.VISIBLE else View.GONE
                        if (state.data.totalIncome == 0.0 && state.data.totalExpenses == 0.0) {
                            binding.emptyIncomeExpenseChartText.text = "No income/expense data for this period."
                        }
                        binding.incomeExpenseChartProgressBar.visibility = View.GONE
                        updateIncomeExpenseBarChart(state.data)
                    }
                    is UiState.Error -> {
                        binding.incomeExpenseChart.visibility = View.GONE
                        binding.emptyBarChartContainer.visibility = View.VISIBLE
                        binding.emptyIncomeExpenseChartText.text = state.message
                        binding.incomeExpenseChartProgressBar.visibility = View.GONE
                    }
                    is UiState.Loading -> {
                        binding.incomeExpenseChart.visibility = View.GONE
                        binding.emptyBarChartContainer.visibility = View.GONE
                        binding.incomeExpenseChartProgressBar.visibility = View.VISIBLE
                    }
                    is UiState.Idle -> {
                        binding.incomeExpenseChart.visibility = View.VISIBLE
                        binding.emptyBarChartContainer.visibility = View.GONE
                        binding.incomeExpenseChartProgressBar.visibility = View.GONE
                    }
                }
            }
        }

        // Monthly savings line chart
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.monthlyNetSavingsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        binding.savingsChart.visibility = View.VISIBLE
                        binding.emptyLineChartContainer.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                        if (state.data.isEmpty()) {
                            binding.emptySavingsChartText.text = "Select a longer period to view savings trend."
                        }
                        binding.savingsChartProgressBar.visibility = View.GONE
                        updateSavingsProgressLineChart(state.data)
                    }
                    is UiState.Error -> {
                        binding.savingsChart.visibility = View.GONE
                        binding.emptyLineChartContainer.visibility = View.VISIBLE
                        binding.emptySavingsChartText.text = state.message
                        binding.savingsChartProgressBar.visibility = View.GONE
                    }
                    is UiState.Loading -> {
                        binding.savingsChart.visibility = View.GONE
                        binding.emptyLineChartContainer.visibility = View.GONE
                        binding.savingsChartProgressBar.visibility = View.VISIBLE
                    }
                    is UiState.Idle -> {
                        binding.savingsChart.visibility = View.VISIBLE
                        binding.emptyLineChartContainer.visibility = View.GONE
                        binding.savingsChartProgressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupExpensePieChartStyle(){
        binding.expensePieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(AndroidColor.WHITE)
            setTransparentCircleColor(AndroidColor.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Expenses"
            setCenterTextSize(16f)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            legend.isEnabled = true
        }
    }

    private fun updateExpensePieChart(spendingData: List<CategorySpending>) {
        val entries = ArrayList<PieEntry>()
        for (item in spendingData) {
            entries.add(PieEntry(item.totalAmount.toFloat(), item.categoryName))
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() +
                         ColorTemplate.JOYFUL_COLORS.toList() +
                         ColorTemplate.COLORFUL_COLORS.toList()

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.expensePieChart))
        data.setValueTextSize(12f)
        data.setValueTextColor(AndroidColor.BLACK)
        
        binding.expensePieChart.data = data
        binding.expensePieChart.invalidate()
        binding.expensePieChart.animateY(1400, Easing.EaseInOutQuad)
    }

    private fun setupIncomeExpenseBarChartStyle() {
        binding.incomeExpenseChart.apply {
            description.isEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            isHighlightFullBarEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(arrayOf("Income", "Expenses"))
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun updateIncomeExpenseBarChart(data: IncomeExpenseDetails) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, data.totalIncome.toFloat()))
        entries.add(BarEntry(1f, data.totalExpenses.toFloat()))

        val dataSet = BarDataSet(entries, "Income/Expense")
        val incomeColor = ContextCompat.getColor(requireContext(), R.color.income_primary)
        val expenseColor = ContextCompat.getColor(requireContext(), R.color.expense_primary)
        dataSet.colors = listOf(incomeColor, expenseColor)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        binding.incomeExpenseChart.data = barData
        binding.incomeExpenseChart.invalidate()
        binding.incomeExpenseChart.animateY(1000)
    }

    private fun setupSavingsProgressLineChartStyle() {
        binding.savingsChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val monthYearFormat = SimpleDateFormat("MMM yy", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return monthYearFormat.format(Date(value.toLong()))
                    }
                }
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        }
    }

    private fun updateSavingsProgressLineChart(savingsData: List<MonthlyNetSavings>) {
        if (savingsData.isEmpty()) {
            binding.savingsChart.clear()
            binding.savingsChart.invalidate()
            binding.emptySavingsChartText.text = "No data to display for savings progress."
            binding.emptySavingsChartText.visibility = View.VISIBLE
            binding.savingsChart.visibility = View.GONE
            return
        }

        val entries = ArrayList<Entry>()
        savingsData.forEachIndexed { _, monthlyNetSavings ->
            entries.add(Entry(monthlyNetSavings.monthEpochStart.toFloat(), monthlyNetSavings.netSavings.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Monthly Net Savings")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary))
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.primary_light)
        dataSet.fillAlpha = 100
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        binding.savingsChart.data = lineData
        binding.savingsChart.invalidate()
        binding.savingsChart.animateX(1000)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 