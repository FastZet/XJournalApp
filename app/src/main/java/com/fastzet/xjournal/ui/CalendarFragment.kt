package com.fastzet.xjournal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fastzet.xjournal.R
import com.fastzet.xjournal.databinding.FragmentCalendarBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch

class CalendarFragment : Fragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JournalViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDatePicker()
    }

    private fun setupDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        binding.calendarButton.setOnClickListener {
            datePicker.show(childFragmentManager, "DATE_PICKER_TAG")
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = selection ?: return@addOnPositiveButtonClickListener
            val year = MaterialDatePicker.todayInUtcMilliseconds().toLocalDate(selectedDate).year
            val month = MaterialDatePicker.todayInUtcMilliseconds().toLocalDate(selectedDate).monthValue
            val day = MaterialDatePicker.todayInUtcMilliseconds().toLocalDate(selectedDate).dayOfMonth
            navigateToEntriesList(year, month, day)
        }
    }

    private fun navigateToEntriesList(year: Int, month: Int, day: Int) {
        val action = CalendarFragmentDirections.actionCalendarToEntriesList(year, month, day)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
