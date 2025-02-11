package com.fastzet.xjournal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.fastzet.xjournal.R
import com.fastzet.xjournal.databinding.FragmentEntriesListBinding
import kotlinx.coroutines.launch

class EntriesListFragment : Fragment() {
    private var _binding: FragmentEntriesListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JournalViewModel by activityViewModels()
    private lateinit var entriesAdapter: JournalEntriesAdapter
    private val args: EntriesListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntriesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeUiState()
    }

    private fun setupRecyclerView() {
        entriesAdapter = JournalEntriesAdapter { entry ->
            viewModel.selectEntry(entry)
            findNavController().navigate(R.id.action_entriesList_to_editor)
        }
        binding.entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = entriesAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewEntry.setOnClickListener {
            viewModel.createNewEntry()
            findNavController().navigate(R.id.action_entriesList_to_editor)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is JournalUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val filteredEntries = state.entries.filter { entry ->
                            val entryDate = entry.timestamp * 1000
                            val entryYear = java.time.Instant.ofEpochMilli(entryDate).atZone(java.time.ZoneId.systemDefault()).year
                            val entryMonth = java.time.Instant.ofEpochMilli(entryDate).atZone(java.time.ZoneId.systemDefault()).monthValue
                            val entryDay = java.time.Instant.ofEpochMilli(entryDate).atZone(java.time.ZoneId.systemDefault()).dayOfMonth
                            entryYear == args.year && entryMonth == args.month && entryDay == args.day
                        }
                        entriesAdapter.submitList(filteredEntries)
                    }
                    is JournalUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        // Show error state
                    }
                    JournalUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
