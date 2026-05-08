package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.Task
import com.example.tectest.data.repository.TaskRepository
import com.example.tectest.data.model.Note
import com.example.tectest.data.repository.NotesRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar

sealed class HomeTasksUIState {
    object Loading : HomeTasksUIState()
    data class Success(val tasks: List<Task>) : HomeTasksUIState()
    data class Error(val message: String) : HomeTasksUIState()
}

sealed class HomeNotesUIState {
    object Loading : HomeNotesUIState()
    data class Success(val notes: List<Note>) : HomeNotesUIState()
    data class Error(val message: String) : HomeNotesUIState()
}

class HomeViewModel : ViewModel() {

    private val taskRepository = TaskRepository()
    private val notesRepository = NotesRepository()

    // tasks shown on
    private val _homeTasksState = MutableLiveData<HomeTasksUIState>()
    val homeTasksState: LiveData<HomeTasksUIState> = _homeTasksState

    // recent notes shown
    private val _recentNotesState = MutableLiveData<HomeNotesUIState>()
    val recentNotesState: LiveData<HomeNotesUIState> = _recentNotesState

    private val _actionFeedback = MutableLiveData<Result<String>>()
    val actionFeedback: LiveData<Result<String>> = _actionFeedback

    // currently selected date from the CalendarView
    val selectedCalendarDate = MutableLiveData<Timestamp>(getStartOfDayTimestamp(Date()))

    init {
        selectedCalendarDate.value?.let { loadDataForDate(it) }
    }

    fun onDateSelected(dateInMillis: Long) {
        val newSelectedTimestamp = getStartOfDayTimestamp(Date(dateInMillis))
        Log.d("HomeViewModel", "onDateSelected called. New Timestamp: ${newSelectedTimestamp.toDate()}")
        // reload if the date actually changed
        if (selectedCalendarDate.value?.toDate()?.toInstant()?.truncatedTo(java.time.temporal.ChronoUnit.DAYS) != newSelectedTimestamp.toDate().toInstant().truncatedTo(java.time.temporal.ChronoUnit.DAYS)) {
            Log.d("HomeViewModel", "Date HAS changed. Calling loadDataForDate.")
            loadDataForDate(newSelectedTimestamp)
        } else {
            Log.d("HomeViewModel", "Date has NOT changed (same day). Not reloading.")
        }
    }

    // Main function to call when date changes or on init
    private fun loadDataForDate(date: Timestamp) {
        selectedCalendarDate.value = date // Update LiveData
        loadTasksForSelectedDate(date)
        loadRecentNotes()
    }

    private fun loadTasksForSelectedDate(date: Timestamp) {
        _homeTasksState.value = HomeTasksUIState.Loading
        viewModelScope.launch {
            Log.d("HomeViewModel", "Loading tasks for date: ${date.toDate()}")
            val result = taskRepository.getUnfinishedTasksForDate(date)
            result.onSuccess { tasks ->
                _homeTasksState.postValue(HomeTasksUIState.Success(tasks))
                Log.d("HomeViewModel", "Loaded ${tasks.size} unfinished tasks for selected date.")
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load tasks"
                _homeTasksState.postValue(HomeTasksUIState.Error(errorMsg))
                Log.e("HomeViewModel", "Error loading tasks for date: $errorMsg", exception)
            }
        }
    }

    fun updateTaskCompletionStateOnHome(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = isCompleted)
            val result = taskRepository.updateTask(updatedTask)
            result.onSuccess {
                _actionFeedback.postValue(Result.success("Task '${task.title}' marked as ${if(isCompleted) "complete" else "incomplete"}."))
                selectedCalendarDate.value?.let { loadTasksForSelectedDate(it) }
                Log.d("HomeViewModel", "Task ${task.id} completion updated. Reloading tasks.")
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to update task"
                _actionFeedback.postValue(Result.failure(Exception(errorMsg)))
                Log.e("HomeViewModel", "Error updating task completion: $errorMsg", exception)
            }
        }
    }

    fun loadRecentNotes() {
        _recentNotesState.value = HomeNotesUIState.Loading
        viewModelScope.launch {
            Log.d("HomeViewModel", "Fetching up to 5 recent notes for home screen...")
            val notesResult = notesRepository.getRecentNotesForHome(limit = 5)
            notesResult.onSuccess { notes ->
                _recentNotesState.postValue(HomeNotesUIState.Success(notes))
                Log.d("HomeViewModel", "Successfully loaded ${notes.size} recent notes.")
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load recent notes"
                _recentNotesState.postValue(HomeNotesUIState.Error(errorMsg))
                Log.e("HomeViewModel", "Error loading recent notes: $errorMsg", exception)
            }
        }
    }

    private fun getStartOfDayTimestamp(date: Date): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }
}