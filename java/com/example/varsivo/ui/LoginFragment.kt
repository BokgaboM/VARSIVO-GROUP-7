package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.data.LoginOutcome
import com.example.varsivo.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Sign in with Student ID + password. The password is checked against its PBKDF2 hash. */
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLoginBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.languageValue.text = userStore.language

        // Clear the error as soon as the student starts typing again.
        binding.studentIdInput.onTextChanged { clearError() }
        binding.passwordInput.onTextChanged { clearError() }

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.passwordInput.onImeAction(EditorInfo.IME_ACTION_DONE) { attemptLogin() }
        binding.signUpText.setOnClickListener { findNavController().navigate(R.id.registerFragment) }
        binding.forgotPasswordText.setOnClickListener { showForgotPasswordDialog() }
        binding.googleButton.setOnClickListener { showGoogleDialog() }
    }

    private fun attemptLogin() {
        val studentId = binding.studentIdInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()

        // Input validation: never crash on empty input, tell the student what is missing.
        if (studentId.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_enter_id_and_password))
            return
        }

        binding.root.hideKeyboard()
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = authRepository.login(studentId, password)
            if (!hasView) return@launch
            setLoading(false)
            when (outcome) {
                LoginOutcome.SUCCESS -> {
                    Log.i(TAG, "Login OK, opening Home")
                    findNavController().navigate(R.id.action_login_to_home)
                }
                LoginOutcome.INVALID_CREDENTIALS -> showError(getString(R.string.error_incorrect_login))
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loginProgress.showIf(loading)
        binding.loginButton.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.show()
    }

    private fun clearError() {
        if (hasView) binding.errorText.hide()
    }

    /** Mock flow — a real reset needs an email/SMS service (see README, "Known limitations"). */
    private fun showForgotPasswordDialog() {
        val input = EditText(requireContext()).apply {
            setText(binding.studentIdInput.text)
            hint = getString(R.string.student_id)
            setSingleLine()
        }
        val padding = resources.getDimensionPixelSize(R.dimen.dialog_field_padding)
        val container = FrameLayout(requireContext()).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_password_title)
            .setMessage(R.string.reset_password_message)
            .setView(container)
            .setPositiveButton(R.string.send_reset_link) { _, _ ->
                Snackbar.make(binding.root, R.string.reset_requested, Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Demo flow — real Google Sign-In needs an OAuth client ID (see README). */
    private fun showGoogleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.continue_with_google)
            .setMessage(R.string.google_demo_message)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                // Demo only: no real Google account is checked.
                Log.i(TAG, "Demo Google sign-in, opening Home")
                if (userStore.getCredentials() != null) userStore.isLoggedIn = true
                findNavController().navigate(R.id.action_login_to_home)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private companion object {
        const val TAG = "LoginFragment"
    }
}
