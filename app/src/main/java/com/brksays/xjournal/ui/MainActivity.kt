package com.brksays.xjournal.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.brksays.xjournal.R
import com.brksays.xjournal.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: JournalViewModel by viewModels()
    private lateinit var entriesAdapter: JournalEntriesAdapter
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupRecyclerView()
        setupFab()
        observeUiState()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupRecyclerView() {
        entriesAdapter = JournalEntriesAdapter { entry ->
            viewModel.selectEntry(entry)
            findNavController(R.id.nav_host_fragment).navigate(R.id.action_entriesList_to_editor)
        }

        binding.entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = entriesAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewEntry.setOnClickListener {
            viewModel.createNewEntry()
            findNavController(R.id.nav_host_fragment).navigate(R.id.action_entriesList_to_editor)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUiState(state)
            }
        }
    }

    private fun updateUiState(state: JournalUiState) {
        binding.apply {
            progressBar.isVisible = state is JournalUiState.Loading
            entriesRecyclerView.isVisible = state is JournalUiState.Success
            
            when (state) {
                is JournalUiState.Success -> {
                    entriesAdapter.submitList(state.entries)
                    if (state.entries.isEmpty()) {
                        emptyStateLayout.isVisible = true
                        entriesRecyclerView.isVisible = false
                    } else {
                        emptyStateLayout.isVisible = false
                        entriesRecyclerView.isVisible = true
                    }
                }
                is JournalUiState.Error -> {
                    Snackbar.make(
                        binding.root,
                        state.message,
                        Snackbar.LENGTH_LONG
                    ).setAction("Retry") {
                        viewModel.retryLastAction()
                    }.show()
                }
                JournalUiState.Loading -> {
                    // Loading state is handled by progressBar visibility
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
