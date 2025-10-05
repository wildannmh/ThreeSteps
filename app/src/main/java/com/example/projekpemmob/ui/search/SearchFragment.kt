package com.example.projekpemmob.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.NavGraphDirections
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentSearchBinding
import com.example.projekpemmob.ui.search.adapter.SearchResultAdapter
import com.example.projekpemmob.ui.search.adapter.SearchRowItem
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<SearchFragmentArgs>()
    private lateinit var adapter: SearchResultAdapter

    private var selectedBrandKey: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SearchResultAdapter(
            onClick = { item ->
                val action = NavGraphDirections.actionGlobalDetails(productId = item.id)
                findNavController().navigate(action)
            },
            onAdd = { item ->
                Snackbar.make(view, "Added to cart: ${item.name}", Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Brand chips
        setupBrandChips()

        // Nilai awal
        binding.etSearch.setText(args.initialQuery)
        runSearch(args.initialQuery)

        // Aksi search
        binding.tilSearch.setStartIconOnClickListener { runSearch(binding.etSearch.text?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(binding.etSearch.text?.toString().orEmpty()); true
            } else false
        }
        binding.tilSearch.setEndIconOnClickListener { binding.etSearch.setText(""); runSearch("") }
        binding.etSearch.doAfterTextChanged { s -> runSearch(s?.toString().orEmpty()) }

        // Fokus input
        binding.etSearch.requestFocus()
        showKeyboard()
    }

    private fun setupBrandChips() {
        val names = resources.getStringArray(R.array.brand_list)
        val keys = resources.getStringArray(R.array.brand_keys)
        val group = binding.chipGroupBrand
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
                runSearch(binding.etSearch.text?.toString().orEmpty())
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

    // CASE-INSENSITIVE SEARCH: gunakan field 'nameLower' + query.lowercase()
    private fun runSearch(query: String) {
        val db = Firebase.firestore
        val col = db.collection("products")
        val qText = query.trim()
        val qLower = qText.lowercase()
        val brand = selectedBrandKey

        val task = when {
            qLower.isBlank() && brand == null -> {
                col.orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get()
            }
            qLower.isBlank() && brand != null -> {
                col.whereEqualTo("brandKey", brand)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50).get()
            }
            qLower.isNotBlank() && brand == null -> {
                col.orderBy("nameLower")
                    .startAt(qLower).endAt(qLower + "\uf8ff")
                    .limit(50).get()
            }
            else -> {
                col.whereEqualTo("brandKey", brand)
                    .orderBy("nameLower")
                    .startAt(qLower).endAt(qLower + "\uf8ff")
                    .limit(50).get()
            }
        }

        task
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
                binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.tvEmpty.text = if (qLower.isBlank()) "Belum ada produk" else "Tidak ada hasil untuk \"$qText\""
            }
            .addOnFailureListener { e ->
                // Fallback: equality brand + filter di memori (ignoreCase)
                val base = if (brand != null) col.whereEqualTo("brandKey", brand) else col
                base.limit(100).get()
                    .addOnSuccessListener { qs ->
                        val all = qs.documents.mapNotNull { d ->
                            val name = d.getString("name").orEmpty()
                            if (qLower.isBlank() || name.lowercase().startsWith(qLower)) {
                                SearchRowItem(
                                    id = d.id,
                                    name = name,
                                    price = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
                                    thumbnailUrl = d.getString("thumbnailUrl").orEmpty()
                                )
                            } else null
                        }
                        adapter.submitList(all)
                        binding.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
                        Snackbar.make(binding.root, "Index pencarian belum ada. Menampilkan hasil sementara.", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Snackbar.make(binding.root, "Gagal memuat hasil: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
                    }
            }
    }

    private fun showKeyboard() {
        binding.etSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
    private fun Int.dpToPx(): Float = this * resources.displayMetrics.density
}