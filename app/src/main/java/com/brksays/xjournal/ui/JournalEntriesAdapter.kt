package com.brksays.xjournal.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brksays.xjournal.databinding.ItemJournalEntryBinding
import com.brksays.xjournal.security.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalEntriesAdapter(
    private val onEntryClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalEntriesAdapter.EntryViewHolder>(EntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntryViewHolder(binding, onEntryClick)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EntryViewHolder(
        private val binding: ItemJournalEntryBinding,
        private val onEntryClick: (JournalEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(entry: JournalEntry) {
            binding.apply {
                titleText.text = entry.title
                // Show preview of content (first 100 characters)
                previewText.text = entry.content.take(100).let {
                    if (entry.content.length > 100) "$it..." else it
                }
                dateText.text = dateFormat.format(Date(entry.timestamp * 1000))
                syncStatusIcon.setImageResource(
                    when (entry.syncStatus) {
                        SyncStatus.SYNCED -> android.R.drawable.ic_menu_upload
                        SyncStatus.NOT_SYNCED -> android.R.drawable.ic_menu_save
                        SyncStatus.PENDING_SYNC -> android.R.drawable.ic_menu_rotate
                        SyncStatus.SYNC_ERROR -> android.R.drawable.ic_menu_report_image
                    }
                )

                root.setOnClickListener { onEntryClick(entry) }
            }
        }
    }

    private class EntryDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}
