package com.example.projekpemmob.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.projekpemmob.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<EditProfileFragmentArgs>()
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var originalSoftInputMode: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Tampilkan Email (dari FirebaseAuth, read-only)
        binding.tvEmail.text = auth.currentUser?.email ?: "N/A"

        // Prefill
        binding.etDisplayName.setText(args.name)
        binding.etPhone.setText(args.phone)
        binding.etAddress.setText(args.address)

        // Toolbar back
        binding.topAppBar.setNavigationOnClickListener { findNavController().popBackStack() }

        // Tombol Ubah Kata Sandi (btnChangePassword) telah dihapus dari XML, jadi tidak ada listener di sini.

        // Save
        binding.btnSave.setOnClickListener {
            val newName = binding.etDisplayName.text?.toString()?.trim().orEmpty()
            val newPhone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val newAddress = binding.etAddress.text?.toString()?.trim().orEmpty()

            // Validate
            if (newPhone.isNotBlank() && !newPhone.matches(Regex("^[0-9+()\\-\\s]{8,}$"))) {
                Snackbar.make(view, "No. HP tidak valid", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (newAddress.length > 200) {
                Snackbar.make(view, "Alamat terlalu panjang", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener
            }

            setLoading(true)

            val user = auth.currentUser
            if (user == null) {
                setLoading(false)
                Snackbar.make(view, "Sesi berakhir, silakan login ulang", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 1) Update displayName di Firebase Auth (jika berubah)
            val updateAuth = if (newName != args.name) {
                val req = UserProfileChangeRequest.Builder().setDisplayName(newName.ifBlank { null }).build()
                user.updateProfile(req)
            } else com.google.android.gms.tasks.Tasks.forResult<Void>(null)

            // 2) Merge ke Firestore users/{uid}
            updateAuth.onSuccessTask {
                val data = hashMapOf(
                    "fullName" to newName,
                    "phone" to newPhone,
                    "address" to newAddress,
                    "email" to (user.email ?: ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                Firebase.firestore.collection("users").document(user.uid)
                    .set(data, SetOptions.merge())
            }.addOnCompleteListener { t ->
                setLoading(false)
                if (t.isSuccessful) {
                    // Kirim sinyal ke Profile untuk refresh
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("profile_updated", true)
                    hideKeyboard()
                    findNavController().popBackStack()
                } else {
                    Snackbar.make(view, t.exception?.localizedMessage ?: "Gagal menyimpan profil", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etDisplayName.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        // Pastikan layout menyesuaikan saat keyboard muncul
        val window = requireActivity().window
        originalSoftInputMode = window.attributes.softInputMode
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onPause() {
        super.onPause()
        // Pulihkan
        originalSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}