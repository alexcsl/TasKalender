package com.example.tectest.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tectest.R
import com.example.tectest.data.model.Note
import com.example.tectest.data.model.Task
import com.example.tectest.databinding.FragmentHomeBinding
import com.example.tectest.viewmodel.HomeNotesUIState
import com.example.tectest.viewmodel.HomeTasksUIState
import com.example.tectest.viewmodel.HomeViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.tectest.utils.TaskDiffCallback
import com.example.tectest.utils.NoteDiffCallback

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        Log.d("HomeFragment", "onCreateView: View inflated and ViewModel initialized.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated: Setting up UI components.")

        setupCalendar()
        setupRecyclerViews()
        setupObservers()

        // Set initial date on CalendarView from ViewModel
        homeViewModel.selectedCalendarDate.value?.let {
            binding.calendarViewHome.date = it.toDate().time
        } ?: run {
            val today = Date()
            binding.calendarViewHome.date = today.time
            homeViewModel.onDateSelected(today.time) // Inform ViewModel about initial date
        }
    }

    private fun setupCalendar() {
        binding.calendarViewHome.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            homeViewModel.onDateSelected(calendar.timeInMillis)
            Log.d("HomeFragment", "Calendar date selected by user: $dayOfMonth/${month + 1}/$year")
        }
    }

    private fun setupRecyclerViews() {
        // Tasks
        tasksAdapter = TasksAdapter(
            onTaskCheckedChange = { task, isCompleted ->
                homeViewModel.updateTaskCompletionStateOnHome(task, isCompleted)
            },
            onTaskClicked = { task ->
                Toast.makeText(
                    requireContext(),
                    "Task displayed: ${task.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.recyclerViewTasksHome.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // isNestedScrollingEnabled = false
        }

        // Notes
        notesAdapter = NotesAdapter { note ->
            Toast.makeText(requireContext(), "Note displayed: ${note.title}", Toast.LENGTH_SHORT)
                .show()
        }
        binding.recyclerViewNotesHome.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // isNestedScrollingEnabled = false
        }
        Log.d("HomeFragment", "RecyclerViews setup complete.")
    }

    private fun setupObservers() {
        homeViewModel.selectedCalendarDate.observe(viewLifecycleOwner) { timestamp ->
            Log.d(
                "HomeFragment",
                "Observed selectedCalendarDate LiveData change: ${timestamp.toDate()}"
            )
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            binding.textViewTasksLabel.text =
                getString(R.string.title_tasks_for_date, sdf.format(timestamp.toDate()))
        }

        homeViewModel.homeTasksState.observe(viewLifecycleOwner) { state ->
            binding.recyclerViewTasksHome.isVisible = state is HomeTasksUIState.Success
            binding.textViewNoTasks.isVisible = false // Default

            when (state) {
                is HomeTasksUIState.Loading -> {
                    Log.d("HomeFragment", "Task State: Loading")
                }

                is HomeTasksUIState.Success -> {
                    tasksAdapter.submitList(state.tasks)
                    if (state.tasks.isEmpty()) {
                        binding.textViewNoTasks.text =
                            getString(R.string.no_tasks_for_selected_date)
                        binding.textViewNoTasks.isVisible = true
                    }
                    Log.d("HomeFragment", "Task State: Success - ${state.tasks.size} tasks.")
                }

                is HomeTasksUIState.Error -> {
                    binding.textViewNoTasks.text = state.message
                    binding.textViewNoTasks.isVisible = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    Log.e("HomeFragment", "Task State: Error - ${state.message}")
                }
            }
        }

        homeViewModel.recentNotesState.observe(viewLifecycleOwner) { state ->
            binding.recyclerViewNotesHome.isVisible = state is HomeNotesUIState.Success
            binding.textViewNoNotes.isVisible = false

            when (state) {
                is HomeNotesUIState.Loading -> {
                    Log.d("HomeFragment", "Notes State: Loading")
                }

                is HomeNotesUIState.Success -> {
                    notesAdapter.submitList(state.notes)
                    if (state.notes.isEmpty()) {
                        binding.textViewNoNotes.text = getString(R.string.no_recent_notes)
                        binding.textViewNoNotes.isVisible = true
                    }
                    Log.d("HomeFragment", "Notes State: Success - ${state.notes.size} notes.")
                }

                is HomeNotesUIState.Error -> {
                    binding.textViewNoNotes.text = state.message
                    binding.textViewNoNotes.isVisible = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    Log.e("HomeFragment", "Notes State: Error - ${state.message}")
                }
            }
        }

        homeViewModel.actionFeedback.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { message ->
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Error: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
        Log.d("HomeFragment", "Observers setup complete.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::tasksAdapter.isInitialized) binding.recyclerViewTasksHome.adapter = null
        if (::notesAdapter.isInitialized) binding.recyclerViewNotesHome.adapter = null
        _binding = null
        Log.d("HomeFragment", "onDestroyView: View unbound.")
    }
}
