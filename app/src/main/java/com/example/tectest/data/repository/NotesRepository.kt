package com.example.tectest.data.repository

import android.util.Log
import com.example.tectest.data.model.Folder
import com.example.tectest.data.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class NotesRepository {

    // Fire up the firestore
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Get data needed
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun getUserFoldersCollection() = getCurrentUserId()?.let {
        firestore.collection("users").document(it).collection("folders")
    }

    private fun getUserNotesCollection(folderId: String) = getCurrentUserId()?.let {
        firestore.collection("users").document(it).collection("folders").document(folderId).collection("notes")
    }

    fun getCurrentUserIdForNote(): String {
        return auth.currentUser?.uid ?: ""
    }

    suspend fun getNoteById(folderId: String, noteId: String): Result<Note?> {
        val collection = getUserNotesCollection(folderId)
            ?: return Result.failure(IllegalStateException("User not logged in or invalid folder for getNoteById."))
        if (noteId.isBlank()) return Result.failure(IllegalArgumentException("Note ID is blank"))

        return try {
            val documentSnapshot = collection.document(noteId).get().await()
            if (documentSnapshot.exists()) {
                val note = documentSnapshot.toObject(Note::class.java)?.copy(id = documentSnapshot.id)
                Result.success(note)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error fetching note $noteId from folder $folderId", e)
            Result.failure(e)
        }
    }

    // === Folder Methods ===

    suspend fun addFolder(folderName: String): Result<String> {
        val collection = getUserFoldersCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        val normalizedName = folderName.trim()
        if (normalizedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Folder name cannot be empty."))
        }

        // check for uniqueness
        val existingCheck = collection.whereEqualTo("name", normalizedName).limit(1).get().await()
        if (!existingCheck.isEmpty) {
            return Result.failure(IllegalStateException("Folder '$normalizedName' already exists."))
        }

        val newFolder = Folder(name = normalizedName, userId = getCurrentUserId()!!)
        return try {
            val docRef = collection.add(newFolder).await()
            Log.d("NotesRepository", "Folder added with ID: ${docRef.id} - $normalizedName")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error adding folder '$normalizedName'", e)
            Result.failure(e)
        }
    }

    suspend fun getFolders(): Result<List<Folder>> {
        val collection = getUserFoldersCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        return try {
            val snapshot = collection.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            val folders = snapshot.documents.mapNotNull {
                it.toObject(Folder::class.java)?.copy(id = it.id)
            }
            Log.d("NotesRepository", "Fetched ${folders.size} folders.")
            Result.success(folders)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error fetching folders", e)
            Result.failure(e)
        }
    }

    // === Note Methods ===

    suspend fun addNote(folderId: String, note: Note): Result<String> {
        val collection = getUserNotesCollection(folderId)
            ?: return Result.failure(IllegalStateException("User not logged in or invalid folder."))
        // make sure note has correct userId and folderId
        val noteToSave = note.copy(userId = getCurrentUserId()!!, folderId = folderId, lastModified = Timestamp.now())
        return try {
            val docRef = collection.add(noteToSave).await()
            Log.d("NotesRepository", "Note added with ID: ${docRef.id} in folder $folderId")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error adding note in folder $folderId", e)
            Result.failure(e)
        }
    }

    suspend fun getNotesForFolder(folderId: String): Result<List<Note>> {
        val collection = getUserNotesCollection(folderId)
            ?: return Result.failure(IllegalStateException("User not logged in or invalid folder."))
        return try {
            // ordered by createdAt descending. grouping -> ViewModel/UI.
            val snapshot = collection.orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            val notes = snapshot.documents.mapNotNull {
                it.toObject(Note::class.java)?.copy(id = it.id)
            }
            Log.d("NotesRepository", "Fetched ${notes.size} notes for folder $folderId.")
            Result.success(notes)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error fetching notes for folder $folderId", e)
            Result.failure(e)
        }
    }

    suspend fun updateNote(note: Note): Result<Unit> {
        if (note.folderId.isBlank() || note.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Folder ID or Note ID is missing for update."))
        }
        val collection = getUserNotesCollection(note.folderId)
            ?: return Result.failure(IllegalStateException("User not logged in or invalid folder."))

        // make sure note has correct userId and update lastModified
        val noteToSave = note.copy(userId = getCurrentUserId()!!, lastModified = Timestamp.now())
        return try {
            collection.document(note.id).set(noteToSave, SetOptions.merge()).await()
            Log.d("NotesRepository", "Note updated: ${note.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error updating note ${note.id}", e)
            Result.failure(e)
        }
    }

//    suspend fun deleteNote(folderId: String, noteId: String): Result<Unit> {
//        if (folderId.isBlank() || noteId.isBlank()) {
//            return Result.failure(IllegalArgumentException("Folder ID or Note ID is missing for delete."))
//        }
//        val collection = getUserNotesCollection(folderId)
//            ?: return Result.failure(IllegalStateException("User not logged in or invalid folder."))
//        return try {
//            collection.document(noteId).delete().await()
//            Log.d("NotesRepository", "Note deleted: $noteId from folder $folderId")
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Log.e("NotesRepository", "Error deleting note $noteId", e)
//            Result.failure(e)
//        }
//    }

    suspend fun getRecentNotesForHome(limit: Int = 5): Result<List<Note>> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in."))
        return try {
            // query across all 'notes' subcollections using a collection group query
            val snapshot = firestore.collectionGroup("notes")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val notes = snapshot.documents.mapNotNull { document ->
                document.toObject(Note::class.java)?.copy(id = document.id)
            }
            Log.d("NotesRepository", "Fetched ${notes.size} recent notes for home.")
            Result.success(notes)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error fetching recent notes for home", e)
            Result.failure(e)
        }
    }
}