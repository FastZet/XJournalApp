package com.fastzet.xjournal.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fastzet.xjournal.R
import com.fastzet.xjournal.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: JournalViewModel by viewModels {
        JournalViewModel.Factory(
            repository = JournalRepository(
                context = applicationContext,
                database = JournalDatabase.getDatabase(applicationContext),
                encryption = JournalEncryption(applicationContext)
            ),
            context = applicationContext
        )
    }
    private lateinit var entriesAdapter: JournalEntriesAdapter
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showExportDialog()
        } else {
            Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show()
        }
    }

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
        binding.fabSync.setOnClickListener {
            viewModel.syncEntries()
        }
        binding.fabExport.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                showExportDialog()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
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

    private fun showExportDialog() {
        val defaultFileName = "journal_backup_${System.currentTimeMillis()}.dat"
        val defaultFilePath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), defaultFileName).absolutePath

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.export_title)
            .setMessage(R.string.export_message)
            .setPositiveButton(R.string.export) { _, _ ->
                viewModel.exportEntries(defaultFilePath)
                Toast.makeText(this, "Exported to $defaultFilePath", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.choose_file) { _, _ ->
                // Implement file picker here if needed
            }
            .setInput(null, defaultFilePath) { input, _ ->
                viewModel.exportEntries(input.toString())
                Toast.makeText(this, "Exported to ${input.toString()}", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
