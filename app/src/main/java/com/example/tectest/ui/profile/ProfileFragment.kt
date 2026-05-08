package com.example.tectest.ui.profile

import android.content.Intent
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
import com.example.tectest.databinding.FragmentProfileBinding
import com.example.tectest.ui.login.LoginActivity
import com.example.tectest.viewmodel.LogoutState
import com.example.tectest.viewmodel.ProfileUIState
import com.example.tectest.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadUserProfile()
    }

    private fun setupClickListeners() {
        binding.buttonLogout.setOnClickListener {
            profileViewModel.logoutUser()
        }
        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
            Toast.makeText(requireContext(), "Edit Profile Clicked (Implement Nav)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        profileViewModel.profileState.observe(viewLifecycleOwner) { state ->
            binding.progressBarProfile.isVisible = state is ProfileUIState.Loading
            binding.imageViewProfilePicture.isVisible = state is ProfileUIState.Success
            binding.textViewProfileUsername.isVisible = state is ProfileUIState.Success
            binding.textViewProfileEmail.isVisible = state is ProfileUIState.Success
            binding.textViewProfilePhone.isVisible = state is ProfileUIState.Success

            when (state) {
                is ProfileUIState.Loading -> { /* Handled by isVisible */ }
                is ProfileUIState.Success -> {
                    binding.textViewProfileUsername.text = state.userProfile.username.ifBlank { "Tap edit to set" }
                    binding.textViewProfileEmail.text = state.userProfile.email
                    binding.textViewProfilePhone.text = state.userProfile.phoneNumber.ifBlank { "Tap edit to set" }
                }
                is ProfileUIState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        profileViewModel.logoutState.observe(viewLifecycleOwner) { state ->
            if (state is LogoutState.Success) {
                Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show()
                // Navigate to LoginActivity and clear back stack
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
                profileViewModel.resetLogoutState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}