package com.example.varsivo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.databinding.FragmentHomeBinding
import com.example.varsivo.util.PartOfDay
import java.util.Calendar

/** Dashboard: greeting, search, "most urgent" banner and the four main sections. */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firstName = userStore.name.trim().substringBefore(" ").ifBlank { getString(R.string.student) }
        binding.greetingText.text = getString(R.string.greeting_hi, firstName)

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        binding.timeOfDayText.setText(
            when (PartOfDay.fromHour(hour)) {
                PartOfDay.MORNING -> R.string.good_morning
                PartOfDay.AFTERNOON -> R.string.good_afternoon
                PartOfDay.EVENING -> R.string.good_evening
            }
        )

        // Pressing the keyboard's Search key opens the bursaries list with the typed query.
        binding.searchBar.setup(
            hint = getString(R.string.search_bursaries_hint),
            onSearchKey = {
                val query = binding.searchBar.searchInput.text?.toString().orEmpty().trim()
                val args = Bundle().apply { putString(BursariesFragment.ARG_QUERY, query) }
                findNavController().navigate(R.id.bursariesFragment, args)
            },
        )

        binding.cardApplications.setOnClickListener {
            findNavController().navigate(R.id.applicationsTrackerFragment)
        }
        binding.cardBursaries.setOnClickListener { findNavController().navigate(R.id.bursariesFragment) }
        binding.cardExplore.setOnClickListener { findNavController().navigate(R.id.exploreFragment) }
        binding.cardUniAssist.setOnClickListener { findNavController().navigate(R.id.firstYearSupportFragment) }
    }
}
