package com.example.tectest.ui.register

import android.app.DatePickerDialog
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
import com.example.tectest.databinding.ActivityRegisterBinding
import com.example.tectest.ui.home.HomeActivity
import com.example.tectest.ui.login.LoginActivity
import com.example.tectest.viewmodel.RegisterState
import com.example.tectest.viewmodel.RegisterViewModel
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var registerViewModel: RegisterViewModel
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.i("RegisterActivity", "onCreate: Activity started.")

        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        setupObservers()
        setupClickListeners()
        setupInputTextWatchers()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etRegisterEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val dob = binding.etDob.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            registerViewModel.register(email, username, phone, dob, password, confirmPassword)
        }

        binding.btnGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        binding.etDob.setOnClickListener { showDatePickerDialog() }
        binding.tilDob.setEndIconOnClickListener { showDatePickerDialog() }
    }

    private fun showDatePickerDialog() {
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }, currentYear, currentMonth, currentDay)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.etDob.setText(sdf.format(calendar.time))
        if (binding.tilDob.isErrorEnabled) binding.tilDob.error = null
    }

    private fun setupObservers() {
        registerViewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterState.Loading -> {
                    binding.progressBarRegister.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                    binding.btnGoToLogin.isEnabled = false
                    Log.d("RegisterActivity", "UI State: Register - Loading")
                }
                is RegisterState.Success -> {
                    binding.progressBarRegister.visibility = View.GONE
                    Log.i("RegisterActivity", "UI State: Register - Success from ViewModel. Navigating to Home.")
                    Toast.makeText(this, "Registration Successful! Welcome!", Toast.LENGTH_LONG).show()
                    clearAllInputErrorsFromUI()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                }
                is RegisterState.Error -> {
                    binding.progressBarRegister.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    binding.btnGoToLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    Log.w("RegisterActivity", "UI State: Register - Error: ${state.message}")
                    if (state.message.contains("Email address already in use", ignoreCase = true)) {
                        binding.tilRegisterEmail.error = state.message
                    }
                }
                is RegisterState.Idle -> {
                    binding.progressBarRegister.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    binding.btnGoToLogin.isEnabled = true
                    Log.d("RegisterActivity", "UI State: Register - Idle")
                }
            }
        }

        registerViewModel.validationErrors.observe(this) { errors ->
            binding.tilRegisterEmail.error = errors?.emailError
            binding.tilUsername.error = errors?.usernameError
            binding.tilPhone.error = errors?.phoneError
            binding.tilDob.error = errors?.dobError
            binding.tilRegisterPassword.error = errors?.passwordError
            binding.tilConfirmPassword.error = errors?.confirmPasswordError
        }
    }

    private fun clearAllInputErrorsFromUI() {
        binding.tilRegisterEmail.error = null
        binding.tilUsername.error = null
    }

    private fun setupInputTextWatchers() {
        addErrorClearingTextWatcher(binding.etRegisterEmail, binding.tilRegisterEmail)
        addErrorClearingTextWatcher(binding.etUsername, binding.tilUsername)
        addErrorClearingTextWatcher(binding.etPhone, binding.tilPhone)
        addErrorClearingTextWatcher(binding.etRegisterPassword, binding.tilRegisterPassword)
        addErrorClearingTextWatcher(binding.etConfirmPassword, binding.tilConfirmPassword)
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
}