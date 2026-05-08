package com.example.tectest.ui.notes

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
// import androidx.navigation.ui.NavigationUI
// import androidx.navigation.ui.setupWithNavController

import com.example.tectest.R
import com.example.tectest.databinding.FragmentAddEditNoteBinding
import com.example.tectest.viewmodel.AddEditNoteUIState
import com.example.tectest.viewmodel.AddEditNoteViewModel

class AddEditNoteFragment : Fragment() {

    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddEditNoteViewModel
    private val args: AddEditNoteFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[AddEditNoteViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeViewModel()

        Log.d("AddEditNoteFragment", "FolderId: ${viewModel.folderId}, NoteId: ${args.noteId}, IsEditMode: ${viewModel.isEditMode}")
    }

    private fun setupToolbar() {
        binding.toolbarAddEditNote.title = args.noteTitleArg
        binding.toolbarAddEditNote.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbarAddEditNote.inflateMenu(R.menu.menu_add_edit_note)
        binding.toolbarAddEditNote.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_done_note -> {
                    saveNote()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressBarAddEditNote.isVisible = state is AddEditNoteUIState.Loading

            when (state) {
                is AddEditNoteUIState.Loading -> { /* Handled */ }
                is AddEditNoteUIState.NoteLoaded -> {
                    Log.d("AddEditNoteFragment", "UIState.NoteLoaded: ${state.note.title}")
                    binding.etNoteTitle.setText(state.note.title)
                    binding.etNoteContent.setText(state.note.content)
                    if (viewModel.isEditMode && state.note.title.isNotBlank()) {
                        binding.toolbarAddEditNote.title = state.note.title
                    }
                }
                is AddEditNoteUIState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    if (state.navigateBack) {
                        findNavController().popBackStack()
                    }
                }
                is AddEditNoteUIState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                is AddEditNoteUIState.Idle -> {  /* empty for now */ }
            }
        }

        // Observe currentNoteData for prefilling, especially for edit mode
        viewModel.currentNoteData.observe(viewLifecycleOwner) { note ->
            if (note != null && viewModel.isEditMode) {
                if (binding.etNoteTitle.text.toString() != note.title) {
                    binding.etNoteTitle.setText(note.title)
                }
                if (binding.etNoteContent.text.toString() != note.content) {
                    binding.etNoteContent.setText(note.content)
                }
                // Update toolbar title when editing and note data is available
                if (note.title.isNotBlank()) {
                    binding.toolbarAddEditNote.title = note.title
                }
            } else if (note != null && !viewModel.isEditMode) {
                // binding.etNoteTitle.setText(note.title)
                // binding.etNoteContent.setText(note.content)
            }
        }
    }

    private fun saveNote() {
        val title = binding.etNoteTitle.text.toString().trim()
        val content = binding.etNoteContent.text.toString().trim()
        viewModel.saveNote(title, content)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}