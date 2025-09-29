package com.example.projekpemmob.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.core.requireLogin
import com.example.projekpemmob.databinding.FragmentHomeBinding
import com.example.projekpemmob.ui.common.HeaderViewModel
import com.example.projekpemmob.ui.common.LeftAction
import com.example.projekpemmob.ui.common.HeaderConfig
import com.example.projekpemmob.ui.home.adapter.ProductCardItem
import com.example.projekpemmob.ui.home.adapter.ProductCardNetAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val headerVM: HeaderViewModel by activityViewModels()

    private val session by lazy { SessionManager(requireContext().applicationContext) }
    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var popularAdapter: ProductCardNetAdapter
    private lateinit var newAdapter: ProductCardNetAdapter

    private var favReg: ListenerRegistration? = null
    private val favoriteIds = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)

        // Konfigurasi header global (kiri = favorites, judul = Home, cart terlihat)
        headerVM.set(HeaderConfig(title = "Home", leftAction = LeftAction.FAVORITES, showCart = true))

        // Search
        val goToSearch = {
            val action = HomeFragmentDirections.actionHomeToSearch(initialQuery = "")
            findNavController().navigate(action)
        }
        binding.tilSearch.setStartIconOnClickListener { goToSearch() }
        binding.etSearch.setOnClickListener { goToSearch() }

        // Popular
        popularAdapter = ProductCardNetAdapter(
            onCardClick = { item -> findNavController().navigate(HomeFragmentDirections.actionHomeToDetails(productId = item.id)) },
            onFavToggle = { item -> toggleFavorite(item) }
        )
        binding.rvPopular.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPopular.adapter = popularAdapter

        // New arrivals
        newAdapter = ProductCardNetAdapter(
            onCardClick = { item -> findNavController().navigate(HomeFragmentDirections.actionHomeToDetails(productId = item.id)) },
            onFavToggle = { item -> toggleFavorite(item) }
        )
        binding.rvNew.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNew.adapter = newAdapter

        setupFavoritesListener()
        loadPopular()
        loadNewArrivals()
    }

    private fun toggleFavorite(item: ProductCardItem) {
        val user = auth.currentUser ?: return requireLogin(session) {}
        val docRef = db.collection("users").document(user.uid).collection("favorites").document(item.id)
        if (favoriteIds.contains(item.id)) {
            docRef.delete()
        } else {
            val data = mapOf(
                "productId" to item.id,
                "name" to item.name,
                "thumbnailUrl" to item.thumbnailUrl,
                "price" to item.minPrice,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            docRef.set(data)
        }
    }

    private fun setupFavoritesListener() {
        favoriteIds.clear()
        favReg?.remove()
        val user = auth.currentUser ?: return
        favReg = db.collection("users").document(user.uid).collection("favorites")
            .addSnapshotListener { qs, _ ->
                favoriteIds.clear()
                qs?.documents?.forEach { favoriteIds.add(it.id) }
                refreshAdapters()
            }
    }

    private fun refreshAdapters() {
        popularAdapter.submitList(popularAdapter.currentList.map { it.copy(isFavorite = favoriteIds.contains(it.id)) })
        newAdapter.submitList(newAdapter.currentList.map { it.copy(isFavorite = favoriteIds.contains(it.id)) })
    }

    private fun loadPopular() {
        db.collection("products")
            .orderBy("minPrice", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { qs ->
                val items = qs.documents.map { d ->
                    ProductCardItem(
                        id = d.id,
                        name = d.getString("name").orEmpty(),
                        minPrice = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
                        isFavorite = favoriteIds.contains(d.id)
                    )
                }
                popularAdapter.submitList(items)
            }
    }

    private fun loadNewArrivals() {
        db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { qs ->
                val items = qs.documents.map { d ->
                    ProductCardItem(
                        id = d.id,
                        name = d.getString("name").orEmpty(),
                        minPrice = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
                        isFavorite = favoriteIds.contains(d.id)
                    )
                }
                newAdapter.submitList(items)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favReg?.remove(); favReg = null
        _binding = null
    }
}