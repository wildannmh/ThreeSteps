package com.example.projekpemmob.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val session by lazy { SessionManager(requireContext().applicationContext) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Jika sudah login/guest, langsung ke Home dan hapus Onboarding dari back stack
        if (FirebaseAuth.getInstance().currentUser != null || session.isGuest()) {
            val opts = NavOptions.Builder()
                .setPopUpTo(R.id.onboardingFragment, /*inclusive=*/true)
                .build()
            findNavController().navigate(OnboardingFragmentDirections.actionOnboardingToHome(), opts)
            return
        }

        binding.btnGetStarted.setOnClickListener {
            findNavController().navigate(OnboardingFragmentDirections.actionOnboardingToSignIn())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}