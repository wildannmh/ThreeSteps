package com.example.projekpemmob.ui.cart

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentCartBinding
import com.example.projekpemmob.util.PriceFormatter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CartFragment : Fragment(R.layout.fragment_cart) {
    private var _b: FragmentCartBinding? = null
    private val b get() = _b!!
    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var adapter: CartAdapter
    private var cartReg: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentCartBinding.bind(view)
        b.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        adapter = CartAdapter(
            onMinus = { row -> updateQty(row, row.qty - 1) },
            onPlus = { row -> updateQty(row, row.qty + 1) },
            onRemove = { row -> removeItem(row) }
        )
        b.rvCart.layoutManager = LinearLayoutManager(requireContext())
        b.rvCart.adapter = adapter

        listenCart()
        b.btnCheckout.setOnClickListener { Snackbar.make(b.root, "Checkout (demo)", Snackbar.LENGTH_SHORT).show() }
    }

    private fun listenCart() {
        val user = auth.currentUser ?: return
        cartReg?.remove()
        cartReg = db.collection("users").document(user.uid).collection("cart")
            .addSnapshotListener { qs, e ->
                if (e != null) {
                    Snackbar.make(b.root, e.localizedMessage ?: "Gagal memuat cart", Snackbar.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val rows = qs?.documents?.map { d ->
                    CartRow(
                        id = d.id,
                        productId = d.getString("productId").orEmpty(),
                        variantId = d.getString("variantId").orEmpty(),
                        name = d.getString("name").orEmpty(),
                        thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
                        size = d.getString("size").orEmpty(),
                        region = d.getString("region").orEmpty(),
                        price = d.getDouble("price") ?: 0.0,
                        qty = (d.getLong("qty") ?: 1L).toInt()
                    )
                }.orEmpty()
                adapter.submitList(rows)
                b.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

                val total = rows.sumOf { it.price * it.qty }
                b.tvTotal.text = "Total: ${PriceFormatter.rupiah(total)}"
            }
    }

    private fun updateQty(row: CartRow, newQty: Int) {
        val user = auth.currentUser ?: return
        if (newQty <= 0) {
            removeItem(row)
            return
        }
        db.collection("users").document(user.uid).collection("cart").document(row.id)
            .set(mapOf("qty" to newQty, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), SetOptions.merge())
    }

    private fun removeItem(row: CartRow) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).collection("cart").document(row.id)
            .delete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cartReg?.remove()
        _b = null
    }
}