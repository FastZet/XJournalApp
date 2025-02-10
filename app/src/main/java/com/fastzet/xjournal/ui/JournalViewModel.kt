package com.fastzet.xjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fastzet.xjournal.data.JournalRepository
import com.fastzet.xjournal.security.JournalEntry
import com.fastzet.xjournal.security.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class JournalViewModel(
    private val repository: JournalRepository,
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState
    private var _selectedEntry: JournalEntry? = null
    val selectedEntry get() = _selectedEntry
    private var _currentJournalId: String = UUID.randomUUID().toString() // Default journal ID
    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            repository.getAllEntriesByJournal(_currentJournalId)
                .catch { exception ->
                    _uiState.value = JournalUiState.Error(
                        "Failed to load journal entries: ${exception.localizedMessage}"
                    )
                }
                .collect { entries ->
                    _uiState.value = JournalUiState.Success(entries = entries)
                }
        }
    }

    fun selectEntry(entry: JournalEntry) {
        _selectedEntry = entry
    }

    fun createNewEntry() {
        _selectedEntry = null
    }

    fun saveEntry(title: String, content: String) {
        viewModelScope.launch {
            try {
                val entry = _selectedEntry?.copy(
                    title = title.trim(),
                    content = content.trim()
                ) ?: JournalEntry(
                    title = title.trim(),
                    content = content.trim(),
                    timestamp = Instant.now().epochSecond,
                    journalId = _currentJournalId
                )
                repository.saveEntry(entry)
                // State will be automatically updated through the Flow in loadEntries
            } catch (e: Exception) {
                _uiState.value = JournalUiState.Error(
                    "Failed to save entry: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry)
                // State will be automatically updated through the Flow in loadEntries
            } catch (e: Exception) {
                _uiState.value = JournalUiState.Error(
                    "Failed to delete entry: ${e.localizedMessage}"
                )
            }
        }
    }

    fun setCurrentJournalId(journalId: String) {
        _currentJournalId = journalId
        loadEntries()
    }

    fun syncEntries() {
        viewModelScope.launch {
            repository.getUnsyncedEntriesByJournal(_currentJournalId).forEach { entry ->
                repository.syncEntry(entry)
            }
        }
    }

    fun exportEntries(filePath: String) {
        viewModelScope.launch {
            val success = repository.exportEntries(filePath)
            if (success) {
                _uiState.value = JournalUiState.Success(entries = (uiState.value as? JournalUiState.Success)?.entries ?: emptyList())
            } else {
                _uiState.value = JournalUiState.Error("Failed to export entries")
            }
        }
    }

    class Factory(private val repository: JournalRepository, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                return JournalViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
