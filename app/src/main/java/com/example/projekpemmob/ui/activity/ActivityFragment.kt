package com.example.projekpemmob.ui.activity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentActivityBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityFragment : Fragment(R.layout.fragment_activity) {
    private var _b: FragmentActivityBinding? = null
    private val b get() = _b!!
    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var adapter: OrdersAdapter
    private var reg: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentActivityBinding.bind(view)

        adapter = OrdersAdapter(
            onClick = { row ->
                if (row.status.equals("awaiting_payment", ignoreCase = true)) {
                    val action = ActivityFragmentDirections.actionActivityFragmentToMockPaymentFragment(row.id)
                    findNavController().navigate(action)
                } else {
                    OrderDetailBottomSheet.newInstance(row.id).show(childFragmentManager, "order_detail")
                }
            }
        )
        b.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        b.rvOrders.adapter = adapter

        listenOrders()
    }

    private fun listenOrders() {
        val user = auth.currentUser
        if (user == null) {
            b.tvEmpty.visibility = View.VISIBLE
            adapter.submitList(emptyList())
            return
        }
        reg?.remove()
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role") ?: "buyer"
                val col = db.collection("orders")
                val query = if (role == "seller" || role == "admin") {
                    // Seller/admin: lihat semua order
                    col.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
                } else {
                    // Buyer: hanya lihat order pribadi
                    col.whereEqualTo("userId", user.uid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(100)
                }
                reg = query.addSnapshotListener { qs, e ->
                    if (e != null) {
                        Snackbar.make(b.root, e.localizedMessage ?: "Gagal memuat aktivitas", Snackbar.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }
                    val rows = qs?.documents?.map { d ->
                        @Suppress("UNCHECKED_CAST")
                        val items = (d.get("items") as? List<Map<String, Any?>>).orEmpty()
                        val names = items.mapNotNull { it["name"] as? String }.filter { it.isNotBlank() }
                        val itemsLabel = when {
                            names.isEmpty() -> "-"
                            names.size == 1 -> names[0]
                            names.size == 2 -> "${names[0]}, ${names[1]}"
                            else -> "${names[0]}, ${names[1]} +${names.size - 2} lainnya"
                        }
                        OrderRow(
                            id = d.id,
                            total = d.getDouble("total") ?: 0.0,
                            status = d.getString("status") ?: "pending",
                            itemsCount = items.size,
                            createdAt = d.getTimestamp("createdAt")?.toDate(),
                            itemsLabel = itemsLabel
                        )
                    }.orEmpty()
                    b.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    adapter.submitList(rows)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reg?.remove(); reg = null
        _b = null
    }
}