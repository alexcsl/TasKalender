package com.example.tectest.data.repository

import android.util.Log
import com.example.tectest.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class TaskRepository {

    // Fire up the firestore
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Check the firestore if user has tasks
    private fun getUserTasksCollection() = firebaseAuth.currentUser?.uid?.let { userId ->
        firestore.collection("users").document(userId).collection("tasks")
    }

    suspend fun addTask(task: Task): Result<String> {
        val userTasksCollection = getUserTasksCollection()
            ?: return Result.failure(IllegalStateException("User not logged in or UID not available for adding task."))

        // Ensure the task being added has the correct userId
        val taskWithUserId = task.copy(userId = firebaseAuth.currentUser!!.uid) // isCompleted comes from the passed 'task'

        return try {
            val documentReference = userTasksCollection.add(taskWithUserId).await()
            Log.d("TaskRepository", "Task added with ID: ${documentReference.id}. Data: $taskWithUserId") // Log the data
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error adding task", e)
            Result.failure(e)
        }
    }

    suspend fun getTasks(
        categoryFilter: String? = null,
        exactDueDateFilter: Timestamp? = null
    ): Result<List<Task>> {
        val userTasksCollection = getUserTasksCollection()
            ?: return Result.failure(IllegalStateException("User not logged in or UID not available for getting tasks."))
        return try {
            var query: Query = userTasksCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            Log.d("TaskRepository", "getTasks received categoryFilter: '$categoryFilter'")
            if (categoryFilter != null) { // No need to check for "all" here, ViewModel handles that
                query = query.whereEqualTo("category", categoryFilter) // Case-sensitive match
                Log.d("TaskRepository", "Firestore query WILL filter for category: '$categoryFilter'")
            } else {
                Log.d("TaskRepository", "Firestore query will NOT filter by category (all tasks).")
            }
            if (exactDueDateFilter != null) {
                query = query.whereEqualTo("dueDate", exactDueDateFilter)
            }
            val snapshot = query.get().await()
            val tasks = snapshot.documents.mapNotNull { document ->
                document.toObject(Task::class.java)?.copy(id = document.id)
            }
            Log.d("TaskRepository", "Fetched ${tasks.size} tasks. Category: $categoryFilter, Date: $exactDueDateFilter")
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error fetching tasks", e)
            Result.failure(e)
        }
    }

    suspend fun getUnfinishedTasksForDate(selectedDateTimestamp: Timestamp): Result<List<Task>> {
        val userTasksCollection = getUserTasksCollection()
            ?: return Result.failure(IllegalStateException("User not logged in or UID not available."))
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDateTimestamp.toDate()

            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay_Date = calendar.time
            val startOfDay_Timestamp = Timestamp(startOfDay_Date)

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val startOfNextDay_Date = calendar.time
            val startOfNextDay_Timestamp = Timestamp(startOfNextDay_Date)

            Log.d("TaskRepository", "Querying getUnfinishedTasksForDate: >= $startOfDay_Timestamp AND < $startOfNextDay_Timestamp")

            val snapshot = userTasksCollection
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("dueDate", startOfDay_Timestamp)
                .whereLessThan("dueDate", startOfNextDay_Timestamp)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .get()
                .await()

            val tasks = snapshot.documents.mapNotNull { document ->
                document.toObject(Task::class.java)?.copy(id = document.id)
            }
            Log.d("TaskRepository", "Found ${snapshot.size()} documents matching query.")
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error fetching unfinished tasks for date", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        val userTasksCollection = getUserTasksCollection()
            ?: return Result.failure(IllegalStateException("User not logged in or UID not available for updating task."))
        if (task.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Task ID is missing for update."))
        }
        // Ensure the task being updated has the correct userId
        val taskWithUserId = task.copy(userId = firebaseAuth.currentUser!!.uid)

        return try {
            userTasksCollection.document(task.id).set(taskWithUserId, SetOptions.merge()).await()
            Log.d("TaskRepository", "Task updated: ${task.id}. Data: $taskWithUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error updating task ${task.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val userTasksCollection = getUserTasksCollection()
            ?: return Result.failure(IllegalStateException("User not logged in or UID not available for deleting task."))
        if (taskId.isBlank()) {
            return Result.failure(IllegalArgumentException("Task ID is missing for delete."))
        }
        return try {
            userTasksCollection.document(taskId).delete().await()
            Log.d("TaskRepository", "Task deleted successfully: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error deleting task $taskId", e)
            Result.failure(e)
        }
    }


}