package com.example.tectest.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.databinding.ItemNoteHomeBinding
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.tectest.data.model.Note
import com.example.tectest.utils.NoteDiffCallback

class NotesAdapter(
    private val onNoteDisplayClick: (Note) -> Unit = { note -> /* Default no-op since no redirect */ }
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    class NoteViewHolder(private val binding: ItemNoteHomeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

        fun bind(note: Note, onNoteDisplayClick: (Note) -> Unit) {
            binding.textViewHomeNoteTitle.text = note.title
            binding.textViewHomeNotePreview.text = note.content
            binding.textViewNoteDateHome.text = note.createdAt?.let {
                dateFormat.format(it.toDate())
            } ?: ""

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "Note: ${note.title}", Toast.LENGTH_SHORT).show()
                onNoteDisplayClick(note)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position), onNoteDisplayClick)
    }
}