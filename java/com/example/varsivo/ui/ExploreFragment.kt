package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.varsivo.R
import com.example.varsivo.databinding.FragmentExploreBinding
import com.example.varsivo.databinding.ItemCareerBinding
import com.example.varsivo.databinding.ItemInstitutionBinding
import com.example.varsivo.databinding.ItemSubjectMarkBinding
import com.example.varsivo.util.ApsCalculator
import com.example.varsivo.util.ExploreData
import com.example.varsivo.util.Validators
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText

/**
 * Explore: an APS (Admission Point Score) calculator that tells the student which institutions
 * they currently qualify for, plus a short list of popular careers.
 * The maths lives in [ApsCalculator] so it can be unit tested.
 */
class ExploreFragment : BaseFragment<FragmentExploreBinding>() {

    private val subjectInputs = mutableListOf<TextInputEditText>()
    private var apsScore: Int? = null
    private var subjectsUsedForScore = ApsCalculator.MAX_SUBJECTS
    private var showAllInstitutions = false
    private var showAllCareers = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentExploreBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        repeat(INITIAL_SUBJECTS) { addSubjectField() }

        binding.toggleCalculatorButton.setOnClickListener {
            val show = binding.calculatorCard.visibility != View.VISIBLE
            binding.calculatorCard.showIf(show)
            binding.toggleCalculatorButton.setText(if (show) R.string.hide_calculator else R.string.calculate_aps)
        }
        binding.addSubjectButton.setOnClickListener { addSubjectField() }
        binding.calculateButton.setOnClickListener { calculate() }

        binding.viewMoreInstitutionsButton.setOnClickListener {
            showAllInstitutions = !showAllInstitutions
            renderInstitutions()
        }
        binding.viewMoreCareersButton.setOnClickListener {
            showAllCareers = !showAllCareers
            renderCareers()
        }

        renderInstitutions()
        renderCareers()
    }

    private fun addSubjectField() {
        if (subjectInputs.size >= ApsCalculator.MAX_SUBJECTS) return
        val row = ItemSubjectMarkBinding.inflate(layoutInflater, binding.subjectContainer, false)
        row.subjectLayout.hint = getString(R.string.subject_mark_hint, subjectInputs.size + 1)
        row.subjectInput.onTextChanged { row.subjectLayout.error = null }
        subjectInputs.add(row.subjectInput)
        binding.subjectContainer.addView(row.root)
        binding.addSubjectButton.showIf(subjectInputs.size < ApsCalculator.MAX_SUBJECTS)
        binding.subjectCountText.text =
            getString(R.string.subjects_added, subjectInputs.size, ApsCalculator.MAX_SUBJECTS)
    }

    private fun calculate() {
        binding.calculatorMessage.hide()
        val marks = mutableListOf<Int>()
        var valid = true

        subjectInputs.forEachIndexed { index, input ->
            val layout = binding.subjectContainer.getChildAt(index) as com.google.android.material.textfield.TextInputLayout
            val mark = input.text?.toString()?.toIntOrNull()
            when {
                mark == null -> {
                    layout.error = getString(R.string.error_enter_mark)
                    valid = false
                }
                !Validators.isValidMark(mark) -> {
                    layout.error = getString(R.string.error_mark_range)
                    valid = false
                }
                else -> marks.add(mark)
            }
        }
        if (!valid) {
            binding.calculatorMessage.text = getString(R.string.error_fix_marks)
            binding.calculatorMessage.show()
            return
        }

        val score = ApsCalculator.calculate(marks) ?: return
        Log.i(TAG, "APS calculated: $score from ${marks.size} subject(s)")
        apsScore = score
        subjectsUsedForScore = marks.size

        binding.apsScoreText.text = getString(R.string.your_score, score)
        binding.apsNoteText.showIf(marks.size < ApsCalculator.MAX_SUBJECTS)
        binding.apsNoteText.text = getString(R.string.estimated_from, marks.size)
        binding.calculatorCard.hide()
        binding.toggleCalculatorButton.setText(R.string.calculate_aps)
        renderInstitutions()
    }

    private fun renderInstitutions() {
        val pool = ExploreData.institutions
        val shown = if (showAllInstitutions) pool else pool.take(INITIAL_INSTITUTIONS)
        binding.institutionContainer.removeAllViews()
        shown.forEach { institution ->
            val row = ItemInstitutionBinding.inflate(layoutInflater, binding.institutionContainer, false)
            row.institutionNameText.text = institution.name
            row.minApsText.text = getString(R.string.min_aps, institution.minAps)
            val score = apsScore
            if (score != null) {
                val eligible = ApsCalculator.isEligible(score, institution.minAps)
                row.matchChip.setText(if (eligible) R.string.match else R.string.below_min)
                row.matchChip.setBackgroundResource(if (eligible) R.drawable.bg_chip_match else R.drawable.bg_chip_below)
                row.matchChip.show()
            }
            binding.institutionContainer.addView(row.root)
        }
        binding.viewMoreInstitutionsButton.showIf(pool.size > INITIAL_INSTITUTIONS)
        binding.viewMoreInstitutionsButton.setText(
            if (showAllInstitutions) R.string.view_less else R.string.view_more_institutions
        )
    }

    private fun renderCareers() {
        val pool = ExploreData.careers
        val shown = if (showAllCareers) pool else pool.take(INITIAL_CAREERS)
        binding.careerContainer.removeAllViews()
        shown.forEach { career ->
            val row = ItemCareerBinding.inflate(layoutInflater, binding.careerContainer, false)
            row.careerText.text = career
            binding.careerContainer.addView(row.root)
        }
        binding.viewMoreCareersButton.showIf(pool.size > INITIAL_CAREERS)
        binding.viewMoreCareersButton.setText(if (showAllCareers) R.string.view_less else R.string.view_more_careers)
    }

    private companion object {
        const val TAG = "ExploreFragment"
        const val INITIAL_SUBJECTS = 3
        const val INITIAL_INSTITUTIONS = 2
        const val INITIAL_CAREERS = 3
    }
}
