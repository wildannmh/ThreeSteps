package com.example.projekpemmob.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val session by lazy { SessionManager(requireContext().applicationContext) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val pass = binding.etPassword.text?.toString().orEmpty()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Email tidak valid"; return@setOnClickListener
            }
            if (pass.length < 6) {
                binding.tilPassword.error = "Minimal 6 karakter"; return@setOnClickListener
            }

            setLoading(true)
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { t ->
                    setLoading(false)
                    if (t.isSuccessful) {
                        session.clearGuest()
                        val opts = NavOptions.Builder()
                            .setPopUpTo(R.id.onboardingFragment, /*inclusive=*/true)
                            .build()
                        findNavController().navigate(SignInFragmentDirections.actionSignInToHome(), opts)
                    } else {
                        Snackbar.make(view, t.exception?.localizedMessage ?: "Gagal masuk", Snackbar.LENGTH_LONG).show()
                    }
                }
        }

        binding.tvCreateAccount.setOnClickListener {
            findNavController().navigate(SignInFragmentDirections.actionSignInToSignUp())
        }
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(SignInFragmentDirections.actionSignInToForgot())
        }
        binding.btnGuest.setOnClickListener {
            session.signInAsGuest()
            val opts = NavOptions.Builder()
                .setPopUpTo(R.id.onboardingFragment, /*inclusive=*/true)
                .build()
            findNavController().navigate(SignInFragmentDirections.actionSignInToHome(), opts)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.btnSignIn.text = if (loading) "Signing in..." else "Sign In"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}