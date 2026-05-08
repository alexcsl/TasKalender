package com.example.tectest.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tectest.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    fun login(email: String, password: String) {
        var isValid = true
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Invalid email format"
            isValid = false
        } else {
            _emailError.value = null
        }

        if (password.isEmpty()) {
            _passwordError.value = "Password cannot be empty"
            isValid = false
        } else {
            _passwordError.value = null
        }

        if (!isValid) {
            _loginState.value = LoginState.Idle
            return
        }

        _loginState.value = LoginState.Loading
        Log.d("LoginViewModel", "Login process started for: $email")

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    authRepository.loginUser(email, password)
                }
                result.onSuccess {
                    Log.d("LoginViewModel", "Firebase login successful for: $email. Posting Success state.")
                    _loginState.postValue(LoginState.Success)
                }.onFailure { exception ->
                    Log.e("LoginViewModel", "Login failed for: $email", exception)
                    val errorMessage = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect password."
                        else -> exception.localizedMessage ?: "Unknown login error."
                    }
                    _loginState.postValue(LoginState.Error(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Unexpected error in login coroutine", e)
                _loginState.postValue(LoginState.Error("An unexpected error occurred during login."))
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
        _emailError.value = null
        _passwordError.value = null
    }
}