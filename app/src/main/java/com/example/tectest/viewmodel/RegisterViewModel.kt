package com.example.tectest.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

data class RegisterValidationErrors(
    val emailError: String? = null,
    val usernameError: String? = null,
    val phoneError: String? = null,
    val dobError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    private val _validationErrors = MutableLiveData<RegisterValidationErrors?>()
    val validationErrors: LiveData<RegisterValidationErrors?> = _validationErrors

    fun register(email: String, username: String, phone: String, dob: String, password: String, confirmPassword: String) {
        val errors = validateInputs(email, username, phone, dob, password, confirmPassword)
        if (errors != null) {
            _validationErrors.value = errors
            _registerState.value = RegisterState.Idle
            return
        } else {
            _validationErrors.value = null
        }

        _registerState.value = RegisterState.Loading
        Log.d("RegisterViewModel", "Registration process started for: $email")

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    authRepository.registerUser(email, password, username, phone, dob)
                }
                result.onSuccess {
                    Log.d("RegisterViewModel", "Firebase registration successful for: $email. Posting Success state.")
                    _registerState.postValue(RegisterState.Success)
                }.onFailure { exception ->
                    Log.e("RegisterViewModel", "Registration failed for: $email", exception)
                    val errorMessage = when (exception) {
                        is FirebaseAuthUserCollisionException -> "This email address is already in use."
                        is FirebaseAuthWeakPasswordException -> "Password is too weak (must be at least 6 characters)."
                        is FirebaseAuthInvalidCredentialsException -> "The email address is badly formatted."
                        is IllegalStateException -> exception.message ?: "A problem occurred verifying registration." // Catch our custom one
                        else -> {
                            val specificMessage = exception.localizedMessage
                            if (!specificMessage.isNullOrBlank()) {
                                "Registration Error: $specificMessage"
                            } else {
                                "An unknown error occurred. (Type: ${exception.javaClass.simpleName})"
                            }
                        }
                    }
                    _registerState.postValue(RegisterState.Error(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Unexpected error in registration coroutine", e)
                _registerState.postValue(RegisterState.Error("An unexpected error occurred during registration."))
            }
        }
    }

    private fun validateInputs(email: String, username: String, phone: String, dob: String, password: String, confirmPassword: String): RegisterValidationErrors? {
        var hasError = false
        var emailErr: String? = null
        var userErr: String? = null
        var phoneErr: String? = null
        var dobErr: String? = null
        var passErr: String? = null
        var confirmPassErr: String? = null

        if (email.isBlank()) {
            emailErr = "Email is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErr = "Invalid email format"
            hasError = true
        }
        if (username.isBlank()) {
            userErr = "Username is required"
            hasError = true
        }
        if (phone.isBlank()) {
            phoneErr = "Phone number is required"
            hasError = true
        }
        if (dob.isBlank()) {
            dobErr = "Date of birth is required"
            hasError = true
        }
        if (password.length < 6) {
            passErr = "Password must be at least 6 characters"
            hasError = true
        }
        if (confirmPassword.isBlank()) {
            confirmPassErr = "Please confirm your password"
            hasError = true
        } else if (password != confirmPassword) {
            confirmPassErr = "Passwords do not match"
            hasError = true
        }

        return if (hasError) {
            RegisterValidationErrors(emailErr, userErr, phoneErr, dobErr, passErr, confirmPassErr)
        } else {
            null
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
        _validationErrors.value = null
    }
}