package com.example.tectest.ui.notes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tectest.R
import com.example.tectest.data.model.Note
import com.example.tectest.databinding.FragmentFolderDetailBinding
import com.example.tectest.ui.notes.NoteDetailAdapter
import com.example.tectest.ui.notes.NoteDetailListItem
import com.example.tectest.viewmodel.FolderDetailViewModel
import com.example.tectest.viewmodel.NotesListUIState

class FolderDetailFragment : Fragment() {

    private var _binding: FragmentFolderDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FolderDetailViewModel
    private val args: FolderDetailFragmentArgs by navArgs()
    private lateinit var noteDetailAdapter: NoteDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderDetailBinding.inflate(inflater, container, false)
        // ViewModelProvider will use SavedStateHandle from fragment's arguments by default
        viewModel = ViewModelProvider(this)[FolderDetailViewModel::class.java]
        Log.d("FolderDetailFragment", "onCreateView. ViewModel Initialized. FolderId: ${viewModel.folderId}, Name: ${viewModel.folderName}")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FolderDetailFragment", "onViewCreated. Displaying folder: ${viewModel.folderName}")

        // Setup Toolbar
        binding.toolbarFolderDetail.title = viewModel.folderName
        binding.toolbarFolderDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        noteDetailAdapter = NoteDetailAdapter { clickedNoteEntry ->
            // When a note item is clicked, navigate to edit it
            Log.d("FolderDetailFragment", "Note clicked: ${clickedNoteEntry.title}, ID: ${clickedNoteEntry.id}")
            if (viewModel.folderId.isNotBlank() && clickedNoteEntry.id.isNotBlank()) {
                val action = FolderDetailFragmentDirections.actionFolderDetailFragmentToAddEditNoteFragment(
                    folderId = viewModel.folderId,
                    noteId = clickedNoteEntry.id, // Pass noteId for editing
                    noteTitleArg = clickedNoteEntry.title // Pass current title for appbar
                )
                findNavController().navigate(action)
            } else {
                Toast.makeText(requireContext(), "Error: Missing note or folder information.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerViewNotesInFolder.apply {
            adapter = noteDetailAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabAddNoteToFolder.setOnClickListener {
            Log.d("FolderDetailFragment", "FAB add note clicked for folder: ${viewModel.folderName}")
            if (viewModel.folderId.isNotBlank()) {
                val action = FolderDetailFragmentDirections.actionFolderDetailFragmentToAddEditNoteFragment(
                    folderId = viewModel.folderId,
                    noteId = null, // Null noteId signifies a new note
                    noteTitleArg = "New Note" // Default title for new note
                )
                findNavController().navigate(action)
            } else {
                Toast.makeText(requireContext(), "Error: Cannot determine folder to add note to.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.notesListState.observe(viewLifecycleOwner) { state ->
            binding.progressBarNotesInFolder.isVisible = state is NotesListUIState.Loading
            binding.recyclerViewNotesInFolder.isVisible = state is NotesListUIState.Success
            binding.textViewNoNotesInFolder.isVisible = false // Reset

            when (state) {
                is NotesListUIState.Loading -> {
                    Log.d("FolderDetailFragment", "Notes List State: Loading")
                }
                is NotesListUIState.Success -> {
                    Log.d("FolderDetailFragment", "Notes List State: Success - Grouped notes count: ${state.groupedNotes.size} groups")
                    val listItems = mutableListOf<NoteDetailListItem>()
                    if (state.groupedNotes.isEmpty()) {
                        binding.textViewNoNotesInFolder.text = "This folder is empty. Add a note!"
                        binding.textViewNoNotesInFolder.isVisible = true
                    } else {
                        state.groupedNotes.forEach { (groupTitle, notesInGroup) ->
                            listItems.add(NoteDetailListItem.NoteHeader(groupTitle))
                            notesInGroup.forEach { note ->
                                listItems.add(NoteDetailListItem.NoteEntry(note))
                            }
                        }
                    }
                    noteDetailAdapter.submitList(listItems)
                    Log.d("FolderDetailFragment", "Submitted ${listItems.size} items to NoteDetailAdapter.")
                }
                is NotesListUIState.Error -> {
                    Log.e("FolderDetailFragment", "Notes List State: Error - ${state.message}")
                    binding.textViewNoNotesInFolder.text = state.message
                    binding.textViewNoNotesInFolder.isVisible = true
                    binding.recyclerViewNotesInFolder.isVisible = false
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // observer for feedback after adding/editing a note
        viewModel.noteActionFeedback.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    // Toast.makeText(requireContext(), "FolderDetail: ${message}", Toast.LENGTH_SHORT).show()
                    Log.i("FolderDetailFragment", "Note action feedback: $message")
                }
                it.onFailure { exception ->
                    Log.e("FolderDetailFragment", "Note action error feedback: ${exception.localizedMessage}")
                }
                // viewModel.clearNoteActionFeedback()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // refresh notes when the fragment is resumed
        Log.d("FolderDetailFragment", "onResume: Reloading notes for folder ${viewModel.folderName}")
        viewModel.loadNotes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewNotesInFolder.adapter = null
        _binding = null
    }
}