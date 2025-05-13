package com.budgetease.ui.transaction

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.budgetease.R
import com.budgetease.databinding.FragmentAddTransactionBinding
import com.budgetease.ui.common.UiState
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import com.budgetease.data.preferences.SessionManager
import javax.inject.Inject
import com.budgetease.data.model.Category
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import androidx.navigation.fragment.findNavController
import android.os.Environment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.ShapeAppearanceModel
import com.bumptech.glide.Glide
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher

// Fragment for adding new transactions with receipt capture functionality
@AndroidEntryPoint
class AddTransactionFragment : Fragment() {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    // Camera and image capture components
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    // Date and time selection
    private var selectedDate = System.currentTimeMillis()
    private var selectedHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var selectedMinute = Calendar.getInstance().get(Calendar.MINUTE)

    private val chipIdToCategoryMap = mutableMapOf<Int, Category>()

    // Permission handling for camera and storage
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            startCamera()
        } else {
            handleCameraPermissionDenied()
        }
    }

    // Image selection from gallery
    private val selectImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setReceiptImageUri(it)
            showCapturedImage(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadCategories()
        setupDatePicker()
        setupCameraComponents()
        observeViewModelStates()

        binding.transactionTypeToggleGroup.check(R.id.expenseButton)
        binding.amountInput.requestFocus()
        ensurePermissionsForReceipt()
    }

    private fun handleCameraPermissionDenied() {
        val shouldShowRationaleCamera = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        val message = if (!shouldShowRationaleCamera) {
            getString(R.string.camera_permission_denied_permanently)
        } else {
            getString(R.string.camera_permission_required)
        }

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(if (!shouldShowRationaleCamera) getString(R.string.settings) else null) { _ ->
                if (!shouldShowRationaleCamera) {
                    openAppSettings()
                }
            }.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    // Initialize UI components and listeners
    private fun setupUI() {
        setupNavigationListeners()
        setupButtonListeners()
        setupCategoryChipGroup()
    }

    private fun setupNavigationListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.manageCategoriesButton.setOnClickListener { 
            findNavController().navigate(R.id.action_addTransactionFragment_to_categoryManagementFragment)
        }
    }

    private fun setupButtonListeners() {
        binding.attachReceiptButton.setOnClickListener {
            checkPermissionsAndStartCamera()
        }

        binding.selectReceiptButton.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }
    }

    private fun setupCategoryChipGroup() {
        if (binding.categoryChipGroup.childCount > 0) {
            (binding.categoryChipGroup.getChildAt(0) as? Chip)?.isChecked = true
        }

        addManageCategoriesChip()
    }

    // Add the "Add Category" chip to the chip group
    private fun addManageCategoriesChip() {
        val addChip = Chip(context).apply {
            setText(getString(R.string.add_category_chip_text))
            setChipBackgroundColorResource(R.color.chip_add_background_selector)
            setTextColor(ContextCompat.getColorStateList(context, R.color.chip_add_text_selector))
            chipMinHeight = resources.getDimension(R.dimen.chip_min_height)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(resources.getDimension(R.dimen.chip_corner_radius))
                .build()
            isCheckable = false
            isClickable = true
            isFocusable = true
            setOnClickListener {
                findNavController().navigate(R.id.action_addTransactionFragment_to_categoryManagementFragment)
            }
        }
        binding.categoryChipGroup.addView(addChip)
    }

    // Load user categories from database
    private fun loadCategories() {
        sessionManager.getSessionUsername()?.let { username ->
            viewModel.loadCategories(username)
        } ?: run {
            Snackbar.make(binding.root, "Error: User not logged in.", Snackbar.LENGTH_LONG).show()
        }
    }

    // Setup date and time picker functionality
    private fun setupDatePicker() {
        updateDateText()
        binding.dateButton.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(selectedDate)
            .build()
            .apply {
                addOnPositiveButtonClickListener { selection ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = selection
                    }
                    showTimePicker(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                }
            }.show(parentFragmentManager, "date_picker")
    }

    private fun showTimePicker(year: Int, month: Int, day: Int) {
        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Select Transaction Time")
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    selectedHour = hour
                    selectedMinute = minute
                    updateSelectedDateTime(year, month, day)
                }
            }.show(parentFragmentManager, "time_picker")
    }

    private fun updateSelectedDateTime(year: Int, month: Int, day: Int) {
        Calendar.getInstance().apply {
            set(year, month, day, selectedHour, selectedMinute, 0)
            set(Calendar.MILLISECOND, 0)
            selectedDate = timeInMillis
        }
        updateDateText()
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        binding.dateButton.text = dateFormat.format(Date(selectedDate))
    }

    // Camera setup and image capture
    private fun setupCameraComponents() {
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun checkPermissionsAndStartCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            setupCameraUseCase(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupCameraUseCase(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            takePhoto()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to start camera: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun takePhoto() {
        imageCapture?.let { capture ->
            val photoFile = File(
                outputDirectory,
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis()) + ".jpg"
            )

            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        showCapturedImage(Uri.fromFile(photoFile))
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Snackbar.make(binding.root, "Photo capture failed: ${exc.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun showCapturedImage(uri: Uri) {
        binding.apply {
            attachReceiptButton.text = getString(R.string.change_receipt)
            selectReceiptButton.text = getString(R.string.change_receipt)
            receiptPreviewImage.visibility = View.VISIBLE
            
            Glide.with(this@AddTransactionFragment)
                .load(uri)
                .placeholder(R.drawable.ic_receipt_placeholder)
                .error(R.drawable.ic_error_placeholder)
                .into(receiptPreviewImage)

            attachReceiptButton.alpha = 0.7f
            selectReceiptButton.alpha = 0.7f
        }
        viewModel.setReceiptImageUri(uri)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = ContextCompat.getExternalFilesDirs(requireContext(), Environment.DIRECTORY_PICTURES)
            .firstOrNull()?.let {
                File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
            }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    // Input validation and transaction saving
    private fun validateInputs(): Boolean {
        var isValid = true

        val amountString = binding.amountInput.text.toString()
        val amount = amountString.toDoubleOrNull()
        
        binding.amountInputLayout.error = when {
            amountString.isEmpty() || amountString == "0.00" -> {
                isValid = false
                getString(R.string.amount_required)
            }
            amount == null || amount <= 0 -> {
                isValid = false
                getString(R.string.amount_invalid)
            }
            else -> null
        }

        if (binding.categoryChipGroup.checkedChipId == View.NO_ID) {
            Snackbar.make(binding.root, "Please select a category.", Snackbar.LENGTH_SHORT).show()
            isValid = false
        }

        binding.notesInputLayout.error = if (binding.notesInput.text.isNullOrEmpty()) {
            isValid = false
            getString(R.string.notes_required)
        } else null

        return isValid
    }

    private fun saveTransaction() {
        val amount = binding.amountInput.text.toString().toDoubleOrNull() ?: return
        val notes = binding.notesInput.text.toString()
        val type = if (binding.transactionTypeToggleGroup.checkedButtonId == R.id.incomeButton) {
            AddTransactionViewModel.TYPE_INCOME
        } else {
            AddTransactionViewModel.TYPE_EXPENSE
        }

        val categoryId = chipIdToCategoryMap[binding.categoryChipGroup.checkedChipId]?.id ?: return
        val imagePath = viewModel.receiptImageUri.value?.toString()
        val username = sessionManager.getSessionUsername() ?: return

        viewModel.addTransaction(
            amount = amount,
            description = notes,
            categoryId = categoryId,
            date = selectedDate,
            imagePath = imagePath,
            type = type,
            username = username
        )
    }

    // State observation and UI updates
    private fun observeViewModelStates() {
        observeCategoriesState()
        observeTransactionState()
    }

    private fun observeCategoriesState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoriesState.collect { state ->
                when (state) {
                    is UiState.Success -> setupCategoryChips(state.data)
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        binding.categoryChipGroup.removeAllViews()
                    }
                    is UiState.Loading, is UiState.Idle -> binding.categoryChipGroup.removeAllViews()
                }
            }
        }
    }

    private fun observeTransactionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addTransactionState.collect { state ->
                binding.saveButton.isEnabled = state !is UiState.Loading
                when (state) {
                    is UiState.Success -> {
                        Snackbar.make(binding.root, R.string.transaction_saved, Snackbar.LENGTH_SHORT).show()
                        viewModel.resetAddTransactionState()
                        findNavController().navigateUp()
                    }
                    is UiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        viewModel.resetAddTransactionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupCategoryChips(categories: List<Category>) {
        binding.categoryChipGroup.removeAllViews()
        chipIdToCategoryMap.clear()

        if (categories.isEmpty()) {
            Snackbar.make(binding.root, R.string.no_categories_found_snackbar, Snackbar.LENGTH_LONG).show()
        }

        categories.forEach { category ->
            addCategoryChip(category)
        }

        if (binding.categoryChipGroup.childCount > 0) {
            (binding.categoryChipGroup.getChildAt(0) as? Chip)?.isChecked = true
        }
    }

    private fun addCategoryChip(category: Category) {
        val chip = Chip(context).apply {
            id = View.generateViewId()
            text = category.name
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_background_selector)
            setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_selector))
            chipIcon = ContextCompat.getDrawable(context, getIconForCategory(category.name))
            isChipIconVisible = true
            iconStartPadding = 8f
            textStartPadding = 4f
            chipMinHeight = resources.getDimension(R.dimen.chip_min_height)
            isCheckable = true
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(resources.getDimension(R.dimen.chip_corner_radius))
                .build()
        }
        chipIdToCategoryMap[chip.id] = category
        binding.categoryChipGroup.addView(chip)
    }

    private fun getIconForCategory(categoryName: String): Int {
        return when (categoryName.lowercase(Locale.ROOT)) {
            "food" -> R.drawable.ic_food
            "transport", "transportation" -> R.drawable.ic_transport
            "rent", "housing" -> R.drawable.ic_rent
            else -> R.drawable.ic_default_category
        }
    }

    private fun ensurePermissionsForReceipt() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
} 