package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shows the logo for a moment, then goes to Home (already signed in) or Login. */
class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSplashBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The coroutine is cancelled automatically if the view is destroyed first.
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            val loggedIn = userStore.isLoggedIn
            Log.d(TAG, "Splash finished, loggedIn=$loggedIn")
            findNavController().navigate(
                if (loggedIn) R.id.action_splash_to_home else R.id.action_splash_to_login
            )
        }
    }

    private companion object {
        const val TAG = "SplashFragment"
        const val SPLASH_DELAY_MS = 1800L
    }
}
