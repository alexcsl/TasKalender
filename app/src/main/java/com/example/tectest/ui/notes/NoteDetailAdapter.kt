package com.example.tectest.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.data.model.Note
import com.example.tectest.databinding.ItemNoteDetailBinding
// import com.example.tectest.databinding.ItemNoteGroupHeaderBinding
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat

sealed class NoteDetailListItem {
    data class NoteHeader(val title: String, val id: String = title) : NoteDetailListItem()
    data class NoteEntry(val note: Note) : NoteDetailListItem()
    val itemId: String
        get() = when(this) {
            is NoteHeader -> this.id
            is NoteEntry -> this.note.id
        }
}

class NoteDetailListItemDiffCallback : DiffUtil.ItemCallback<NoteDetailListItem>() {
    override fun areItemsTheSame(oldItem: NoteDetailListItem, newItem: NoteDetailListItem): Boolean {
        return oldItem.itemId == newItem.itemId
    }
    override fun areContentsTheSame(oldItem: NoteDetailListItem, newItem: NoteDetailListItem): Boolean {
        return oldItem == newItem
    }
}

class NoteDetailAdapter(
    private val onNoteClicked: (Note) -> Unit
) : ListAdapter<NoteDetailListItem, RecyclerView.ViewHolder>(NoteDetailListItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_NOTE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NoteDetailListItem.NoteHeader -> VIEW_TYPE_HEADER
            is NoteDetailListItem.NoteEntry -> VIEW_TYPE_NOTE_ENTRY
            null -> throw IllegalStateException("Null item in adapter")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val textView = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(48, 24, 24, 12)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall) // Or TitleMedium
                    setTextColor(ContextCompat.getColor(parent.context, R.color.app_on_background_text))
                }
                NoteHeaderViewHolder(textView)
            }
            VIEW_TYPE_NOTE_ENTRY -> {
                val binding = ItemNoteDetailBinding.inflate(inflater, parent, false)
                NoteEntryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            when (item) {
                is NoteDetailListItem.NoteHeader -> (holder as NoteHeaderViewHolder).bind(item)
                is NoteDetailListItem.NoteEntry -> (holder as NoteEntryViewHolder).bind(item.note, onNoteClicked)
            }
        }
    }

    class NoteHeaderViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(header: NoteDetailListItem.NoteHeader) {
            textView.text = header.title
        }
    }

    class NoteEntryViewHolder(private val binding: ItemNoteDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        fun bind(note: Note, onNoteClicked: (Note) -> Unit) {
            binding.textViewNoteDetailTitle.text = note.title
            binding.textViewNoteDetailPreview.text = note.content
            binding.textViewNoteDetailDate.text = note.createdAt?.let {
                "Created: ${dateFormat.format(it.toDate())}"
            } ?: "Date N/A"
            itemView.setOnClickListener { onNoteClicked(note) }
        }
    }
}