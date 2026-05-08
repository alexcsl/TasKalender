package com.example.tectest.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.tectest.databinding.BottomSheetAddFolderBinding
import com.example.tectest.viewmodel.NotesViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.util.Log

class AddFolderBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddFolderBinding? = null
    private val binding get() = _binding!!

    private val notesViewModel: NotesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveNewFolderSheet.setOnClickListener {
            val folderName = binding.etNewFolderNameSheet.text.toString().trim()
            if (folderName.isEmpty()) {
                binding.tilNewFolderNameSheet.error = "Folder name cannot be empty"
            } else {
                binding.tilNewFolderNameSheet.error = null
                notesViewModel.addFolder(folderName)
            }
        }

        // feedback to dismiss on success or show error from ViewModel
        notesViewModel.folderActionFeedback.observe(viewLifecycleOwner) { result ->
            if (result == null || _binding == null) return@observe

            Log.d(TAG, "AddFolderSheet received actionFeedback: $result")

            result.onSuccess { message ->
                if (message.contains("Folder", ignoreCase = true) && message.contains("created!", ignoreCase = true)) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                notesViewModel.clearFolderActionFeedback()
            }.onFailure { exception ->
                val errorMessage = exception.localizedMessage ?: "Failed to create folder"
                if (binding.tilNewFolderNameSheet != null) {
                    binding.tilNewFolderNameSheet.error = errorMessage
                } else {
                    Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
                Log.e(TAG, "AddFolderSheet error: $errorMessage", exception)
                notesViewModel.clearFolderActionFeedback()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddFolderBottomSheet"
        fun newInstance(): AddFolderBottomSheetDialogFragment {
            return AddFolderBottomSheetDialogFragment()
        }
    }
}