package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.Note
import com.example.tectest.data.repository.NotesRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

sealed class AddEditNoteUIState {
    object Idle : AddEditNoteUIState()
    object Loading : AddEditNoteUIState()
    data class Success(val message: String, val navigateBack: Boolean = false) : AddEditNoteUIState()
    data class Error(val message: String) : AddEditNoteUIState()
    data class NoteLoaded(val note: Note) : AddEditNoteUIState()
}

class AddEditNoteViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val notesRepository = NotesRepository()
    // get folderId and noteId from SavedStateHandle (passed via Navigation Args)
    val folderId: String = savedStateHandle.get<String>("folderId") ?: ""
    private val noteId: String? = savedStateHandle.get<String>("noteId")

    val isEditMode: Boolean = !noteId.isNullOrBlank()

    private val _uiState = MutableLiveData<AddEditNoteUIState>()
    val uiState: LiveData<AddEditNoteUIState> = _uiState

    // holds the current note data being edited or prepared for creation
    private val _currentNoteData = MutableLiveData<Note>() // This will hold the full Note object
    val currentNoteData: LiveData<Note> = _currentNoteData

    init {
        Log.d("AddEditNoteVM", "Init. EditMode: $isEditMode, NoteId: $noteId, FolderId: $folderId")
        if (isEditMode && !noteId.isNullOrBlank()) {
            loadNoteForEditing(noteId)
        } else if (folderId.isNotBlank()) {
            _currentNoteData.value = Note(
                folderId = folderId,
                userId = notesRepository.getCurrentUserIdForNote()
            )
            _uiState.value = AddEditNoteUIState.NoteLoaded(_currentNoteData.value!!)
        } else {
            Log.e("AddEditNoteVM", "Critical error: FolderId is blank for a new note.")
            _uiState.value = AddEditNoteUIState.Error("Folder information missing.")
        }
    }

    private fun loadNoteForEditing(id: String) {
        if (folderId.isBlank()) {
            _uiState.value = AddEditNoteUIState.Error("Folder ID missing for editing.")
            return
        }
        _uiState.value = AddEditNoteUIState.Loading
        viewModelScope.launch {
            val result = notesRepository.getNoteById(folderId, id)
            result.onSuccess { note ->
                if (note != null) {
                    _currentNoteData.postValue(note)
                    _uiState.postValue(AddEditNoteUIState.NoteLoaded(note))
                    Log.d("AddEditNoteVM", "Note loaded for editing: ${note.title}")
                } else {
                    _uiState.postValue(AddEditNoteUIState.Error("Note not found."))
                    Log.w("AddEditNoteVM", "Note with ID $id not found in folder $folderId")
                }
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load note for editing."
                _uiState.postValue(AddEditNoteUIState.Error(errorMsg))
                Log.e("AddEditNoteVM", "Error loading note $id: $errorMsg", exception)
            }
        }
    }

    fun saveNote(title: String, content: String) {
        if (title.isBlank()) {
            _uiState.value = AddEditNoteUIState.Error("Title cannot be empty.")
            return
        }
        if (content.isBlank()) {
            _uiState.value = AddEditNoteUIState.Error("Content cannot be empty.")
            return
        }
        if (folderId.isBlank()) {
            _uiState.value = AddEditNoteUIState.Error("Folder information is missing. Cannot save.")
            Log.e("AddEditNoteVM", "Attempted to save note but folderId is blank.")
            return
        }

        _uiState.value = AddEditNoteUIState.Loading

        val noteToSave = (_currentNoteData.value ?: Note()).copy(
            title = title,
            content = content,
            folderId = folderId,
            userId = notesRepository.getCurrentUserIdForNote(),
            lastModified = Timestamp.now()
        )

        Log.d("AddEditNoteVM", "Preparing to save note. IsEditMode: $isEditMode, Note ID: ${noteToSave.id}")

        viewModelScope.launch {
            val result: Result<Any> = if (isEditMode && noteToSave.id.isNotBlank()) {
                Log.d("AddEditNoteVM", "Calling updateNote for ID: ${noteToSave.id}")
                notesRepository.updateNote(noteToSave)
            } else {
                Log.d("AddEditNoteVM", "Calling addNote for folder: $folderId")
                notesRepository.addNote(folderId, noteToSave)
            }

            result.onSuccess {
                val successMessage = if (isEditMode) "Note updated!" else "Note created!"
                _uiState.postValue(AddEditNoteUIState.Success(successMessage, navigateBack = true))
                Log.i("AddEditNoteVM", "$successMessage (Result: $it)")
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to save note."
                _uiState.postValue(AddEditNoteUIState.Error(errorMsg))
                Log.e("AddEditNoteVM", "Error saving note: $errorMsg", exception)
            }
        }
    }
}