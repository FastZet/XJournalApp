package com.brksays.xjournal.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.brksays.xjournal.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: JournalViewModel by viewModels()
    private lateinit var entriesAdapter: JournalEntriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFab()
        observeUiState()
    }

    private fun setupRecyclerView() {
        entriesAdapter = JournalEntriesAdapter { entry ->
            viewModel.selectEntry(entry)
            // Navigate to editor fragment
        }

        binding.entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = entriesAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewEntry.setOnClickListener {
            viewModel.createNewEntry()
            // Navigate to editor fragment
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is JournalUiState.Success -> {
                        entriesAdapter.submitList(state.entries)
                    }
                    is JournalUiState.Error -> {
                        // Show error state
                    }
                    JournalUiState.Loading -> {
                        // Show loading state
                    }
                }
            }
        }
    }
}
