package com.example.tectest.ui.task

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.data.model.Task
import com.example.tectest.databinding.ItemTaskBinding
import com.example.tectest.utils.TaskDiffCallback
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onTaskCompletedChanged: (Task, Boolean) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(
            task: Task,
            onTaskClicked: (Task) -> Unit,
            onTaskCompletedChanged: (Task, Boolean) -> Unit
        ) {
            Log.d("TaskAdapter", "Binding task: ID=${task.id}, Title='${task.title}'")
            binding.textViewTaskTitle.text = task.title
            binding.textViewTaskDueDate.text = itemView.context.getString(R.string.due_date_prefix, dateFormat.format(task.dueDate.toDate()))
            binding.textViewTaskCategory.text = task.category
            binding.checkboxTaskCompleted.isChecked = task.isCompleted

            updateTitleStrikeThrough(task.isCompleted)

            binding.checkboxTaskCompleted.setOnCheckedChangeListener(null)
            binding.checkboxTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTaskCompletedChanged(task, isChecked)
            }

            itemView.setOnClickListener {
                onTaskClicked(task)
            }
        }

        private fun updateTitleStrikeThrough(isCompleted: Boolean) {
            if (isCompleted) {
                binding.textViewTaskTitle.paintFlags = binding.textViewTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textViewTaskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.icon_tint_inactive))
            } else {
                binding.textViewTaskTitle.paintFlags = binding.textViewTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                val typedArray = itemView.context.obtainStyledAttributes(arrayOf(com.google.android.material.R.attr.colorOnSurface).toIntArray())
                val defaultTextColor = typedArray.getColor(0, ContextCompat.getColor(itemView.context, R.color.app_on_surface_text))
                typedArray.recycle()
                binding.textViewTaskTitle.setTextColor(defaultTextColor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task, onTaskClicked, onTaskCompletedChanged)
    }
}