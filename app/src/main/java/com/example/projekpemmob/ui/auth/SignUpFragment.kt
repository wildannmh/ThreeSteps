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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etFullName.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val pass = binding.etPassword.text?.toString().orEmpty()

            if (name.isEmpty()) { Snackbar.make(view, "Nama wajib diisi", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { Snackbar.make(view, "Email tidak valid", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            if (pass.length < 6) { Snackbar.make(view, "Password minimal 6 karakter", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }

            setLoading(true)
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { t ->
                    if (!t.isSuccessful) {
                        setLoading(false)
                        Snackbar.make(view, t.exception?.localizedMessage ?: "Pendaftaran gagal", Snackbar.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser
                    if (user == null) {
                        setLoading(false)
                        Snackbar.make(view, "User tidak tersedia setelah pendaftaran", Snackbar.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    // 1) Simpan ke Firebase Auth (displayName)
                    val profileReq = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user.updateProfile(profileReq)
                        .addOnCompleteListener {
                            // 2) (Opsional) Simpan juga ke Firestore
                            val data = hashMapOf(
                                "fullName" to name,
                                "email" to email,
                                "role" to "buyer",                 // default role
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            Firebase.firestore.collection("users")
                                .document(user.uid)
                                .set(data)
                                .addOnCompleteListener {
                                    // Selesai -> navigasi ke Home dan hapus Onboarding dari back stack
                                    setLoading(false)
                                    val opts = NavOptions.Builder()
                                        .setPopUpTo(R.id.onboardingFragment, /*inclusive=*/true)
                                        .build()
                                    findNavController().navigate(SignUpFragmentDirections.actionSignUpToHome(), opts)
                                }
                        }
                }
        }

        binding.btnGetStarted.setOnClickListener {
            findNavController().navigate(SignUpFragmentDirections.actionSignUpToSignIn())
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignUp.isEnabled = !loading
        binding.btnSignUp.text = if (loading) "Creating..." else "Sign Up"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}