package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.data.RegisterOutcome
import com.example.varsivo.databinding.FragmentRegisterBinding
import com.example.varsivo.util.Validators
import kotlinx.coroutines.launch

/** Creates an account. The password is salted + hashed (PBKDF2) before it is saved or uploaded. */
class RegisterFragment : BaseFragment<FragmentRegisterBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentRegisterBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.confirmPasswordInput.onImeAction(EditorInfo.IME_ACTION_DONE) { attemptRegister() }

        // Remove a field's error as soon as the student edits it.
        binding.nameInput.onTextChanged { binding.nameLayout.error = null }
        binding.studentIdInput.onTextChanged { binding.studentIdLayout.error = null }
        binding.passwordInput.onTextChanged { binding.passwordLayout.error = null }
        binding.confirmPasswordInput.onTextChanged { binding.confirmPasswordLayout.error = null }
    }

    private fun attemptRegister() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val school = binding.schoolInput.text?.toString().orEmpty().trim()
        val studentId = binding.studentIdInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmPasswordInput.text?.toString().orEmpty()

        if (!validate(name, studentId, password, confirm)) return

        binding.root.hideKeyboard()
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = authRepository.register(name, school, studentId, password)
            if (!hasView) return@launch
            setLoading(false)
            when (outcome) {
                RegisterOutcome.ALREADY_EXISTS -> {
                    binding.studentIdLayout.error = getString(R.string.error_student_id_taken)
                }
                RegisterOutcome.SAVED_AND_SYNCED -> onRegistered(R.string.registration_success_online)
                RegisterOutcome.SAVED_OFFLINE -> onRegistered(R.string.registration_success_offline)
            }
        }
    }

    /** Returns true when every field is valid; otherwise marks the bad fields and returns false. */
    private fun validate(name: String, studentId: String, password: String, confirm: String): Boolean {
        var valid = true
        if (!Validators.isValidName(name)) {
            binding.nameLayout.error = getString(R.string.error_name_invalid)
            valid = false
        }
        if (!Validators.isValidStudentId(studentId)) {
            binding.studentIdLayout.error = getString(R.string.error_student_id_invalid)
            valid = false
        }
        if (!Validators.isStrongPassword(password)) {
            binding.passwordLayout.error = getString(R.string.error_password_weak)
            valid = false
        }
        if (password != confirm) {
            binding.confirmPasswordLayout.error = getString(R.string.error_passwords_dont_match)
            valid = false
        }
        if (!binding.termsCheckbox.isChecked) {
            showMessage(getString(R.string.error_accept_terms), isError = true)
            valid = false
        } else {
            binding.messageText.hide()
        }
        if (!valid) Log.d(TAG, "Registration blocked by validation")
        return valid
    }

    private fun onRegistered(messageRes: Int) {
        Log.i(TAG, "Registration complete")
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
        findNavController().navigateUp() // back to the login screen
    }

    private fun setLoading(loading: Boolean) {
        binding.registerProgress.showIf(loading)
        binding.registerButton.isEnabled = !loading
    }

    private fun showMessage(message: String, isError: Boolean) {
        binding.messageText.text = message
        binding.messageText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isError) R.color.varsivo_error_light else R.color.varsivo_success_light
            )
        )
        binding.messageText.show()
    }

    private companion object {
        const val TAG = "RegisterFragment"
    }
}
