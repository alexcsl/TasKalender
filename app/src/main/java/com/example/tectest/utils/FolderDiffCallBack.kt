package com.example.tectest.utils

import androidx.recyclerview.widget.DiffUtil
import com.example.tectest.viewmodel.FolderWithNoteCount

class FolderDiffCallback : DiffUtil.ItemCallback<FolderWithNoteCount>() {
    override fun areItemsTheSame(oldItem: FolderWithNoteCount, newItem: FolderWithNoteCount): Boolean {
        return oldItem.folder.id == newItem.folder.id
    }

    override fun areContentsTheSame(oldItem: FolderWithNoteCount, newItem: FolderWithNoteCount): Boolean {
        return oldItem == newItem
    }
}