package com.example.tectest.ui.cash

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.tectest.R
import com.example.tectest.data.model.TransactionType
import com.example.tectest.databinding.BottomSheetAddTransactionBinding
import com.example.tectest.ui.cash.AddCategoryBottomSheetDialogFragment
import com.example.tectest.viewmodel.CashViewModel
import com.example.tectest.viewmodel.CategoriesUIState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val cashViewModel: CashViewModel by activityViewModels()

    private var selectedTransactionType: TransactionType = TransactionType.EXPENSE
    private var selectedDate: Calendar = Calendar.getInstance()
    private val uiDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    private lateinit var categoryAdapter: ArrayAdapter<String>
    private var categoriesListFromVM = mutableListOf<String>("Other") // default

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated for AddTransactionBottomSheet")

        setupTabs()
        setupDatePicking()
        setupCategoryDropdown()
        setupAddCategoryButton()
        setupSaveButton()
        observeViewModelUpdates()
    }

    private fun setupTabs() {
        binding.tabLayoutTransactionType.addTab(binding.tabLayoutTransactionType.newTab().setText("Expense"))
        binding.tabLayoutTransactionType.addTab(binding.tabLayoutTransactionType.newTab().setText("Income"))

        binding.tabLayoutTransactionType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTransactionType = if (tab?.position == 0) TransactionType.EXPENSE else TransactionType.INCOME
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        binding.tabLayoutTransactionType.getTabAt(0)?.select() // Default to Expense
    }

    private fun setupDatePicking() {
        binding.tvTransactionDate.text = uiDateFormat.format(selectedDate.time)
        binding.tvTransactionDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.time = selectedDate.time
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                binding.tvTransactionDate.text = uiDateFormat.format(selectedDate.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show()
        }
    }

    private fun setupCategoryDropdown() {
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoriesListFromVM)
        (binding.menuTransactionCategory.editText as? AutoCompleteTextView)?.setAdapter(categoryAdapter)

        val defaultCategory = "Other"
        if (categoriesListFromVM.contains(defaultCategory)) {
            (binding.menuTransactionCategory.editText as? AutoCompleteTextView)?.setText(defaultCategory, false)
        } else if (categoriesListFromVM.isNotEmpty()) {
            (binding.menuTransactionCategory.editText as? AutoCompleteTextView)?.setText(categoriesListFromVM[0], false)
        }
    }

    private fun setupAddCategoryButton() {
        binding.buttonAddNewCategory.setOnClickListener {
            val addCategorySheet = AddCategoryBottomSheetDialogFragment.newInstance()
            addCategorySheet.show(childFragmentManager, AddCategoryBottomSheetDialogFragment.TAG)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            val title = binding.etTransactionTitle.text.toString().trim()
            val amountStr = binding.etTransactionAmount.text.toString().trim()
            val category = (binding.menuTransactionCategory.editText as? AutoCompleteTextView)?.text.toString()

            var isValid = true
            if (title.isEmpty()) {
                binding.tilTransactionTitle.error = "Title is required"; isValid = false
            } else { binding.tilTransactionTitle.error = null }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.tilTransactionAmount.error = "Valid positive amount is required"; isValid = false
            } else { binding.tilTransactionAmount.error = null }

            if (category.isEmpty() || !categoriesListFromVM.contains(category)) {
                binding.menuTransactionCategory.error = "Please select a valid category"
                isValid = false
            } else { binding.menuTransactionCategory.error = null }


            if (isValid && amount != null) {
                cashViewModel.addTransaction(title, amount, selectedTransactionType, selectedDate.time, category)
            }
        }
    }

    private fun observeViewModelUpdates() {
        // observer for categories to update the dropdown
        cashViewModel.categoriesState.observe(viewLifecycleOwner) { state ->
            if (state is CategoriesUIState.Success) {
                Log.d(TAG, "Observed new categories: ${state.categories.map { it.name }}")
                categoriesListFromVM.clear()
                categoriesListFromVM.addAll(state.categories.map { it.name })
                if (!categoriesListFromVM.contains("Other")) {
                    categoriesListFromVM.add(0, "Other")
                }
                categoryAdapter.notifyDataSetChanged()

                // stay in current selection or set default if current not in new list
                val currentDropdownText = (binding.menuTransactionCategory.editText as? AutoCompleteTextView)?.text.toString()
                if (!categoriesListFromVM.contains(currentDropdownText)) {
                    val defaultCat = if (categoriesListFromVM.contains("Other")) "Other" else categoriesListFromVM.firstOrNull() ?: ""
                    (binding.menuTransactionCategory.editText as? AutoCompleteTextView)?.setText(defaultCat, false)
                }

            } else if (state is CategoriesUIState.Error) {
                Toast.makeText(requireContext(), "Error loading categories: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // observer for feedback from addTransaction
        cashViewModel.actionFeedback.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe

            result.onSuccess { message ->
                // Make toast if successful.
                if (message.contains("Transaction", ignoreCase = true) && message.contains("added", ignoreCase = true)) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Transaction added successfully. Dismissing AddTransactionSheet.")
                    dismiss()
                }
                cashViewModel.clearActionFeedback()
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Operation Failed: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Action failed feedback: ${exception.localizedMessage}", exception)
                cashViewModel.clearActionFeedback()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTransactionBottomSheet"
        fun newInstance(): AddTransactionBottomSheetDialogFragment {
            return AddTransactionBottomSheetDialogFragment()
        }
    }
}