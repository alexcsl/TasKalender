package com.example.tectest.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.tectest.databinding.ActivityLoginBinding
import com.example.tectest.ui.home.HomeActivity
import com.example.tectest.ui.register.RegisterActivity
import com.example.tectest.viewmodel.LoginState
import com.example.tectest.viewmodel.LoginViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.i("LoginActivity", "onCreate: Activity started.")

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            Log.i("LoginActivity", "User (${firebaseAuth.currentUser?.uid}), (${firebaseAuth.currentUser?.email}) already logged in. Navigating to Home.")
            navigateToHome()
            return
        }
        Log.i("LoginActivity", "No active user session. Displaying login form.")

        setupObservers()
        setupClickListeners()
        setupInputTextWatchers()
    }

    private fun navigateToHome() {
        Log.i("LoginActivity", "navigateToHome: Preparing to start HomeActivity.")
        try {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Log.i("LoginActivity", "navigateToHome: HomeActivity started and LoginActivity finished.")
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error starting HomeActivity", e)
            Toast.makeText(this, "Error loading application: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            loginViewModel.login(email, password)
        }

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        loginViewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBarLogin.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                    Log.d("LoginActivity", "UI State: Login - Loading")
                }
                is LoginState.Success -> {
                    binding.progressBarLogin.visibility = View.GONE
                    // Don't re-enable btnLogin, we are navigating away
                    Log.i("LoginActivity", "UI State: Login - Success from ViewModel.")
                    clearAllInputErrorsFromUI()
                    navigateToHome()
                }
                is LoginState.Error -> {
                    binding.progressBarLogin.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    Log.w("LoginActivity", "UI State: Login - Error: ${state.message}")
                }
                is LoginState.Idle -> {
                    binding.progressBarLogin.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Log.d("LoginActivity", "UI State: Login - Idle")
                }
            }
        }

        loginViewModel.emailError.observe(this) { binding.tilEmail.error = it }
        loginViewModel.passwordError.observe(this) { binding.tilPassword.error = it }
    }

    private fun setupInputTextWatchers() {
        addErrorClearingTextWatcher(binding.etEmail, binding.tilEmail)
        addErrorClearingTextWatcher(binding.etPassword, binding.tilPassword)
    }

    private fun addErrorClearingTextWatcher(editText: EditText, textInputLayout: TextInputLayout) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textInputLayout.isErrorEnabled) textInputLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun clearAllInputErrorsFromUI() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }
}