package com.example.tectest.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.tectest.R
import com.example.tectest.data.model.UserProfile
import com.example.tectest.databinding.FragmentEditProfileBinding
import com.example.tectest.viewmodel.EditProfileUIState
import com.example.tectest.viewmodel.EditProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditProfileViewModel
    private val dobDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDOB: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(EditProfileViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarEditProfile.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        // No menu needed if using separate buttons for update actions
    }

    private fun setupClickListeners() {
         binding.btnUpdateProfileDetails.setOnClickListener {
            val username = binding.etEditUsername.text.toString().trim()
            val phone = binding.etEditPhone.text.toString().trim()
            viewModel.updateProfileDetails(username, phone)
        }

        binding.btnUpdateEmail.setOnClickListener {
            val currentPassword = binding.etEditEmailCurrentPassword.text.toString()
            val newEmail = binding.etEditNewEmail.text.toString().trim()
            viewModel.updateEmail(newEmail, currentPassword)
        }

        binding.btnChangePassword.setOnClickListener {
            val currentPassword = binding.etEditCurrentPassword.text.toString()
            val newPassword = binding.etEditNewPassword.text.toString()
            val confirmNewPassword = binding.etEditConfirmNewPassword.text.toString()
            viewModel.updatePassword(currentPassword, newPassword, confirmNewPassword)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressBarEditProfile.isVisible =
                state is EditProfileUIState.Loading || state is EditProfileUIState.UpdateInProgress

            when (state) {
                is EditProfileUIState.Loading -> {
                    Log.d("EditProfileFragment", "UI State: Loading profile data")
                    // disable buttons while loading initial profile
                    setFormEnabled(false)
                }
                is EditProfileUIState.ProfileLoaded -> {
                    Log.d("EditProfileFragment", "UI State: ProfileLoaded")
                    prefillProfileData(state.userProfile)
                    setFormEnabled(true) // enable form after data is loaded
                }
                is EditProfileUIState.UpdateInProgress -> {
                    Log.d("EditProfileFragment", "UI State: Update In Progress")
                    // disable buttons
                    setFormEnabled(false)
                }
                is EditProfileUIState.UpdateSuccess -> {
                    Log.i("EditProfileFragment", "UI State: UpdateSuccess - ${state.message}")
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    setFormEnabled(true) // enable form
                    if (state.navigateBack) {
                        findNavController().popBackStack() // go back to ProfileFragment
                    }
                }
                is EditProfileUIState.UpdateError -> {
                    Log.w("EditProfileFragment", "UI State: UpdateError - ${state.message}")
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    setFormEnabled(true)
                }
                is EditProfileUIState.Idle -> {
                    Log.d("EditProfileFragment", "UI State: Idle")
                    setFormEnabled(true)
                }
            }
        }

        // observe currentUserProfile to prefill initially or if reloaded
        viewModel.currentUserProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                if (binding.etEditUsername.text.isNullOrBlank() &&
                    binding.etEditPhone.text.isNullOrBlank() &&
                    binding.etEditDob.text.isNullOrBlank() &&
                    binding.etEditNewEmail.text.isNullOrBlank()
                ) {
                    Log.d("EditProfileFragment", "Prefilling form from currentUserProfile observer.")
                    prefillProfileData(it)
                }
            }
        }
    }

    private fun prefillProfileData(profile: UserProfile) {
        binding.etEditUsername.setText(profile.username)
        binding.etEditPhone.setText(profile.phoneNumber)
        binding.etEditNewEmail.setText(profile.email)
        if (profile.dateOfBirth.isNotBlank()) {
            try {
                // Attempt to parse and set if format matches
                val date = dobDateFormat.parse(profile.dateOfBirth)
                if (date != null) {
                    selectedDOB.time = date
                    binding.etEditDob.setText(dobDateFormat.format(date))
                } else {
                    binding.etEditDob.setText(profile.dateOfBirth)
                }
            } catch (e: Exception) {
                binding.etEditDob.setText(profile.dateOfBirth)
                Log.w("EditProfileFragment", "Could not parse stored DOB: ${profile.dateOfBirth}", e)
            }
        }
    }

    private fun setFormEnabled(isEnabled: Boolean) {
        binding.etEditUsername.isEnabled = isEnabled
        binding.etEditPhone.isEnabled = isEnabled
        binding.etEditDob.isEnabled = isEnabled
        binding.tilEditDob.isEnabled = isEnabled
        binding.btnUpdateProfileDetails.isEnabled = isEnabled

        binding.etEditEmailCurrentPassword.isEnabled = isEnabled
        binding.etEditNewEmail.isEnabled = isEnabled
        binding.btnUpdateEmail.isEnabled = isEnabled

        binding.etEditCurrentPassword.isEnabled = isEnabled
        binding.etEditNewPassword.isEnabled = isEnabled
        binding.etEditConfirmNewPassword.isEnabled = isEnabled
        binding.btnChangePassword.isEnabled = isEnabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
