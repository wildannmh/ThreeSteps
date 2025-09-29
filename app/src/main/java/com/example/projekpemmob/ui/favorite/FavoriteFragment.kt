package com.example.projekpemmob.ui.favorite

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentFavoriteBinding
import com.example.projekpemmob.ui.home.adapter.ProductCardItem
import com.example.projekpemmob.ui.home.adapter.ProductCardNetAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FavoriteFragment : Fragment(R.layout.fragment_favorite) {
    private var _b: FragmentFavoriteBinding? = null
    private val b get() = _b!!

    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var adapter: ProductCardNetAdapter
    private var favReg: ListenerRegistration? = null
    private var cartBadgeReg: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentFavoriteBinding.bind(view)

        // Card biasa; tekan heart_filled untuk hapus favorite
        adapter = ProductCardNetAdapter(
            onCardClick = { item ->
                val action = FavoriteFragmentDirections.actionFavoriteToDetails(item.id)
                findNavController().navigate(action)
            },
            onFavToggle = { item -> removeFavorite(item.id) },
            fullWidth = true
        )
        b.rvFav.layoutManager = LinearLayoutManager(requireContext())
        b.rvFav.adapter = adapter

        // Dengarkan perubahan daftar favorit
        listenFavorites()
    }

    private fun listenFavorites() {
        val user = auth.currentUser
        if (user == null) {
            b.tvEmpty.visibility = View.VISIBLE
            adapter.submitList(emptyList())
            return
        }
        favReg?.remove()
        favReg = db.collection("users").document(user.uid).collection("favorites")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { qs, e ->
                if (e != null) {
                    Snackbar.make(b.root, e.localizedMessage ?: "Gagal memuat favorit", Snackbar.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val items = qs?.documents?.map { d ->
                    ProductCardItem(
                        id = d.id, // docId = productId
                        name = d.getString("name").orEmpty(),
                        minPrice = d.getDouble("price") ?: 0.0,
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
                        isFavorite = true
                    )
                }.orEmpty()
                b.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(items)
            }
    }

    private fun removeFavorite(productId: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).collection("favorites").document(productId)
            .delete()
            .addOnFailureListener {
                Snackbar.make(b.root, it.localizedMessage ?: "Gagal menghapus favorit", Snackbar.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favReg?.remove(); favReg = null
        cartBadgeReg?.remove(); cartBadgeReg = null
        _b = null
    }
}