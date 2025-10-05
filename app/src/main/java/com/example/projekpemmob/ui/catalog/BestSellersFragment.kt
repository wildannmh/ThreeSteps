package com.example.projekpemmob.ui.catalog

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.NavGraphDirections
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.core.requireLogin
import com.example.projekpemmob.databinding.FragmentBestSellersBinding
import com.example.projekpemmob.ui.search.adapter.SearchResultAdapter
import com.example.projekpemmob.ui.search.adapter.SearchRowItem
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BestSellersFragment : Fragment(R.layout.fragment_best_sellers) {
    private var _b: FragmentBestSellersBinding? = null
    private val b get() = _b!!

    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val session by lazy { SessionManager(requireContext().applicationContext) }

    private lateinit var adapter: SearchResultAdapter

    private var selectedBrandKey: String? = null
    private var searchQuery: String = ""
    private var isLoading = false
    private var isLastPage = false
    private var lastDoc: DocumentSnapshot? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentBestSellersBinding.bind(view)

        selectedBrandKey = arguments?.getString("brandKey")

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        adapter = SearchResultAdapter(
            onClick = { item ->
                val action = NavGraphDirections.actionGlobalDetails(productId = item.id)
                findNavController().navigate(action)
            },
            onAdd = { item ->
                Snackbar.make(view, "Added to cart: ${item.name}", Snackbar.LENGTH_SHORT).show()
            }
        )

        // FULL seperti search: pakai LinearLayoutManager (1 kolom)
        b.rvGrid.layoutManager = LinearLayoutManager(requireContext())
        b.rvGrid.adapter = adapter
        b.rvGrid.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val visible = lm.childCount
                val total = lm.itemCount
                val first = lm.findFirstVisibleItemPosition()
                if (!isLoading && !isLastPage && (visible + first) >= (total - 4) && searchQuery.isBlank()) {
                    loadPage()
                }
            }
        })

        // Search di Best Sellers (case-insensitive)
        b.tilSearch.setStartIconOnClickListener { runSearch(b.etSearch.text?.toString().orEmpty()) }
        b.tilSearch.setEndIconOnClickListener { b.etSearch.setText(""); runSearch("") }
        b.etSearch.doAfterTextChanged { s -> runSearch(s?.toString().orEmpty()) }

        setupBrandChips()
        resetAndLoad()
    }

    private fun setupBrandChips() {
        val names = resources.getStringArray(R.array.brand_list)
        val keys = resources.getStringArray(R.array.brand_keys)
        val group = b.chipGroupBrand
        group.removeAllViews()

        for (i in names.indices) {
            val label = names[i]
            val key = keys.getOrNull(i) ?: slugify(label)
            val checked = selectedBrandKey == key
            val chip = buildBrandChip(label, key, checked)
            chip.setOnClickListener {
                selectedBrandKey = if (selectedBrandKey == key) null else key
                // Rebuild all chips to update style and checked state
                setupBrandChips()
                if (searchQuery.isBlank()) resetAndLoad() else runSearch(searchQuery)
            }
            group.addView(chip)
        }
    }

    private fun buildBrandChip(label: String, key: String, checked: Boolean): Chip {
        return Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter_Elevated).apply {
            id = View.generateViewId()
            text = if (checked) label else null
            isCheckable = false
            isClickable = true
            isFocusable = true
            tag = key
            // Icon
            val resName = "ic_brand_${key}"
            val iconId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
            chipIcon = if (iconId != 0) requireContext().getDrawable(iconId) else null
            isChipIconVisible = true
            chipIconSize = 32.dpToPx()
            // Style
            if (checked) {
                setChipBackgroundColorResource(android.R.color.white)
                setChipStrokeColorResource(R.color.primary)
                chipStrokeWidth = 2.dpToPx()
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                chipStartPadding = 24.dpToPx()
                chipEndPadding = 24.dpToPx()
                textStartPadding = 8.dpToPx()
                textEndPadding = 8.dpToPx()
                iconStartPadding = 4.dpToPx()
                iconEndPadding = 4.dpToPx()
            } else {
                setChipBackgroundColorResource(android.R.color.white)
                setChipStrokeColorResource(R.color.chip_stroke_selector)
                chipStrokeWidth = 1.dpToPx()
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                chipStartPadding = 24.dpToPx()
                chipEndPadding = 24.dpToPx()
                textStartPadding = 0f
                textEndPadding = 0f
                iconStartPadding = 0f
                iconEndPadding = 0f
            }
        }
    }

    private fun slugify(s: String) = s.trim().lowercase().replace(Regex("\\s+"), "_")

    private fun resetAndLoad() {
        isLoading = false
        isLastPage = false
        lastDoc = null
        adapter.submitList(emptyList())
        b.tvEmpty.visibility = View.GONE
        b.progress.visibility = View.GONE
        loadPage()
    }

    // Tanpa query: Best Sellers = salesCount desc + createdAt desc (pagination)
    private fun loadPage() {
        if (isLoading) return
        isLoading = true
        b.progress.visibility = View.VISIBLE
        b.tvEmpty.visibility = View.GONE

        var q: Query = db.collection("products")
        selectedBrandKey?.let { q = q.whereEqualTo("brandKey", it) }
        q = q.orderBy("salesCount", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
        lastDoc?.let { q = q.startAfter(it) }

        q.get()
            .addOnSuccessListener { qs ->
                val items = qs.documents.map { d ->
                    SearchRowItem(
                        id = d.id,
                        name = d.getString("name").orEmpty(),
                        price = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty()
                    )
                }
                val current = adapter.currentList.toMutableList().apply { addAll(items) }
                adapter.submitList(current)
                b.tvEmpty.visibility = if (current.isEmpty()) View.VISIBLE else View.GONE
                lastDoc = qs.documents.lastOrNull()
                isLastPage = qs.size() < 20
            }
            .addOnFailureListener { e ->
                // Fallback: equality + sort in-memory
                val base = if (selectedBrandKey != null)
                    db.collection("products").whereEqualTo("brandKey", selectedBrandKey)
                else db.collection("products")
                base.limit(100).get()
                    .addOnSuccessListener { qs ->
                        val sorted = qs.documents
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
                                SearchRowItem(
                                    id = d.id,
                                    name = d.getString("name").orEmpty(),
                                    price = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                                    thumbnailUrl = d.getString("thumbnailUrl").orEmpty()
                                )
                            }
                        adapter.submitList(sorted)
                        b.tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
                        isLastPage = true
                    }
                    .addOnFailureListener {
                        Snackbar.make(b.root, e.localizedMessage ?: "Gagal memuat Best Sellers", Snackbar.LENGTH_LONG).show()
                    }
            }
            .addOnCompleteListener {
                b.progress.visibility = View.GONE
                isLoading = false
            }
    }

    // Dengan query: prefix search by nameLower (+brand bila dipilih), non-paginated
    private fun runSearch(qText: String) {
        searchQuery = qText.trim()
        val qLower = searchQuery.lowercase()
        if (qLower.isBlank()) {
            resetAndLoad()
            return
        }

        b.progress.visibility = View.VISIBLE
        b.tvEmpty.visibility = View.GONE
        adapter.submitList(emptyList())
        isLastPage = true

        val base = db.collection("products")
        val brand = selectedBrandKey
        val q = if (brand != null) {
            base.whereEqualTo("brandKey", brand)
                .orderBy("nameLower")
                .startAt(qLower).endAt(qLower + "\uf8ff")
                .limit(50)
        } else {
            base.orderBy("nameLower")
                .startAt(qLower).endAt(qLower + "\uf8ff")
                .limit(50)
        }

        q.get()
            .addOnSuccessListener { qs ->
                val items = qs.documents.map { d ->
                    SearchRowItem(
                        id = d.id,
                        name = d.getString("name").orEmpty(),
                        price = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty()
                    )
                }
                adapter.submitList(items)
                b.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                val eq = if (brand != null) base.whereEqualTo("brandKey", brand) else base
                eq.limit(100).get()
                    .addOnSuccessListener { qs ->
                        val filtered = qs.documents
                            .filter { (it.getString("name") ?: "").lowercase().startsWith(qLower) }
                            .map { d ->
                                SearchRowItem(
                                    id = d.id,
                                    name = d.getString("name").orEmpty(),
                                    price = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                                    thumbnailUrl = d.getString("thumbnailUrl").orEmpty()
                                )
                            }
                        adapter.submitList(filtered)
                        b.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                        Snackbar.make(b.root, "Index brandKey+nameLower belum ada. Menampilkan hasil sementara.", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Snackbar.make(b.root, e.localizedMessage ?: "Gagal mencari produk", Snackbar.LENGTH_LONG).show()
                    }
            }
            .addOnCompleteListener { b.progress.visibility = View.GONE }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
    private fun Int.dpToPx(): Float = this * resources.displayMetrics.density
}