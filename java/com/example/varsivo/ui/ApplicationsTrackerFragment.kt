package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.varsivo.R
import com.example.varsivo.data.TrackedApplication
import com.example.varsivo.databinding.FragmentApplicationsTrackerBinding
import com.example.varsivo.ui.adapter.ApplicationAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Shows the applications the student has submitted (from the local SQLite database).
 * Opening the screen also retries syncing anything still flagged "Offline will sync...".
 */
class ApplicationsTrackerFragment : BaseFragment<FragmentApplicationsTrackerBinding>() {

    private val adapter = ApplicationAdapter(
        onStatusClick = { app -> changeStatus(app) },
        onDeleteClick = { app -> confirmDelete(app) }
    )

    private var allApplications: List<TrackedApplication> = emptyList()
    private var query = ""

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentApplicationsTrackerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.applicationList.layoutManager = LinearLayoutManager(requireContext())
        binding.applicationList.adapter = adapter

        binding.searchBar.setup(hint = getString(R.string.search_applications_hint)) { text ->
            query = text
            render()
        }
        binding.browseButton.setOnClickListener { findNavController().navigate(R.id.universitiesFragment) }

        load(syncFirst = true)
    }

    private fun load(syncFirst: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allApplications = applicationRepository.getAll()
                if (hasView) render()
                if (syncFirst) {
                    // Retry anything that was saved while offline, then refresh the list.
                    applicationRepository.syncPending()
                    allApplications = applicationRepository.getAll()
                    if (hasView) render()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not load applications", e)
            }
        }
    }

    private fun render() {
        val filtered = allApplications.filter {
            it.institution.contains(query, ignoreCase = true) ||
                it.programme.contains(query, ignoreCase = true)
        }
        adapter.submit(filtered)
        binding.emptyText.showIf(filtered.isEmpty())
        binding.emptyText.text = if (allApplications.isEmpty()) {
            getString(R.string.no_applications_yet)
        } else {
            getString(R.string.no_applications_found, query)
        }
    }

    private fun changeStatus(app: TrackedApplication) {
        viewLifecycleOwner.lifecycleScope.launch {
            applicationRepository.cycleStatus(app)
            load()
        }
    }

    private fun confirmDelete(app: TrackedApplication) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_application)
            .setMessage(getString(R.string.delete_application_message, app.institution))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    applicationRepository.delete(app.id)
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private companion object {
        const val TAG = "ApplicationsTracker"
    }
}
