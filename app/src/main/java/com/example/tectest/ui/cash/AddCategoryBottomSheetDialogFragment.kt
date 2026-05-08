package com.example.tectest.ui.cash

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.example.tectest.R
import com.example.tectest.databinding.BottomSheetAddCategoryBinding
import com.example.tectest.viewmodel.CashViewModel
import com.example.tectest.viewmodel.CategoriesUIState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddCategoryBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddCategoryBinding? = null
    private val binding get() = _binding!!

    private val cashViewModel: CashViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated for AddCategoryBottomSheet")

        binding.btnSaveNewCategory.setOnClickListener {
            val categoryName = binding.etNewCategoryName.text.toString().trim()

            var isValid = true
            if (categoryName.isEmpty()) {
                binding.tilNewCategoryName.error = "Category name cannot be empty"
                isValid = false
            } else {
                binding.tilNewCategoryName.error = null
            }

            // Client-side check for existing category
            val existingCategoriesState = cashViewModel.categoriesState.value
            if (isValid && existingCategoriesState is CategoriesUIState.Success) {
                if (existingCategoriesState.categories.any { it.name.equals(categoryName, ignoreCase = true) }) {
                    binding.tilNewCategoryName.error = "Category '$categoryName' already exists"
                    isValid = false
                }
            }

            if (isValid) {
                cashViewModel.addTransactionCategory(categoryName)
            }
        }

        // Observer for feedback from addTransactionCategory call
        cashViewModel.actionFeedback.observe(viewLifecycleOwner) { result ->
            if (result == null || _binding == null) {
                return@observe
            }

            Log.d(TAG, "AddCategoryDialog received actionFeedback: $result")

            var consumed = false
            result.onSuccess { message ->
                if (message.contains("Category", ignoreCase = true) && message.contains("added!", ignoreCase = true)) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    dismiss()
                    consumed = true
                }
            }
            result.onFailure { exception ->
                if (binding.tilNewCategoryName != null) { // Check specific binding view
                    if (exception.message?.contains("already exists", ignoreCase = true) == true) {
                        binding.tilNewCategoryName.error = exception.message
                    } else {
                        Toast.makeText(requireContext(), "Error: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                consumed = true // Also consume errors to prevent reshowing toast by other observers
            }

            if (consumed) {
                cashViewModel.clearActionFeedback()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddCategoryBottomSheet"
        fun newInstance(): AddCategoryBottomSheetDialogFragment {
            return AddCategoryBottomSheetDialogFragment()
        }
    }
}