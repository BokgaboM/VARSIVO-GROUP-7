package com.example.varsivo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.varsivo.VarsivoApplication
import com.example.varsivo.data.ApplicationRepository
import com.example.varsivo.data.AuthRepository
import com.example.varsivo.data.UserStore

/**
 * Base class for every screen. Handles the ViewBinding lifecycle (the binding is created in
 * onCreateView and cleared in onDestroyView so the Fragment never leaks its views), and gives
 * quick access to the shared data objects.
 *
 * Reference: Android Developers (2024) *View binding* https://developer.android.com/topic/libraries/view-binding
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    /** Only valid between onCreateView and onDestroyView. */
    protected val binding: VB
        get() = checkNotNull(_binding) { "Binding accessed outside the view lifecycle" }

    /** True while the view exists (used by async callbacks that may finish after the screen closed). */
    protected val hasView: Boolean
        get() = _binding != null

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected val userStore: UserStore
        get() = (requireActivity().application as VarsivoApplication).userStore

    protected val authRepository: AuthRepository
        get() = (requireActivity().application as VarsivoApplication).authRepository

    protected val applicationRepository: ApplicationRepository
        get() = (requireActivity().application as VarsivoApplication).applicationRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = inflateBinding(inflater, container)
        _binding = b
        return b.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
