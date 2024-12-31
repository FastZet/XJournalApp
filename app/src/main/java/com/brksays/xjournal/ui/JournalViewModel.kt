package com.brksays.xjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brksays.xjournal.data.JournalRepository
import com.brksays.xjournal.security.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState

    private var _selectedEntry: JournalEntry? = null
    val selectedEntry get() = _selectedEntry

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            repository.getAllEntries()
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
                    timestamp = Instant.now().epochSecond
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

    class Factory(private val repository: JournalRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
                return JournalViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
