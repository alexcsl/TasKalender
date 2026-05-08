package com.example.tectest.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tectest.R
import com.example.tectest.databinding.ItemFolderBinding
import com.example.tectest.viewmodel.FolderWithNoteCount
import com.example.tectest.utils.FolderDiffCallback
import java.text.SimpleDateFormat
import java.util.Locale

class FolderAdapter(
    private val onFolderClicked: (FolderWithNoteCount) -> Unit
) : ListAdapter<FolderWithNoteCount, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    class FolderViewHolder(private val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(folderWithCount: FolderWithNoteCount, onFolderClicked: (FolderWithNoteCount) -> Unit) {
            val folder = folderWithCount.folder
            binding.textViewFolderName.text = folder.name
            binding.textViewNoteCount.text = itemView.context.resources.getQuantityString(
                R.plurals.numberOfNotes, folderWithCount.noteCount, folderWithCount.noteCount
            )

            binding.textViewFolderCreatedDate.text = folder.createdAt?.let {
                itemView.context.getString(R.string.created_date_prefix, dateFormat.format(it.toDate()))
            } ?: ""

            itemView.setOnClickListener { onFolderClicked(folderWithCount) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position), onFolderClicked)
    }
}