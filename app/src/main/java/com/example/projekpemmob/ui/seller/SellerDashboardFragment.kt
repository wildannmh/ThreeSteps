package com.example.projekpemmob.ui.seller

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentSellerDashboardBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SellerDashboardFragment : Fragment(R.layout.fragment_seller_dashboard) {
    private var _b: FragmentSellerDashboardBinding? = null
    private val b get() = _b!!

    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val adapter by lazy {
        SellerProductsAdapter { productId ->
            // Buka edit produk existing
            val action = SellerDashboardFragmentDirections.actionSellerDashboardToEditProduct(productId)
            findNavController().navigate(action)
        }
    }

    private var registration: ListenerRegistration? = null
    private var allItems: List<SellerProductItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentSellerDashboardBinding.bind(view)

        // Back
        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Recycler
        b.rvMyProducts.layoutManager = LinearLayoutManager(requireContext())
        b.rvMyProducts.adapter = adapter

        // New product
        b.btnNewProduct.setOnClickListener {
            // Buka edit produk dalam mode create (productId kosong)
            findNavController().navigate(R.id.editProductFragment)
        }

        // Search (client-side filter)
        b.etSearch.doAfterTextChanged { text ->
            applyFilter(text?.toString().orEmpty())
        }

        if (uid.isBlank()) {
            Snackbar.make(b.root, "Sesi berakhir. Silakan login ulang.", Snackbar.LENGTH_LONG).show()
            return
        }

        listenMyProducts()
    }

    private fun listenMyProducts() {
        registration?.remove()
        registration = Firebase.firestore.collection("products")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    Snackbar.make(b.root, err.localizedMessage ?: "Gagal memuat produk", Snackbar.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val items = qs?.documents?.map { d ->
                    SellerProductItem(
                        id = d.id,
                        name = d.getString("name").orEmpty(),
                        minPrice = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty()
                    )
                }.orEmpty()
                allItems = items
                applyFilter(b.etSearch.text?.toString().orEmpty())
            }
    }

    private fun applyFilter(query: String) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) {
            allItems
        } else {
            allItems.filter { it.name.contains(q, ignoreCase = true) }
        }
        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registration?.remove()
        registration = null
        _b = null
    }
}