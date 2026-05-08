package com.example.tectest.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.UserProfile
import com.example.tectest.data.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class EditProfileUIState {
    object Idle : EditProfileUIState()
    object Loading : EditProfileUIState()
    data class ProfileLoaded(val userProfile: UserProfile) : EditProfileUIState()
    object UpdateInProgress : EditProfileUIState()
    data class UpdateSuccess(val message: String, val navigateBack: Boolean = false) : EditProfileUIState()
    data class UpdateError(val message: String) : EditProfileUIState()
}

class EditProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableLiveData<EditProfileUIState>()
    val uiState: LiveData<EditProfileUIState> = _uiState

    private val _currentUserProfile = MutableLiveData<UserProfile?>()
    val currentUserProfile: LiveData<UserProfile?> = _currentUserProfile

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


    init {
        loadCurrentUserProfile()
    }

    fun loadCurrentUserProfile() {
        _uiState.value = EditProfileUIState.Loading
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            _uiState.value = EditProfileUIState.UpdateError("User not authenticated.")
            return
        }

        viewModelScope.launch {
            val profileResult = authRepository.getUserProfile(firebaseUser.uid)
            profileResult.onSuccess { profile ->
                if (profile != null) {
                    val fullProfile = profile.copy(email = firebaseUser.email ?: profile.email)
                    _currentUserProfile.postValue(fullProfile)
                    _uiState.postValue(EditProfileUIState.ProfileLoaded(fullProfile))
                } else {
                    val fallbackProfile = UserProfile(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.displayName ?: ""
                    )
                    _currentUserProfile.postValue(fallbackProfile)
                    _uiState.postValue(EditProfileUIState.ProfileLoaded(fallbackProfile))
                    Log.w("EditProfileVM", "Firestore profile not found, using Auth data as fallback.")
                }
            }.onFailure {
                _uiState.postValue(EditProfileUIState.UpdateError(it.localizedMessage ?: "Failed to load profile."))
            }
        }
    }

    fun updateProfileDetails(username: String, phoneNumber: String) {
        val currentProfile = _currentUserProfile.value ?: run {
            _uiState.value = EditProfileUIState.UpdateError("Current profile data not available.")
            return
        }
        if (username.isBlank() || phoneNumber.isBlank()) {
            _uiState.value = EditProfileUIState.UpdateError("Username and phone number are required.")
            return
        }

        _uiState.value = EditProfileUIState.UpdateInProgress
        val updatedProfile = currentProfile.copy(
            username = username,
            phoneNumber = phoneNumber
        )

        viewModelScope.launch {
            val result = authRepository.updateUserProfileDetails(updatedProfile)
            result.onSuccess {
                _currentUserProfile.postValue(updatedProfile)
                _uiState.postValue(EditProfileUIState.UpdateSuccess("Profile details updated!", navigateBack = true))
            }.onFailure {
                _uiState.postValue(EditProfileUIState.UpdateError(it.localizedMessage ?: "Failed to update profile."))
            }
        }
    }

    fun updateEmail(newEmail: String, currentPasswordForReauth: String) {
        val user = firebaseAuth.currentUser ?: run {
            _uiState.value = EditProfileUIState.UpdateError("User not authenticated."); return
        }
        if (user.email.equals(newEmail, ignoreCase = true)) {
            _uiState.value = EditProfileUIState.UpdateError("New email is the same as the current one."); return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _uiState.value = EditProfileUIState.UpdateError("Invalid new email format."); return
        }
        if (currentPasswordForReauth.isBlank()) {
            _uiState.value = EditProfileUIState.UpdateError("Current password is required to change email."); return
        }

        _uiState.value = EditProfileUIState.UpdateInProgress
        viewModelScope.launch {
            // Pre-check if new email is already in use by another authenticated user
            try {
                val signInMethods = firebaseAuth.fetchSignInMethodsForEmail(newEmail).await()
                if (signInMethods != null && signInMethods.signInMethods != null && signInMethods.signInMethods!!.isNotEmpty()) {
                    // Check if its a different user. If its the current users old email, thats fine, but updateEmail should handle that. This check is more for other accounts.
                    if (firebaseAuth.currentUser?.email?.equals(newEmail, ignoreCase = true) != true) {
                        _uiState.postValue(EditProfileUIState.UpdateError("New email is already in use by another account."))
                        Log.w("EditProfileVM", "Pre-check: Email $newEmail already exists for another user.")
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w("EditProfileVM", "Pre-check for email existence failed, proceeding with update attempt.", e)
            }


            try {
                Log.d("EditProfileVM", "Re-authenticating user for email update...")
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPasswordForReauth)
                user.reauthenticate(credential).await()
                Log.d("EditProfileVM", "User re-authenticated successfully.")

                Log.d("EditProfileVM", "Attempting to update email in Firebase Auth to $newEmail")
                user.updateEmail(newEmail).await()
                Log.i("EditProfileVM", "Firebase Auth email successfully updated to $newEmail.")

                val currentProfile = _currentUserProfile.value ?: authRepository.getUserProfile(user.uid).getOrNull()
                currentProfile?.let {
                    val updatedProfile = it.copy(email = newEmail)
                    authRepository.updateUserProfileDetails(updatedProfile).onSuccess {
                        _currentUserProfile.postValue(updatedProfile)
                        _uiState.postValue(EditProfileUIState.UpdateSuccess(
                            "Email updated successfully to $newEmail.",
                            navigateBack = true
                        ))
                        Log.d("EditProfileVM", "Firestore email updated to $newEmail.")
                    }.onFailure { firestoreEx ->
                        val errorMsg = "Auth email updated, but Firestore update failed: ${firestoreEx.localizedMessage}"
                        _uiState.postValue(EditProfileUIState.UpdateError(errorMsg))
                        Log.e("EditProfileVM", "Error updating email in Firestore", firestoreEx)
                    }
                } ?: run {
                    _uiState.postValue(EditProfileUIState.UpdateError("Auth email updated, but failed to update profile in database (profile data missing)."))
                }

            } catch (e: FirebaseAuthUserCollisionException) {
                _uiState.postValue(EditProfileUIState.UpdateError("This email address is already in use by another account."))
                Log.w("EditProfileVM", "Email update failed: collision during Firebase Auth updateEmail.", e)
            } catch (e: Exception) {
                val errorDetail = e.localizedMessage ?: "An unknown error occurred."
                _uiState.postValue(EditProfileUIState.UpdateError("Failed to update email: $errorDetail"))
                Log.e("EditProfileVM", "Email update failed with exception.", e)
            }
        }
    }


    fun updatePassword(currentPasswordForReauth: String, newPassword: String, confirmNewPassword: String) {
        val user = firebaseAuth.currentUser ?: run {
            _uiState.value = EditProfileUIState.UpdateError("User not authenticated."); return
        }
        if (currentPasswordForReauth.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank()) {
            _uiState.value = EditProfileUIState.UpdateError("All password fields are required."); return
        }
        if (newPassword.length < 6) {
            _uiState.value = EditProfileUIState.UpdateError("New password must be at least 6 characters."); return
        }
        if (newPassword == currentPasswordForReauth) { // Simplified check, Firebase actually checks against hashed old pw
            _uiState.value = EditProfileUIState.UpdateError("New password cannot be the same as the old password."); return
        }
        if (newPassword != confirmNewPassword) {
            _uiState.value = EditProfileUIState.UpdateError("New passwords do not match."); return
        }

        _uiState.value = EditProfileUIState.UpdateInProgress
        viewModelScope.launch {
            try {
                // Re-authenticate user first
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPasswordForReauth)
                user.reauthenticate(credential).await()
                Log.d("EditProfileVM", "User re-authenticated successfully for password update.")

                // Update password in Firebase Auth
                user.updatePassword(newPassword).await()
                _uiState.postValue(EditProfileUIState.UpdateSuccess("Password updated successfully!", navigateBack = true))
                Log.i("EditProfileVM", "Password updated successfully.")

            } catch (e: Exception) {
                // Catch common errors like FirebaseAuthInvalidCredentialsException for wrong current password
                _uiState.postValue(EditProfileUIState.UpdateError("Failed to update password: ${e.localizedMessage}"))
                Log.e("EditProfileVM", "Password update failed", e)
            }
        }
    }
}