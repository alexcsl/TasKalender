package com.example.tectest.ui.task

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tectest.R
import com.example.tectest.data.model.Task
import com.example.tectest.databinding.FragmentTaskBinding
import com.example.tectest.viewmodel.TaskUIState
import com.example.tectest.viewmodel.TaskViewModel
import com.google.android.material.tabs.TabLayout
import com.example.tectest.ui.task.AddTaskBottomSheetDialogFragment
import androidx.core.view.isVisible


class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    private val categoriesForTabs = listOf("All", "Home", "Work", "Other")

    // add new task
    private fun showAddTaskDialog() {
        var categoryForNewTask = taskViewModel.selectedCategory.value ?: "All"
        if (categoryForNewTask.equals("All", ignoreCase = true)) {
            categoryForNewTask = "Other"
        }
        Log.d("TaskFragment", "FAB clicked. Category for new task: '$categoryForNewTask'")

        val addTaskSheet = AddTaskBottomSheetDialogFragment.newInstanceForAdd(categoryForNewTask)
        addTaskSheet.show(childFragmentManager, AddTaskBottomSheetDialogFragment.TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabLayout()
        setupObservers()
        setupClickListeners()

        taskViewModel.loadTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClicked = { task ->
                val editTaskSheet = AddTaskBottomSheetDialogFragment.newInstanceForEdit(task)
                editTaskSheet.show(childFragmentManager, AddTaskBottomSheetDialogFragment.TAG)
            },
            onTaskCompletedChanged = { task, isCompleted ->
                taskViewModel.updateTaskCompletion(task, isCompleted)
            }
        )
        binding.recyclerViewTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupTabLayout() {
        categoriesForTabs.forEach { categoryName ->
            binding.tabLayoutTaskCategories.addTab(
                binding.tabLayoutTaskCategories.newTab().setText(categoryName)
            )
        }
        binding.tabLayoutTaskCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedCategoryName = tab?.text.toString()
                taskViewModel.selectedCategory.value = selectedCategoryName
                taskViewModel.loadTasks()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // make sure initial selection if ViewModel already has a category
        taskViewModel.selectedCategory.value?.let { currentCategory ->
            val index = categoriesForTabs.indexOf(currentCategory)
            if (index != -1) {
                binding.tabLayoutTaskCategories.getTabAt(index)?.select()
            }
        }
    }

    private fun setupObservers() {
        taskViewModel.taskUIState.observe(viewLifecycleOwner) { state ->
            binding.progressBarTasks.isVisible =
                state is TaskUIState.Loading
            binding.recyclerViewTasks.isVisible = state is TaskUIState.Success

            when (state) {
                is TaskUIState.Loading -> {
                    Log.d("TaskFragment", "Task List State: Loading")
                }

                is TaskUIState.Success -> {
                    taskAdapter.submitList(state.tasks)
                    binding.textViewNoTasksFound.isVisible =
                        state.tasks.isEmpty() && !binding.progressBarTasks.isVisible // Assuming textViewNoTasksFound ID
                    Log.d("TaskFragment", "Task List State: Success - ${state.tasks.size} tasks.")
                }

                is TaskUIState.Error -> {
                    binding.textViewNoTasksFound.text = state.message
                    binding.textViewNoTasksFound.isVisible = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    Log.e("TaskFragment", "Task List State: Error - ${state.message}")
                }

                is TaskUIState.Idle -> { /* Handle if needed */
                }
            }
        }


        taskViewModel.taskActionResult.observe(viewLifecycleOwner) { result ->
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
                        "Action failed: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::taskAdapter.isInitialized) binding.recyclerViewTasks.adapter = null
        _binding = null
    }
}