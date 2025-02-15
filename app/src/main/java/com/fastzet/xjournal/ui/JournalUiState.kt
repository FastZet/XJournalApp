package com.fastzet.xjournal.ui

import com.fastzet.xjournal.security.JournalEntry

sealed class JournalUiState {
    data object Loading : JournalUiState()
    
    data class Success(
        val entries: List<JournalEntry> = emptyList(),
        val isAddEntryDialogVisible: Boolean = false,
        val selectedEntry: JournalEntry? = null
    ) : JournalUiState()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : JournalUiState()
}
