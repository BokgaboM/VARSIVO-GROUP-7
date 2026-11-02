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
import com.example.varsivo.api.Bursary
import com.example.varsivo.api.RetrofitClient
import com.example.varsivo.databinding.FragmentBursariesBinding
import com.example.varsivo.ui.adapter.BursaryAdapter
import com.example.varsivo.util.BursaryFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Lists bursaries. The three wireframe bursaries are always shown; the ones returned by the
 * hosted REST API (GET /bursaries) appear under "View more" or as soon as the student searches.
 */
class BursariesFragment : BaseFragment<FragmentBursariesBinding>() {

    private val adapter = BursaryAdapter(onBursaryClick = ::openDetails)

    private var apiBursaries: List<Bursary> = emptyList()
    private var isLoading = true
    private var errorMessage: String? = null
    private var showAll = false
    private var query = ""

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentBursariesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.bursaryList.layoutManager = LinearLayoutManager(requireContext())
        binding.bursaryList.adapter = adapter

        binding.searchBar.setup(hint = getString(R.string.search_bursaries_hint)) { text ->
            query = text
            render()
        }
        arguments?.getString(ARG_QUERY)?.takeIf { it.isNotBlank() }?.let {
            binding.searchBar.searchInput.setText(it) // triggers the watcher, which renders
        }

        binding.viewMoreButton.setOnClickListener {
            showAll = !showAll
            render()
        }
        binding.retryButton.setOnClickListener { loadBursaries() }

        loadBursaries()
        render()
    }

    /** GET /bursaries from the hosted REST API. */
    private fun loadBursaries() {
        isLoading = true
        errorMessage = null
        render()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = RetrofitClient.bursaryApi.getBursaries().map { it.sanitised() }
                Log.i(TAG, "Loaded ${result.size} bursaries from the API")
                apiBursaries = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not load bursaries", e)
                errorMessage = getString(R.string.error_load_bursaries)
            }
            isLoading = false
            if (hasView) render()
        }
    }

    private fun render() {
        if (!hasView) return
        val searching = query.isNotBlank()
        val visible = BursaryFilter.visibleBursaries(apiBursaries, showAll, query)
        adapter.submit(visible)

        val wantsApiData = showAll || searching
        val showProgress = isLoading && wantsApiData
        val error = errorMessage
        val showError = (error != null) && wantsApiData
        val showEmpty = !showProgress && !showError && visible.isEmpty()

        binding.progress.showIf(showProgress)
        binding.stateContainer.showIf(showError || showEmpty)
        binding.retryButton.showIf(showError)
        binding.stateText.text = when {
            showError -> error
            showEmpty -> getString(R.string.no_bursaries_found, query)
            else -> ""
        }
        binding.bursaryList.showIf(!showProgress && !showError && visible.isNotEmpty())

        binding.viewMoreButton.showIf(!searching && !isLoading && (apiBursaries.isNotEmpty() || errorMessage != null))
        binding.viewMoreButton.setText(if (showAll) R.string.view_less else R.string.view_more_bursaries)
    }

    private fun openDetails(bursary: Bursary) {
        Log.d(TAG, "Opening details for ${bursary.name}")
        val args = Bundle().apply {
            putString(BursaryDetailsFragment.ARG_NAME, bursary.name)
            putString(BursaryDetailsFragment.ARG_DESCRIPTION, bursary.description)
            putString(BursaryDetailsFragment.ARG_PROVIDER, bursary.provider)
            putString(BursaryDetailsFragment.ARG_ELIGIBILITY, bursary.eligibility)
            putString(BursaryDetailsFragment.ARG_CLOSING_DATE, bursary.closingDate)
        }
        findNavController().navigate(R.id.bursaryDetailsFragment, args)
    }

    companion object {
        private const val TAG = "BursariesFragment"
        const val ARG_QUERY = "query"
    }
}
