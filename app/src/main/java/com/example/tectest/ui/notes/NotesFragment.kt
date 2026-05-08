package com.example.tectest.ui.notes

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tectest.R
import com.example.tectest.databinding.FragmentNotesBinding
import com.example.tectest.ui.notes.FolderAdapter
import com.example.tectest.viewmodel.FoldersUIState
import com.example.tectest.viewmodel.NotesViewModel
import com.google.android.material.textfield.TextInputLayout
import androidx.navigation.fragment.findNavController

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NotesViewModel
    private lateinit var folderAdapter: FolderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        folderAdapter = FolderAdapter { folderWithCount ->
            Log.d("NotesFragment", "Folder clicked: ${folderWithCount.folder.name}, ID: ${folderWithCount.folder.id}")
            if (folderWithCount.folder.id.isNotBlank()) {
                try {
                    val action = NotesFragmentDirections.actionNotesFragmentToFolderDetailFragment(
                        folderId = folderWithCount.folder.id,
                        folderName = folderWithCount.folder.name
                    )
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Log.e("NotesFragment", "Navigation failed for folder click", e)
                    Toast.makeText(requireContext(), "Error navigating to folder details.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Error: Folder ID is missing.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerViewFolders.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabAddFolder.setOnClickListener {
            val addFolderSheet = AddFolderBottomSheetDialogFragment.newInstance()
            addFolderSheet.show(childFragmentManager, AddFolderBottomSheetDialogFragment.TAG)
        }
    }

    private fun observeViewModel() {
        notesViewModel.foldersState.observe(viewLifecycleOwner) { state ->
            binding.progressBarFolders.isVisible = state is FoldersUIState.Loading
            binding.recyclerViewFolders.isVisible = state is FoldersUIState.Success
            binding.textViewNoFolders.isVisible = false

            when (state) {
                is FoldersUIState.Loading -> { /* Handled */ }
                is FoldersUIState.Success -> {
                    folderAdapter.submitList(state.folders)
                    binding.textViewNoFolders.isVisible = state.folders.isEmpty() && !binding.progressBarFolders.isVisible
                }
                is FoldersUIState.Error -> {
                    binding.textViewNoFolders.text = state.message
                    binding.textViewNoFolders.isVisible = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        notesViewModel.folderActionFeedback.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Log.i("NotesFragment", "Folder Action Success: $message")
                }
                it.onFailure { exception ->
                    Log.e("NotesFragment", "Folder Action Error: ${exception.localizedMessage}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewFolders.adapter = null
        _binding = null
    }
}