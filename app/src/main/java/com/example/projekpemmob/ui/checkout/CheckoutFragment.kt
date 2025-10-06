package com.example.projekpemmob.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentCheckoutBinding
import com.example.projekpemmob.util.PriceFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {
    private var _b: FragmentCheckoutBinding? = null
    private val b get() = _b!!
    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var subtotal = 0.0
    // Tetapkan ongkos kirim JNE REG secara statis
    private var shippingCost = 15000.0 // Ongkir JNE REG, diambil dari mock default
    private var total = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentCheckoutBinding.bind(view)

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.tvCourierPrice.text = PriceFormatter.rupiah(shippingCost)


        lifecycleScope.launch {
            loadCartAndSummary()
        }

        b.btnCreateOrder.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Konfirmasi Pesanan")
                .setIcon(R.drawable.ic_question_24)
                .setMessage("Pastikan data pesanan dan alamat pengiriman sudah benar.\n\nPesanan yang sudah dibuat tidak dapat diubah.\n\nLanjutkan membuat pesanan?")
                .setPositiveButton("Ya, Buat Pesanan") { dialog, _ ->
                    dialog.dismiss()
                    lifecycleScope.launch { createOrderAndGo() }
                }
                .setNegativeButton("Cek Lagi", null)
                .show()
        }
    }

    private suspend fun loadCartAndSummary() {
        val user = auth.currentUser ?: run {
            Snackbar.make(b.root, "Silakan login terlebih dahulu", Snackbar.LENGTH_LONG).show(); return
        }
        val qs = db.collection("users").document(user.uid).collection("cart").get().await()
        val rows = qs.documents.map { d ->
            val price = d.getDouble("price") ?: 0.0
            val qty = (d.getLong("qty") ?: 1L).toInt()
            price * qty
        }
        subtotal = rows.sum()

        // shippingCost sudah didefinisikan di atas (15000.0) dan tidak perlu diubah lagi
        // karena tidak ada pilihan kurir.

        renderSummary()
    }

    private fun renderSummary() {
        b.tvSubtotal.text = "Subtotal: ${PriceFormatter.rupiah(subtotal)}"
        b.tvShipping.text = "Ongkir: ${PriceFormatter.rupiah(shippingCost)}"
        total = subtotal + shippingCost
        b.tvTotal.text = "Total: ${PriceFormatter.rupiah(total)}"
    }

    private suspend fun createOrderAndGo() {
        val user = auth.currentUser ?: return
        // validasi form singkat
        val receiver = b.etReceiver.text?.toString()?.trim().orEmpty()
        val phone = b.etPhone.text?.toString()?.trim().orEmpty()
        val address = b.etAddress.text?.toString()?.trim().orEmpty()
        val city = b.etCity.text?.toString()?.trim().orEmpty()
        val postal = b.etPostal.text?.toString()?.trim().orEmpty()
        // Kurir ditetapkan statis, tidak lagi diambil dari actCourier
        val courier = "JNE REG"

        if (receiver.isBlank() || phone.isBlank() || address.isBlank() || city.isBlank() || postal.isBlank()) {
            Snackbar.make(b.root, "Lengkapi alamat pengiriman", Snackbar.LENGTH_LONG).show()
            return
        }

        // kumpulkan items dari cart
        val cartSnap = db.collection("users").document(user.uid).collection("cart").get().await()
        if (cartSnap.isEmpty) {
            Snackbar.make(b.root, "Keranjang kosong", Snackbar.LENGTH_SHORT).show(); return
        }
        val items = cartSnap.documents.map { d ->
            mapOf(
                "productId" to d.getString("productId").orEmpty(),
                "variantId" to d.getString("variantId").orEmpty(),
                "name" to d.getString("name").orEmpty(),
                "thumbnailUrl" to d.getString("thumbnailUrl").orEmpty(),
                "region" to d.getString("region").orEmpty(),
                "size" to d.getString("size").orEmpty(),
                "price" to (d.getDouble("price") ?: 0.0),
                "qty" to ((d.getLong("qty") ?: 1L).toInt())
            )
        }

        // expiresAt: 15 menit dari sekarang (client time; OK untuk mock)
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 15 * 60 * 1000))

        val orderRef = db.collection("orders").document()
        val orderData = hashMapOf(
            "userId" to user.uid,
            "items" to items,
            "subtotal" to subtotal,
            "shippingCost" to shippingCost,
            "total" to total,
            "status" to "awaiting_payment",
            "shipping" to mapOf(
                "receiver" to receiver,
                "phone" to phone,
                "addressLine" to address,
                "city" to city,
                "postalCode" to postal,
                "courier" to courier // Menggunakan nilai statis "JNE REG"
            ),
            "expiresAt" to expiresAt,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            orderRef.set(orderData).await()
            // menuju MockPayment
            val action = CheckoutFragmentDirections.actionCheckoutToMockPayment(orderRef.id)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Snackbar.make(b.root, e.message ?: "Gagal membuat order", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}