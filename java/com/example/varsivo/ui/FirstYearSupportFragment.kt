package com.example.varsivo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.databinding.FragmentFirstYearSupportBinding
import com.example.varsivo.databinding.ItemDeadlineBinding
import com.example.varsivo.databinding.ItemTimetableEntryBinding

/**
 * UniAssist (premium tier): today's timetable and upcoming deadlines for first-year students.
 * The data below is placeholder content for the prototype; a later version will load it from
 * the student's real timetable / assignment endpoints.
 */
class FirstYearSupportFragment : BaseFragment<FragmentFirstYearSupportBinding>() {

    private data class TimetableEntry(val time: String, val subject: String)
    private data class Deadline(val title: String, val daysLeft: Int)

    private val timetable = listOf(
        TimetableEntry("09:00", "Data Structures"),
        TimetableEntry("14:00", "Software Eng")
    )

    private val deadlines = listOf(
        Deadline("Assignment 2", 2),
        Deadline("Maths test", 5),
        Deadline("Assignment 3", 10)
    )

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFirstYearSupportBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Prototype: always verified. Later this comes from the student-email verification status.
        val isVerified = true
        binding.lockedContainer.showIf(!isVerified)
        binding.contentScroll.showIf(isVerified)
        binding.verifiedBadge.showIf(isVerified)

        timetable.forEach { entry ->
            val row = ItemTimetableEntryBinding.inflate(layoutInflater, binding.timetableContainer, false)
            row.root.text = getString(R.string.timetable_entry, entry.time, entry.subject)
            binding.timetableContainer.addView(row.root)
        }

        deadlines.forEachIndexed { index, deadline ->
            val row = ItemDeadlineBinding.inflate(layoutInflater, binding.deadlineContainer, false)
            row.deadlineTitle.text = deadline.title
            row.deadlineDays.text = getString(R.string.days_left, deadline.daysLeft)
            row.deadlineDivider.showIf(index != deadlines.lastIndex)
            binding.deadlineContainer.addView(row.root)
        }
    }
}
