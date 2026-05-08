package com.example.tectest.data.repository

import android.util.Log
import com.example.tectest.data.model.Transaction
import com.example.tectest.data.model.TransactionCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class TransactionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun getUserTransactionsCollection() = getCurrentUserId()?.let {
        firestore.collection("users").document(it).collection("transactions")
    }

    private fun getUserTransactionCategoriesCollection() = getCurrentUserId()?.let {
        firestore.collection("users").document(it).collection("transactionCategories")
    }

    // == Transaction Methods ==
    suspend fun addTransaction(transaction: Transaction): Result<String> {
        val collection = getUserTransactionsCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        val transactionWithUser = transaction.copy(userId = getCurrentUserId()!!)
        return try {
            val docRef = collection.add(transactionWithUser).await()
            Log.d("TransactionRepo", "Transaction added with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error adding transaction", e)
            Result.failure(e)
        }
    }

    suspend fun getTransactionsForMonth(year: Int, month: Int): Result<List<Transaction>> {
        val collection = getUserTransactionsCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0) // month - 1 because of Java's calendar zero-indexing
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        val startOfNextMonth = Timestamp(calendar.time)

        Log.d("TransactionRepo", "Fetching transactions for $year-$month: >= $startOfMonth, < $startOfNextMonth")

        return try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThan("date", startOfNextMonth)
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val transactions = snapshot.documents.mapNotNull {
                it.toObject(Transaction::class.java)?.copy(id = it.id)
            }
            Log.d("TransactionRepo", "Fetched ${transactions.size} transactions for month.")
            Result.success(transactions)
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error fetching transactions for month", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val collection = getUserTransactionsCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        return try {
            collection.document(transactionId).delete().await()
            Log.d("TransactionRepo", "Transaction deleted: $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error deleting transaction $transactionId", e)
            Result.failure(e)
        }
    }

    // == Transaction Category Methods ==
    suspend fun addTransactionCategory(categoryName: String): Result<String> {
        val collection = getUserTransactionCategoriesCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        val normalizedName = categoryName.trim()
        if (normalizedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty."))
        }

        // Check if category already exists (case-insensitive for user-friendliness, but save consistently)
        val existingCheck = collection.whereEqualTo("name", normalizedName).limit(1).get().await()
        if (!existingCheck.isEmpty) {
            return Result.failure(IllegalStateException("Category '$normalizedName' already exists."))
        }

        val newCategory = TransactionCategory(name = normalizedName, userId = getCurrentUserId()!!)
        return try {
            val docRef = collection.add(newCategory).await()
            Log.d("TransactionRepo", "Transaction category added: ${docRef.id} - $normalizedName")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error adding transaction category $normalizedName", e)
            Result.failure(e)
        }
    }

    suspend fun getTransactionCategories(): Result<List<TransactionCategory>> {
        val collection = getUserTransactionCategoriesCollection()
            ?: return Result.failure(IllegalStateException("User not logged in."))
        return try {
            val snapshot = collection.orderBy("name", Query.Direction.ASCENDING).get().await()
            val categories = snapshot.documents.mapNotNull {
                it.toObject(TransactionCategory::class.java)?.copy(id = it.id)
            }
            Log.d("TransactionRepo", "Fetched ${categories.size} transaction categories.")
            Result.success(categories)
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error fetching transaction categories", e)
            Result.failure(e)
        }
    }
}