package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.Task
import com.example.tectest.data.repository.TaskRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

sealed class TaskUIState {
    object Loading : TaskUIState()
    data class Success(val tasks: List<Task>) : TaskUIState()
    data class Error(val message: String) : TaskUIState()
    object Idle : TaskUIState()
}

class TaskViewModel : ViewModel() {

    private val taskRepository = TaskRepository()

    private val _taskUIState = MutableLiveData<TaskUIState>()
    val taskUIState: LiveData<TaskUIState> = _taskUIState

    private val _taskActionResult = MutableLiveData<Result<String>>()
    val taskActionResult: LiveData<Result<String>> = _taskActionResult

    val selectedCategory = MutableLiveData<String>("All")

    init {
        loadTasks()
    }

    fun loadTasks() {
        _taskUIState.value = TaskUIState.Loading
        viewModelScope.launch {
            val categoryForFilter = if (selectedCategory.value?.lowercase() == "all") null else selectedCategory.value

            Log.d("TaskViewModel", "loadTasks called. Category from LiveData: '${selectedCategory.value}', Filter being sent: '$categoryForFilter'")

            val result = taskRepository.getTasks(
                categoryFilter = categoryForFilter,
                exactDueDateFilter = null
            )

            result.onSuccess { tasks ->
                _taskUIState.postValue(TaskUIState.Success(tasks))
                Log.d("TaskViewModel", "Loaded ${tasks.size} tasks for category: $categoryForFilter")
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load tasks"
                _taskUIState.postValue(TaskUIState.Error(errorMsg))
                Log.e("TaskViewModel", "Error loading tasks: $errorMsg", exception)
            }
        }
    }

    fun addTask(title: String, categoryToSave: String, dueDate: Date) {
        if (title.isBlank()) {
            _taskActionResult.postValue(Result.failure(IllegalArgumentException("Task title cannot be empty.")))
            return
        }

        val task = Task(
            title = title,
            category = categoryToSave,
            dueDate = Timestamp(dueDate)
        )
        Log.d("TaskViewModel", "Attempting to add task: Title='${task.title}', Category='${task.category}', Due='${task.dueDate.toDate()}'")

        viewModelScope.launch {
            val result = taskRepository.addTask(task)
            result.onSuccess { taskId ->
                _taskActionResult.postValue(Result.success("Task '$title' added!"))
                Log.d("TaskViewModel", "Task added, reloading tasks for TaskFragment.")
                loadTasks()
            }.onFailure { exception ->
                _taskActionResult.postValue(Result.failure(exception))}
        }
    }

    fun updateTaskCompletion(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = isCompleted)
            val result = taskRepository.updateTask(updatedTask)
            result.onSuccess {
                _taskActionResult.postValue(Result.success("Task '${task.title}' status updated."))
                loadTasks()
            }.onFailure { exception ->
                _taskActionResult.postValue(Result.failure(exception))
            }
        }
    }

    fun editTask(updatedTaskData: Task) {
        if (updatedTaskData.title.isBlank()) {
            _taskActionResult.postValue(Result.failure(IllegalArgumentException("Task title cannot be empty.")))
            return
        }
        viewModelScope.launch {
            val result = taskRepository.updateTask(updatedTaskData)
            result.onSuccess {
                _taskActionResult.postValue(Result.success("Task '${updatedTaskData.title}' updated!"))
                loadTasks()
            }.onFailure { exception ->
                _taskActionResult.postValue(Result.failure(exception))
            }
        }
    }

    fun deleteTask(taskId: String, taskTitle: String) {
        Log.d("TaskViewModel", "Attempting to delete task: ID=$taskId, Title='$taskTitle'")
        viewModelScope.launch {
            val result = taskRepository.deleteTask(taskId)
            result.onSuccess {
                _taskActionResult.postValue(Result.success("Task '$taskTitle' deleted."))
                Log.d("TaskViewModel", "Task $taskId deleted successfully from repo, reloading tasks.")
                loadTasks() // Refresh the list
            }.onFailure { exception ->
                val errorMsg = "Failed to delete task '$taskTitle': ${exception.localizedMessage}"
                _taskActionResult.postValue(Result.failure(Exception(errorMsg)))
                Log.e("TaskViewModel", errorMsg, exception)
                loadTasks()
            }
        }
    }
}