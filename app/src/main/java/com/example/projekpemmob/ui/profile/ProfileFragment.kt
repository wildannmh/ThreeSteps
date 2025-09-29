package com.example.projekpemmob.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val session by lazy { SessionManager(requireContext().applicationContext) }

    private var name: String = ""
    private var phone: String = ""
    private var address: String = ""

    private var cartBadgeReg: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = auth.currentUser
        if (user == null) {
            binding.groupLogged.visibility = View.GONE
            binding.groupGuest.visibility = View.VISIBLE
            binding.btnGoToSignIn.setOnClickListener { findNavController().navigate(R.id.signInFragment) }
            return
        }

        // Logged-in
        binding.groupGuest.visibility = View.GONE
        binding.groupLogged.visibility = View.VISIBLE

        binding.tvEmail.text = user.email ?: "(no email)"
        loadProfile()

        // Tampilkan tombol Seller jika role == seller
        Firebase.firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "buyer"
                binding.btnSellerDashboard.visibility = if (role == "seller") View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.btnSellerDashboard.visibility = View.GONE
            }

        binding.btnSellerDashboard.setOnClickListener {
            findNavController().navigate(R.id.sellerDashboardFragment)
        }

        binding.btnStartEdit.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileToEditProfile(
                name.ifBlank { "" },
                phone.ifBlank { "" },
                address.ifBlank { "" }
            )
            findNavController().navigate(action)
        }

        // Tombol simpan/batal inline tidak dipakai
        binding.btnCancelEdit?.visibility = View.GONE
        binding.btnSaveProfile?.visibility = View.GONE

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            session.clearGuest()
            findNavController().navigate(
                R.id.onboardingFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.profileFragment, true)
                    .build()
            )
        }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        // Dari FirebaseAuth
        name = user.displayName.orEmpty()
        binding.tvDisplayNameValue.text = if (name.isBlank()) "-" else name

        // Dari Firestore
        Firebase.firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                phone = doc.getString("phone")?.trim().orEmpty()
                address = doc.getString("address")?.trim().orEmpty()
                binding.tvPhoneValue.text = if (phone.isBlank()) "-" else phone
                binding.tvAddressValue.text = if (address.isBlank()) "-" else address
            }
            .addOnFailureListener {
                binding.tvPhoneValue.text = "-"
                binding.tvAddressValue.text = "-"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cartBadgeReg?.remove()
        cartBadgeReg = null
        _binding = null
    }
}