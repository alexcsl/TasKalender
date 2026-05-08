package com.example.tectest.ui.home

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.data.model.Task
import com.example.tectest.databinding.ItemTaskHomeBinding
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.tectest.utils.TaskDiffCallback

class TasksAdapter(
    private val onTaskCheckedChange: (Task, Boolean) -> Unit,
    private val onTaskClicked: (Task) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(private val binding: ItemTaskHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        fun bind(task: Task, onTaskCheckedChange: (Task, Boolean) -> Unit, onTaskClicked: (Task) -> Unit) {
            binding.textViewHomeTaskTitle.text = task.title
            binding.textViewHomeTaskDueDate.text = itemView.context.getString(R.string.due_date_prefix_simple, dateFormat.format(task.dueDate.toDate()))

            binding.checkboxHomeTaskCompleted.setOnCheckedChangeListener(null)
            binding.checkboxHomeTaskCompleted.isChecked = task.isCompleted

            if (task.isCompleted) {
                binding.textViewHomeTaskTitle.paintFlags = binding.textViewHomeTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textViewHomeTaskTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.icon_tint_inactive))
            } else {
                binding.textViewHomeTaskTitle.paintFlags = binding.textViewHomeTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                val typedArray = itemView.context.obtainStyledAttributes(arrayOf(com.google.android.material.R.attr.colorOnSurfaceVariant).toIntArray())
                val defaultTextColor = typedArray.getColor(0, ContextCompat.getColor(itemView.context, R.color.app_on_surface_text)) // Fallback
                typedArray.recycle()
                binding.textViewHomeTaskTitle.setTextColor(defaultTextColor)
            }

            binding.checkboxHomeTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTaskCheckedChange(task, isChecked)
            }
            itemView.setOnClickListener { onTaskClicked(task) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskHomeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), onTaskCheckedChange, onTaskClicked)
    }
}