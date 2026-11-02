package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.data.ApplicationType
import com.example.varsivo.databinding.FragmentApplicationFormBinding
import com.example.varsivo.util.Validators
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * One form for both university and bursary applications (arguments decide which).
 * A valid submission is saved in the local SQLite database and synced to the REST API; the
 * student is then taken to the Applications tracker where it appears.
 */
class ApplicationFormFragment : BaseFragment<FragmentApplicationFormBinding>() {

    private val type: ApplicationType
        get() = runCatching { ApplicationType.valueOf(arguments?.getString(ARG_TYPE).orEmpty()) }
            .getOrDefault(ApplicationType.UNIVERSITY)

    private val target: String
        get() = arguments?.getString(ARG_TARGET).orEmpty()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentApplicationFormBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isBursary = type == ApplicationType.BURSARY
        binding.toolbar.setTitle(if (isBursary) R.string.bursary_application else R.string.university_application)
        binding.toolbar.setSubtitle(if (isBursary) R.string.bursary_application_subtitle else R.string.university_application_subtitle)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.applyingLabel.setText(if (isBursary) R.string.applying_for else R.string.applying_to)
        binding.targetNameText.text = target
        binding.footerText.setText(if (isBursary) R.string.bursary_form_footer else R.string.university_form_footer)
        binding.institutionLayout.showIf(isBursary)

        // Pre-fill what we already know about the student.
        binding.fullNameInput.setText(userStore.name)

        binding.fullNameInput.onTextChanged { binding.fullNameLayout.error = null }
        binding.emailInput.onTextChanged { binding.emailLayout.error = null }
        binding.institutionInput.onTextChanged { binding.institutionLayout.error = null }
        binding.courseInput.onTextChanged { binding.courseLayout.error = null }

        binding.submitButton.setOnClickListener { submit() }
        binding.courseInput.onImeAction(EditorInfo.IME_ACTION_DONE) { submit() }
    }

    private fun submit() {
        val fullName = binding.fullNameInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val institution = binding.institutionInput.text?.toString().orEmpty().trim()
        val course = binding.courseInput.text?.toString().orEmpty().trim()
        val isBursary = type == ApplicationType.BURSARY

        // Validate every field and mark each one that is wrong.
        var valid = true
        if (!Validators.isValidName(fullName)) {
            binding.fullNameLayout.error = getString(R.string.error_name_invalid); valid = false
        }
        if (!Validators.isValidEmail(email)) {
            binding.emailLayout.error = getString(R.string.error_email_invalid); valid = false
        }
        if (isBursary && institution.length < 2) {
            binding.institutionLayout.error = getString(R.string.error_field_required); valid = false
        }
        if (course.length < 2) {
            binding.courseLayout.error = getString(R.string.error_field_required); valid = false
        }
        if (!valid) {
            Log.d(TAG, "Application form blocked by validation")
            return
        }

        binding.root.hideKeyboard()
        binding.submitButton.isEnabled = false

        // For a bursary the "institution" is the university the student will study at;
        // for a university application the institution is the university chosen earlier.
        val institutionName = if (isBursary) "$target ($institution)" else target

        viewLifecycleOwner.lifecycleScope.launch {
            val synced = applicationRepository.submit(type, institutionName, course, fullName, email)
            Log.i(TAG, "Application submitted (synced=$synced)")
            if (!hasView) return@launch
            Snackbar.make(
                binding.root,
                if (synced) R.string.application_submitted else R.string.application_saved_offline,
                Snackbar.LENGTH_LONG
            ).show()
            // Go to the tracker, keeping Home underneath so Back returns to Home.
            findNavController().navigate(
                R.id.applicationsTrackerFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).build()
            )
        }
    }

    companion object {
        private const val TAG = "ApplicationFormFragment"
        const val ARG_TYPE = "applicationType"
        const val ARG_TARGET = "applicationTarget"
    }
}
