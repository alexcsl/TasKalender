package com.example.tectest.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.model.UserProfile
import com.example.tectest.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class ProfileUIState {
    object Loading : ProfileUIState()
    data class Success(val userProfile: UserProfile) : ProfileUIState()
    data class Error(val message: String) : ProfileUIState()
}

sealed class LogoutState {
    object Idle : LogoutState()
    object Success : LogoutState()
}

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _profileState = MutableLiveData<ProfileUIState>()
    val profileState: LiveData<ProfileUIState> = _profileState

    private val _logoutState = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutState: LiveData<LogoutState> = _logoutState

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _profileState.value = ProfileUIState.Loading
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null || currentUser.uid.isBlank()) {
            _profileState.value = ProfileUIState.Error("User not logged in.")
            return
        }

        viewModelScope.launch {
            val email = currentUser.email ?: "N/A"

            // Fetch other details from Firestore via AuthRepository
            val firestoreProfileResult = authRepository.getUserProfile(currentUser.uid)
            firestoreProfileResult.onSuccess { firestoreProfile ->
                if (firestoreProfile != null) {
                    // Combine Auth email with Firestore details
                    val completeProfile = firestoreProfile.copy(email = email)
                    _profileState.postValue(ProfileUIState.Success(completeProfile))
                    Log.d("ProfileViewModel", "User profile loaded: $completeProfile")
                } else {
                    val partialProfile = UserProfile(
                        uid = currentUser.uid,
                        email = email,
                        username = "N/A (Update Profile)",
                        phoneNumber = "N/A (Update Profile)",
                        dateOfBirth = "N/A (Update Profile)"
                    )
                    _profileState.postValue(ProfileUIState.Success(partialProfile))
                    Log.w("ProfileViewModel", "Firestore profile not found for ${currentUser.uid}, showing partial.")
                }
            }.onFailure { exception ->
                val errorMsg = exception.localizedMessage ?: "Failed to load profile details"
                _profileState.postValue(ProfileUIState.Error(errorMsg))
                Log.e("ProfileViewModel", "Error loading profile: $errorMsg", exception)
            }
        }
    }

    fun logoutUser() {
        authRepository.signOut()
        _logoutState.value = LogoutState.Success
        Log.i("ProfileViewModel", "User logged out.")
    }

    fun resetLogoutState() {
        _logoutState.value = LogoutState.Idle
    }
}