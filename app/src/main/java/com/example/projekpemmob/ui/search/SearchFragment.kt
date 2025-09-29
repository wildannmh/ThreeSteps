package com.example.projekpemmob.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.databinding.FragmentSearchBinding
import com.example.projekpemmob.ui.search.adapter.SearchResultAdapter
import com.example.projekpemmob.ui.search.adapter.SearchRowItem
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<SearchFragmentArgs>()
    private lateinit var adapter: SearchResultAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SearchResultAdapter(
            onClick = { item ->
                // Kirim productId (String)
                val action = SearchFragmentDirections.actionSearchToDetails(item.id)
                findNavController().navigate(action)
            },
            onAdd = { item ->
                Snackbar.make(view, "Added to cart: ${item.name}", Snackbar.LENGTH_SHORT).show()
                // TODO: tambahkan logika simpan ke keranjang user bila diperlukan
            }
        )
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Tampilkan semua produk saat masuk
        binding.etSearch.setText(args.initialQuery)
        runSearch(args.initialQuery)

        // Ikon search
        binding.tilSearch.setStartIconOnClickListener {
            runSearch(binding.etSearch.text?.toString().orEmpty())
        }
        // Tombol Search di keyboard
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(binding.etSearch.text?.toString().orEmpty()); true
            } else false
        }
        // Clear icon
        binding.tilSearch.setEndIconOnClickListener {
            binding.etSearch.setText("")
            runSearch("")
        }
        // Live filter (opsional)
        binding.etSearch.doAfterTextChanged { s ->
            runSearch(s?.toString().orEmpty())
        }

        // Fokus input
        binding.etSearch.requestFocus()
        showKeyboard()
    }

    private fun runSearch(query: String) {
        val db = Firebase.firestore
        val col = db.collection("products")

        val task = if (query.isBlank()) {
            // Semua produk terbaru
            col.orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get()
        } else {
            // Prefix search by name (case-sensitive di Firestore; simpan name dengan Title Case konsisten)
            val q = query.trim()
            col.orderBy("name").startAt(q).endAt(q + "\uf8ff").limit(50).get()
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
                binding.tvEmpty.text = if (query.isBlank()) "Belum ada produk" else "Tidak ada hasil untuk \"$query\""
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Gagal memuat hasil: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun showKeyboard() {
        binding.etSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}