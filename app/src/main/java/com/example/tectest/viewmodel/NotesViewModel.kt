package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.Folder
import com.example.tectest.data.repository.NotesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

sealed class FoldersUIState {
    object Loading : FoldersUIState()
    data class Success(val folders: List<FolderWithNoteCount>) : FoldersUIState()
    data class Error(val message: String) : FoldersUIState()
}

data class FolderWithNoteCount(
    val folder: Folder,
    val noteCount: Int
)

class NotesViewModel : ViewModel() {

    private val notesRepository = NotesRepository()

    private val _foldersState = MutableLiveData<FoldersUIState>()
    val foldersState: LiveData<FoldersUIState> = _foldersState

    private val _folderActionFeedback = MutableLiveData<Result<String>?>() // Made nullable for clearable events
    val folderActionFeedback: LiveData<Result<String>?> = _folderActionFeedback

    init {
        loadFolders()
    }

    fun loadFolders() {
        _foldersState.value = FoldersUIState.Loading
        Log.d("NotesViewModel", "loadFolders: Fetching folders...")
        viewModelScope.launch {
            val foldersResult = notesRepository.getFolders()
            foldersResult.onSuccess { folders ->
                Log.d("NotesViewModel", "loadFolders: Successfully fetched ${folders.size} folders. Now fetching note counts...")

                val foldersWithCountsSequential = mutableListOf<FolderWithNoteCount>()
                for (folder in folders) {
                    val notesResult = notesRepository.getNotesForFolder(folder.id)
                    val count = if (notesResult.isSuccess) (notesResult.getOrNull()?.size ?: 0) else 0
                    foldersWithCountsSequential.add(FolderWithNoteCount(folder, count))
                    Log.d("NotesViewModel", "Folder '${folder.name}' has $count notes.")
                }
                _foldersState.postValue(FoldersUIState.Success(foldersWithCountsSequential))
                Log.d("NotesViewModel", "Folders loaded with sequential note counts: ${foldersWithCountsSequential.size}")

            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load folders"
                _foldersState.postValue(FoldersUIState.Error(errorMsg))
                Log.e("NotesViewModel", "Error loading folders: $errorMsg", exception)
            }
        }
    }

    fun addFolder(folderName: String) {
        if (folderName.isBlank()) {
            _folderActionFeedback.value = Result.failure(IllegalArgumentException("Folder name cannot be empty."))
            return
        }
        viewModelScope.launch {
            val result = notesRepository.addFolder(folderName)
            result.onSuccess { folderId ->
                _folderActionFeedback.value = Result.success("Folder '$folderName' created!")
                Log.d("NotesViewModel", "Folder '$folderName' (ID: $folderId) added. Reloading folders.")
                loadFolders()
            }.onFailure { exception ->
                if (exception.message?.contains("already exists", ignoreCase = true) == true) {
                    _folderActionFeedback.value = Result.failure(IllegalStateException("Folder '$folderName' already exists."))
                } else {
                    _folderActionFeedback.value = Result.failure(exception)
                }
                Log.e("NotesViewModel", "Failed to add folder '$folderName'", exception)
            }
        }
    }

    fun clearFolderActionFeedback() {
        _folderActionFeedback.value = null
    }
}