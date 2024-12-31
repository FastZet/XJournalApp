package com.brksays.xjournal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.brksays.xjournal.R
import com.brksays.xjournal.databinding.FragmentEntryEditorBinding
import kotlinx.coroutines.launch

class EntryEditorFragment : Fragment() {
    private var _binding: FragmentEntryEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JournalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntryEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        loadSelectedEntry()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.editorToolbar.apply {
            title = if (viewModel.selectedEntry != null) "Edit Entry" else "New Entry"
            
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_save -> {
                        saveEntry()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun loadSelectedEntry() {
        // Load the selected entry if we're editing
        viewModel.selectedEntry?.let { entry ->
            binding.titleEditText.setText(entry.title)
            binding.contentEditText.setText(entry.content)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is JournalUiState.Success -> {
                        // Handle success if needed
                    }
                    is JournalUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    is JournalUiState.Loading -> {
                        // Handle loading if needed
                    }
                }
            }
        }
    }

    private fun saveEntry() {
        val title = binding.titleEditText.text.toString().trim()
        val content = binding.contentEditText.text.toString().trim()

        if (title.isEmpty()) {
            binding.titleInputLayout.error = "Title cannot be empty"
            return
        }

        binding.titleInputLayout.error = null
        viewModel.saveEntry(title, content)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
