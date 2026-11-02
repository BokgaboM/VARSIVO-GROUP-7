package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.varsivo.R
import com.example.varsivo.api.RetrofitClient
import com.example.varsivo.api.University
import com.example.varsivo.data.ApplicationType
import com.example.varsivo.databinding.FragmentUniversitiesBinding
import com.example.varsivo.ui.adapter.UniversityAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** "Find Your University": searchable list from the public Universities REST API. */
class UniversitiesFragment : BaseFragment<FragmentUniversitiesBinding>() {

    private val adapter = UniversityAdapter(
        onApplyClick = { university ->
            findNavController().navigate(
                R.id.applicationFormFragment,
                bundleOf(
                    ApplicationFormFragment.ARG_TYPE to ApplicationType.UNIVERSITY.name,
                    ApplicationFormFragment.ARG_TARGET to university.name
                )
            )
        },
        onMapClick = { university -> openMap(university.name) }
    )

    private var universities: List<University> = emptyList()
    private var isLoading = true
    private var loadFailed = false
    private var query = ""

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUniversitiesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.universityList.layoutManager = LinearLayoutManager(requireContext())
        binding.universityList.adapter = adapter

        binding.searchInput.onTextChanged { text ->
            query = text
            render()
        }
        binding.mapButton.setOnClickListener { openMap(query.trim()) }
        binding.retryButton.setOnClickListener { loadUniversities() }

        loadUniversities()
    }

    /** GET https://universities.hipolabs.com/search?country=South Africa */
    private fun loadUniversities() {
        isLoading = true
        loadFailed = false
        render()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                universities = RetrofitClient.universityApi.getUniversities("South Africa")
                    .filter { !it.name.isNullOrBlank() }
                Log.i(TAG, "Loaded ${universities.size} universities")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not load universities", e)
                loadFailed = true
            }
            isLoading = false
            if (hasView) render()
        }
    }

    private fun render() {
        if (!hasView) return
        val filtered = universities.filter { it.name.contains(query.trim(), ignoreCase = true) }
        adapter.submit(filtered)

        binding.loadingContainer.showIf(isLoading)
        val showError = !isLoading && loadFailed
        val showEmpty = !isLoading && !loadFailed && filtered.isEmpty()
        binding.stateContainer.showIf(showError || showEmpty)
        binding.retryButton.showIf(showError)
        when {
            showError -> {
                binding.stateTitle.setText(R.string.something_went_wrong)
                binding.stateText.setText(R.string.error_load_universities)
            }
            showEmpty -> {
                binding.stateTitle.setText(R.string.no_universities_found)
                binding.stateText.setText(R.string.try_different_name)
            }
        }
        binding.universityList.showIf(!isLoading && !loadFailed && filtered.isNotEmpty())
        binding.listTitle.setText(if (query.isBlank()) R.string.universities_in_sa else R.string.search_results)
    }

    private fun openMap(name: String) {
        findNavController().navigate(
            R.id.universityMapFragment,
            bundleOf(UniversityMapFragment.ARG_UNIVERSITY to name)
        )
    }

    private companion object {
        const val TAG = "UniversitiesFragment"
    }
}
