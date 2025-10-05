package com.example.projekpemmob.ui.home

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.core.requireLogin
import com.example.projekpemmob.databinding.FragmentHomeBinding
import com.example.projekpemmob.ui.common.HeaderConfig
import com.example.projekpemmob.ui.common.HeaderViewModel
import com.example.projekpemmob.ui.common.LeftAction
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

    // Brand filter key (slug), null = All
    private var selectedBrandKey: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)

        headerVM.set(HeaderConfig(title = "Home", leftAction = LeftAction.FAVORITES, showCart = true))

        val goToSearch = {
            val action = HomeFragmentDirections.actionHomeToSearch(initialQuery = "")
            findNavController().navigate(action)
        }
        binding.tilSearch.setStartIconOnClickListener { goToSearch() }
        binding.etSearch.setOnClickListener { goToSearch() }

        setupBrandChips()

        // Best Sellers (adapter reuse)
        popularAdapter = ProductCardNetAdapter(
            onCardClick = { item -> findNavController().navigate(HomeFragmentDirections.actionHomeToDetails(productId = item.id)) },
            onFavToggle = { item -> toggleFavorite(item) }
        )
        binding.rvPopular.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPopular.adapter = popularAdapter

        // See all -> BestSellersFragment, bawa brand terpilih
        binding.tvPopularSeeAll.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeToBestSellers(brandKey = selectedBrandKey)
            findNavController().navigate(action)
        }

        // New arrivals
        newAdapter = ProductCardNetAdapter(
            onCardClick = { item -> findNavController().navigate(HomeFragmentDirections.actionHomeToDetails(productId = item.id)) },
            onFavToggle = { item -> toggleFavorite(item) }
        )
        binding.rvNew.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNew.adapter = newAdapter

        setupFavoritesListener()
        loadBestSellers()
        loadNewArrivals()
    }

    private fun setupBrandChips() {
        val names = resources.getStringArray(R.array.brand_list)
        val keys = resources.getStringArray(R.array.brand_keys)
        val group = binding.chipGroupBrand

        fun rebuildChips(selectedKey: String?) {
            group.removeAllViews()
            for (i in names.indices) {
                val label = names[i]
                val key = keys.getOrNull(i) ?: slugify(label)
                val checked = selectedKey == key
                val chip = buildBrandChip(label, key, checked)
                chip.setOnClickListener {
                    val isChecked = (selectedBrandKey == key)
                    selectedBrandKey = if (isChecked) null else key
                    rebuildChips(selectedBrandKey)
                    popularAdapter.submitList(emptyList())
                    newAdapter.submitList(emptyList())
                    loadBestSellers()
                    loadNewArrivals()
                }
                group.addView(chip)
            }
        }
        rebuildChips(selectedBrandKey)
    }

    private fun buildBrandChip(label: String, key: String, checked: Boolean): com.google.android.material.chip.Chip {
        val chip = com.google.android.material.chip.Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
            id = View.generateViewId()
            tag = key
            isCheckable = false
            isClickable = true
            isFocusable = true
            isCheckedIconVisible = false
            chipMinHeight = 48.dpToPx()
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val resName = "ic_brand_${key}"
        val iconId = chip.resources.getIdentifier(resName, "drawable", requireContext().packageName)
        chip.chipIcon = if (iconId != 0) requireContext().getDrawable(iconId) else null
        chip.isChipIconVisible = true
        chip.chipIconSize = 32.dpToPx()

        if (checked) {
            chip.text = label
            chip.setChipBackgroundColorResource(android.R.color.white) // Tetap putih
            chip.setChipStrokeColorResource(R.color.primary)           // Stroke warna utama
            chip.chipStrokeWidth = 2.dpToPx()                          // Atur ketebalan stroke
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary)) // Teks jadi warna utama
            chip.chipStartPadding = 24.dpToPx()
            chip.chipEndPadding = 24.dpToPx()
            chip.textStartPadding = 8.dpToPx()
            chip.textEndPadding = 8.dpToPx()
            chip.iconStartPadding = 4.dpToPx()
            chip.iconEndPadding = 4.dpToPx()
        } else {
            chip.text = null
            chip.setChipBackgroundColorResource(android.R.color.white)
            chip.setChipStrokeColorResource(R.color.chip_stroke_selector) // pakai warna grey/abu default
            chip.chipStrokeWidth = 1.dpToPx()
            chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            chip.chipIconSize = 32.dpToPx()
            chip.chipIconTint = null
            chip.chipStartPadding = 24.dpToPx()
            chip.chipEndPadding = 24.dpToPx()
            chip.textStartPadding = 0f
            chip.textEndPadding = 0f
            chip.iconStartPadding = 0f
            chip.iconEndPadding = 0f
        }
        return chip
    }

    private fun slugify(s: String): String = s.trim().lowercase().replace(Regex("\\s+"), "_")

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

    // Best Sellers: urut berdasarkan salesCount desc, lalu createdAt desc (tanpa filter isBestSeller)
    private fun loadBestSellers() {
        val brand = selectedBrandKey
        var q: Query = db.collection("products")
        if (brand != null) q = q.whereEqualTo("brandKey", brand)
        q = q
            .orderBy("salesCount", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)

        q.get()
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
            .addOnFailureListener { e ->
                // Fallback: equality (brandKey) saja + sort di memori (salesCount desc, createdAt desc)
                val base = if (brand != null)
                    db.collection("products").whereEqualTo("brandKey", brand)
                else
                    db.collection("products")

                base.limit(50).get()
                    .addOnSuccessListener { qs ->
                        val items = qs.documents
                            .map { d ->
                                val sc = d.getLong("salesCount")
                                    ?: d.getDouble("salesCount")?.toLong()
                                    ?: 0L
                                val created = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                                Triple(sc, created, d)
                            }
                            .sortedWith(compareByDescending<Triple<Long, Long, com.google.firebase.firestore.DocumentSnapshot>> { it.first }
                                .thenByDescending { it.second })
                            .map { (_, _, d) ->
                                ProductCardItem(
                                    id = d.id,
                                    name = d.getString("name").orEmpty(),
                                    minPrice = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                                    thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
                                    isFavorite = favoriteIds.contains(d.id)
                                )
                            }
                            .take(10)
                        popularAdapter.submitList(items)
                    }
                    .addOnFailureListener {
                        Snackbar.make(binding.root, e.localizedMessage ?: "Gagal memuat Best Sellers", Snackbar.LENGTH_LONG).show()
                    }
            }
    }

    private fun loadNewArrivals() {
        val brand = selectedBrandKey
        var q: Query = db.collection("products")
        if (brand != null) q = q.whereEqualTo("brandKey", brand)
        q = q.orderBy("createdAt", Query.Direction.DESCENDING).limit(10)

        q.get()
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
            .addOnFailureListener { e ->
                val base = if (brand != null)
                    db.collection("products").whereEqualTo("brandKey", brand)
                else db.collection("products")
                base.limit(50).get()
                    .addOnSuccessListener { qs ->
                        val items = qs.documents
                            .map { d ->
                                val createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                                ProductCardItem(
                                    id = d.id,
                                    name = d.getString("name").orEmpty(),
                                    minPrice = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                                    thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
                                    isFavorite = favoriteIds.contains(d.id)
                                ) to createdAt
                            }
                            .sortedByDescending { it.second }
                            .map { it.first }
                            .take(10)
                        newAdapter.submitList(items)
                        Snackbar.make(binding.root, "Index New Arrivals belum ada. Menampilkan hasil sementara.", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Snackbar.make(binding.root, e.localizedMessage ?: "Gagal memuat New Arrivals", Snackbar.LENGTH_LONG).show()
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favReg?.remove(); favReg = null
        _binding = null
    }

    private fun Int.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }
}