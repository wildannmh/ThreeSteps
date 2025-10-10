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

        // Tampilkan data di header
        binding.tvEmail.text = user.email ?: "(no email)"
        loadProfile() // Akan mengisi name dan tvDisplayNameValue

        // Tampilkan tombol Seller jika roles mengandung "seller"
        Firebase.firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val roles = (doc.get("roles") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val legacyRole = doc.getString("role")
                val isSeller = roles.contains("seller") || legacyRole == "seller"
                binding.btnSellerDashboard.visibility = if (isSeller) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.btnSellerDashboard.visibility = View.GONE
            }

        binding.btnSellerDashboard.setOnClickListener {
            findNavController().navigate(R.id.sellerDashboardFragment)
        }

        // Item "Edit Profil" (Informasi Profil & Pengiriman)
        binding.btnStartEdit.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileToEditProfile(
                name.ifBlank { "" },
                phone.ifBlank { "" },
                address.ifBlank { "" }
            )
            findNavController().navigate(action)
        }

        // Item "Pengaturan Akun" (btnSettings) telah dihapus dari XML, jadi tidak ada listener di sini.

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            session.clearGuest()

            val navGraphId = R.id.nav_graph

            val navOptions = NavOptions.Builder()
                .setPopUpTo(navGraphId, true)
                .build()

            findNavController().navigate(R.id.onboardingFragment, null, navOptions)
        }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        // Dari FirebaseAuth
        name = user.displayName.orEmpty()
        binding.tvDisplayNameValue.text = if (name.isBlank()) "Pengguna Baru" else name

        // Dari Firestore (ambil data untuk dikirim ke Edit Profile)
        Firebase.firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                phone = doc.getString("phone")?.trim().orEmpty()
                address = doc.getString("address")?.trim().orEmpty()
            }
            .addOnFailureListener {
                // Handle kegagalan, biarkan phone dan address tetap kosong
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}