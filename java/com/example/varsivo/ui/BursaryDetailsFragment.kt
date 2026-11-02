package com.example.varsivo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.data.ApplicationType
import com.example.varsivo.databinding.FragmentBursaryDetailsBinding
import com.example.varsivo.databinding.ItemRequirementBinding

/** Details of one bursary. Uses the API's text when there is any, otherwise sensible defaults. */
class BursaryDetailsFragment : BaseFragment<FragmentBursaryDetailsBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentBursaryDetailsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        val name = args?.getString(ARG_NAME).orEmpty()
        val description = args?.getString(ARG_DESCRIPTION).orEmpty()
        val provider = args?.getString(ARG_PROVIDER).orEmpty()
        val eligibility = args?.getString(ARG_ELIGIBILITY).orEmpty()
        val closingDate = args?.getString(ARG_CLOSING_DATE).orEmpty()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.bursaryNameText.text = name
        binding.providerText.text =
            if (provider.isNotBlank()) getString(R.string.provided_by, provider)
            else getString(R.string.funding_opportunity)
        binding.descriptionText.text =
            description.ifBlank { getString(R.string.default_bursary_description) }

        // Requirements: the API's eligibility text if present, else the default checklist.
        val requirements = if (eligibility.isNotBlank()) {
            listOf(eligibility)
        } else {
            resources.getStringArray(R.array.default_bursary_requirements).toList()
        }
        requirements.forEach { text ->
            val row = ItemRequirementBinding.inflate(layoutInflater, binding.requirementsContainer, false)
            row.requirementText.text = text
            binding.requirementsContainer.addView(row.root)
        }

        binding.closingDateText.text = getString(R.string.closing_date_label, closingDate.ifBlank { getString(R.string.tbc) })

        binding.applyButton.setOnClickListener {
            findNavController().navigate(
                R.id.applicationFormFragment,
                bundleOf(
                    ApplicationFormFragment.ARG_TYPE to ApplicationType.BURSARY.name,
                    ApplicationFormFragment.ARG_TARGET to name
                )
            )
        }
    }

    companion object {
        const val ARG_NAME = "bursaryName"
        const val ARG_DESCRIPTION = "bursaryDescription"
        const val ARG_PROVIDER = "bursaryProvider"
        const val ARG_ELIGIBILITY = "bursaryEligibility"
        const val ARG_CLOSING_DATE = "bursaryClosingDate"
    }
}
