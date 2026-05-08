package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.Note
import com.example.tectest.data.repository.NotesRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class NotesListUIState {
    object Loading : NotesListUIState()
    data class Success(val groupedNotes: Map<String, List<Note>>) : NotesListUIState()
    data class Error(val message: String) : NotesListUIState()
}

class FolderDetailViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val notesRepository = NotesRepository()
    val folderId: String = savedStateHandle.get<String>("folderId") ?: ""
    val folderName: String = savedStateHandle.get<String>("folderName") ?: "Notes"

    private val _notesListState = MutableLiveData<NotesListUIState>()
    val notesListState: LiveData<NotesListUIState> = _notesListState

    // For feedback on actions like adding a note
    private val _noteActionFeedback = MutableLiveData<Result<String>>()
    val noteActionFeedback: LiveData<Result<String>> = _noteActionFeedback

    init {
        if (folderId.isNotBlank()) {
            Log.d("FolderDetailVM", "Initialized for folderId: $folderId, folderName: $folderName")
            loadNotes()
        } else {
            Log.e("FolderDetailVM", "Error: folderId is blank during ViewModel initialization!")
            _notesListState.value = NotesListUIState.Error("Folder ID not provided.")
        }
    }

    fun loadNotes() {
        if (folderId.isBlank()) {
            _notesListState.value = NotesListUIState.Error("Cannot load notes: Folder ID is missing.")
            return
        }
        _notesListState.value = NotesListUIState.Loading
        viewModelScope.launch {
            val notesResult = notesRepository.getNotesForFolder(folderId)
            notesResult.onSuccess { notes ->
                val grouped = groupNotes(notes)
                _notesListState.postValue(NotesListUIState.Success(grouped))
                Log.d("FolderDetailVM", "Notes loaded and grouped successfully for folder $folderId: ${notes.size} total notes.")
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load notes"
                _notesListState.postValue(NotesListUIState.Error(errorMsg))
                Log.e("FolderDetailVM", "Error loading notes for folder $folderId: $errorMsg", exception)
            }
        }
    }

    private fun groupNotes(notes: List<Note>): Map<String, List<Note>> {
        val groupedMap = mutableMapOf<String, MutableList<Note>>()
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val last30DaysCutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time

        // Sort notes by createdAt descending to process recent first
        val sortedNotes = notes.sortedByDescending { it.createdAt }

        for (note in sortedNotes) {
            val noteDate = note.createdAt?.toDate() ?: Date(0)

            val groupKey: String = if (noteDate.after(last30DaysCutoff)) {
                "Last 30 Days"
            } else {
                monthYearFormat.format(noteDate)
            }
            groupedMap.getOrPut(groupKey) { mutableListOf() }.add(note)
        }

        // Ensure "Last 30 Days" group appears first if it exists, then by month descending
        return groupedMap.entries.sortedWith(compareBy<Map.Entry<String, List<Note>>> {
            it.key != "Last 30 Days"
        }.thenComparator { a, b ->
            if (a.key == "Last 30 Days" || b.key == "Last 30 Days") 0
            else {
                try {
                    val dateA = monthYearFormat.parse(a.key)
                    val dateB = monthYearFormat.parse(b.key)
                    dateB?.compareTo(dateA) ?: 0
                } catch (e: Exception) {
                    a.key.compareTo(b.key)
                }
            }
        }).associate { it.key to it.value }
    }

    fun addNoteToCurrentFolder(title: String, content: String) {
        if (folderId.isBlank()) {
            _noteActionFeedback.postValue(Result.failure(Exception("Cannot add note: Folder ID is missing.")))
            return
        }
        if (title.isBlank()) {
            _noteActionFeedback.postValue(Result.failure(IllegalArgumentException("Note title cannot be empty.")))
            return
        }

        val newNote = Note(
            title = title,
            content = content,
            folderId = folderId
        )

        viewModelScope.launch {
            val result = notesRepository.addNote(folderId, newNote)
            result.onSuccess { noteId ->
                _noteActionFeedback.postValue(Result.success("Note '$title' added!"))
                Log.d("FolderDetailVM", "Note '$title' (ID: $noteId) added to folder $folderId. Reloading notes.")
                loadNotes()
            }.onFailure { exception ->
                _noteActionFeedback.postValue(Result.failure(exception))
                Log.e("FolderDetailVM", "Failed to add note '$title'", exception)
            }
        }
    }

    fun refreshNotesForCurrentFolder() {
        if (folderId.isNotBlank()) {
            Log.d("FolderDetailVM", "Explicitly refreshing notes for folderId: $folderId")
            loadNotes()
        } else {
            Log.w("FolderDetailVM", "Cannot refresh notes, folderId is blank.")
        }
    }

    fun clearNoteActionFeedback() {
        // (_noteActionFeedback as MutableLiveData<Result<String>?>).value = null // If nullable
    }
}