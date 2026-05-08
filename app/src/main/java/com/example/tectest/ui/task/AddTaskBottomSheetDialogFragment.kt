package com.example.tectest.ui.task

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.tectest.R
import com.example.tectest.data.model.Task
import com.example.tectest.databinding.BottomSheetAddTaskBinding
import com.example.tectest.viewmodel.TaskViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.example.tectest.utils.getParcelableCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTaskBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddTaskBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by activityViewModels()

    private var selectedDueDate: Calendar = Calendar.getInstance() // Defaults to today
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    //  will be edited when addint new task or editing
    private var categoryToAssignOnNew: String = "Other"
    private var existingTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_TASK_ID)) { // editing existing task
                existingTask = Task(
                    id = it.getString(ARG_TASK_ID)!!,
                    title = it.getString(ARG_TASK_TITLE)!!,
                    category = it.getString(ARG_TASK_CATEGORY)!!,
                    dueDate = it.getParcelableCompat(ARG_TASK_DUE_DATE, Timestamp::class.java)!!,
                    isCompleted = it.getBoolean(ARG_TASK_IS_COMPLETED)
                )
                Log.d(TAG, "onCreate: Editing existing task - ID: ${existingTask?.id}, Title: ${existingTask?.title}")
            } else { // Adding new task
                categoryToAssignOnNew = it.getString(ARG_CATEGORY_FOR_NEW_TASK, "Other")
                Log.d(TAG, "onCreate: Adding new task with category: $categoryToAssignOnNew")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated. Is existingTask null? ${existingTask == null}")

        if (existingTask != null) {
            binding.textViewAddTaskTitleHeader.text = getString(R.string.title_edit_task)
            binding.btnSaveTask.text = getString(R.string.button_update_task)
            binding.tvDeleteTask.visibility = View.VISIBLE
            prefillFieldsForEdit()
        } else {
            binding.textViewAddTaskTitleHeader.text = getString(R.string.title_add_task)
            binding.btnSaveTask.text = getString(R.string.button_save_task)
            binding.tvDeleteTask.visibility = View.GONE
            binding.tvAddTaskDueDate.text = dateFormat.format(selectedDueDate.time)
        }

        setupDueDateClickListener()
        setupSaveButton()
        setupDeleteButtonListener()
    }

    private fun prefillFieldsForEdit() {
        existingTask?.let { task ->
            binding.etAddTaskTitle.setText(task.title)
            selectedDueDate.time = task.dueDate.toDate()
            binding.tvAddTaskDueDate.text = dateFormat.format(task.dueDate.toDate())
            // Category is not editable
        }
    }

    private fun setupDueDateClickListener() {
        binding.tvAddTaskDueDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val currentYear = selectedDueDate.get(Calendar.YEAR)
        val currentMonth = selectedDueDate.get(Calendar.MONTH)
        val currentDay = selectedDueDate.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, year, month, day ->
            selectedDueDate.set(year, month, day, 0,0,0)
            selectedDueDate.set(Calendar.MILLISECOND, 0)
            binding.tvAddTaskDueDate.text = dateFormat.format(selectedDueDate.time)
        }, currentYear, currentMonth, currentDay).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTask.setOnClickListener {
            val title = binding.etAddTaskTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.tilAddTaskTitle.error = getString(R.string.error_field_required)
                return@setOnClickListener
            } else {
                binding.tilAddTaskTitle.error = null
            }

            val finalDueDate = Timestamp(selectedDueDate.time)

            if (existingTask != null) {
                // Editing existing task
                val updatedTask = existingTask!!.copy(
                    title = title,
                    dueDate = finalDueDate
                )
                Log.d(TAG, "Updating task: ID=${updatedTask.id}, Title='${updatedTask.title}', Category='${updatedTask.category}'")
                taskViewModel.editTask(updatedTask)
            } else {
                // Adding new task
                Log.d(TAG, "Adding new task. Title: '$title', Category: '$categoryToAssignOnNew', DueDate: ${finalDueDate.toDate()}")
                taskViewModel.addTask(title, categoryToAssignOnNew, selectedDueDate.time) // addTask expects java.util.Date
            }
            dismiss()
        }
    }

    private fun setupDeleteButtonListener() {
        binding.tvDeleteTask.setOnClickListener {
            existingTask?.let { taskToDelete ->
                showDeleteConfirmationDialog(taskToDelete)
            }
        }
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_task_title)
            .setMessage(getString(R.string.confirm_delete_task_message, task.title))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.button_delete_task_dialog) { _, _ ->
                Log.d(TAG, "CONFIRMED DELETE for task: ID=${task.id}, Title='${task.title}'") // ADD THIS LOG
                taskViewModel.deleteTask(task.id, task.title)
                dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: AddTaskBottomSheet unbound.")
    }

    companion object {
        const val TAG = "AddTaskBottomSheetDialogFragment"
        private const val ARG_CATEGORY_FOR_NEW_TASK = "arg_category_for_new_task"
        private const val ARG_TASK_ID = "arg_task_id"
        private const val ARG_TASK_TITLE = "arg_task_title"
        private const val ARG_TASK_CATEGORY = "arg_task_category"
        private const val ARG_TASK_DUE_DATE = "arg_task_due_date"
        private const val ARG_TASK_IS_COMPLETED = "arg_task_is_completed"

        fun newInstanceForAdd(categoryForNewTask: String): AddTaskBottomSheetDialogFragment {
            return AddTaskBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_FOR_NEW_TASK, categoryForNewTask)
                }
            }
        }

        fun newInstanceForEdit(task: Task): AddTaskBottomSheetDialogFragment {
            return AddTaskBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TASK_ID, task.id)
                    putString(ARG_TASK_TITLE, task.title)
                    putString(ARG_TASK_CATEGORY, task.category)
                    putParcelable(ARG_TASK_DUE_DATE, task.dueDate) // Timestamp is Parcelable
                    putBoolean(ARG_TASK_IS_COMPLETED, task.isCompleted)
                }
            }
        }
    }
}