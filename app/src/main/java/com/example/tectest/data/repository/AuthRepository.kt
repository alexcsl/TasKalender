package com.example.tectest.data.repository

import android.util.Log
import com.example.tectest.data.model.UserProfile
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class AuthRepository {
    // Class setups: Initializes firebase services
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Data retrieval: Provides data from firestore -> UserData to ViewModel -> shown on View
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val documentSnapshot = firestore.collection("users").document(userId).get().await()
            if (documentSnapshot.exists()) {
                val userProfile = documentSnapshot.toObject(UserProfile::class.java)?.copy(uid = userId) // Converts Firestore to UserProfile
                Result.success(userProfile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user profile for $userId", e)
            Result.failure(e)
        }
    }

    // Authentication  for Login
    suspend fun loginUser(email: String, password: String): Result<AuthResult> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "Login successful for $email")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed for $email", e)
            Result.failure(e)
        }
    }

    // User creation
    suspend fun registerUser(email: String, password: String, username: String, phone: String, dob: String): Result<AuthResult> {
        return try {
            Log.d("AuthRepository", "Attempting to create user $email")
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()  // Creates firebase auth
            Log.d("AuthRepository", "Auth user creation call completed for $email")

            var firebaseUser = authResult.user
            // Validation n Checking
            if (firebaseUser == null) {
                Log.w("AuthRepository", "Initial authResult.user was null for $email. Delaying and re-fetching.")
                delay(300)
                firebaseUser = firebaseAuth.currentUser
            }

            // Store it to firebase
            firebaseUser?.let { user ->
                Log.d("AuthRepository", "User ${user.uid} successfully retrieved for $email. Writing to Firestore.")
                val userMap = hashMapOf(
                    "uid" to user.uid,
                    "username" to username,
                    "email" to email,
                    "phone" to phone,
                    "dateOfBirth" to dob
                )
                // Purely to check performance on create user.. somehow still slow :(
                val firestoreStartTime = System.currentTimeMillis()
                firestore.collection("users").document(user.uid).set(userMap).await()
                val firestoreEndTime = System.currentTimeMillis()
                Log.d("AuthRepository", "Firestore write for ${user.uid} took ${firestoreEndTime - firestoreStartTime}ms")
                Result.success(authResult)
            } ?: run {
                Log.e("AuthRepository", "Firebase user remained null for $email even after re-fetch attempt.")
                throw IllegalStateException("Firebase user could not be confirmed after creation.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration failed in repository for $email", e)
            Result.failure(e)
        }
    }

    // Data modification for Profile Page
    suspend fun updateUserProfileDetails(userProfile: UserProfile): Result<Unit> {
        val userId = userProfile.uid.ifBlank { firebaseAuth.currentUser?.uid }
        if (userId.isNullOrBlank()) {
            Log.e("AuthRepository", "User ID is missing, cannot update profile details.")
            return Result.failure(IllegalArgumentException("User ID is missing"))
        }
        return try {
            val updates = mapOf(
                "username" to userProfile.username,
                "phoneNumber" to userProfile.phoneNumber,
                // "dateOfBirth" to userProfile.dateOfBirth
            )
            firestore.collection("users").document(userId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d("AuthRepository", "User profile details (username, phone) updated in Firestore for $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating user profile details for $userId", e)
            Result.failure(e)
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    fun signOut() {
        firebaseAuth.signOut()
        Log.d("AuthRepository", "User signed out.")
    }
}