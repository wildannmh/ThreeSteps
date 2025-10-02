package com.example.projekpemmob.ui.checkout

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.FragmentMockPaymentBinding
import com.example.projekpemmob.util.PriceFormatter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MockPaymentFragment : Fragment(R.layout.fragment_mock_payment) {
    private var _b: FragmentMockPaymentBinding? = null
    private val b get() = _b!!
    private val args by navArgs<MockPaymentFragmentArgs>()
    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var timer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentMockPaymentBinding.bind(view)
        val orderId = args.orderId

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.tvOrderId.text = "Order: $orderId"

        MainScope().launch {
            loadSummaryAndStartCountdown(orderId)
        }

        b.btnPayMock.setOnClickListener {
            MainScope().launch { payMock(orderId) }
        }
    }

    private suspend fun loadSummaryAndStartCountdown(orderId: String) {
        val doc = db.collection("orders").document(orderId).get().await()
        if (!doc.exists()) {
            Snackbar.make(b.root, "Order tidak ditemukan", Snackbar.LENGTH_LONG).show()
            return
        }
        val total = doc.getDouble("total") ?: 0.0
        val status = doc.getString("status") ?: "pending"
        val expiresAt = doc.getTimestamp("expiresAt") // bisa null jika dibuat sebelum fitur ini
        b.tvTotal.text = "Total: ${PriceFormatter.rupiah(total)}"

        if (status == "paid") {
            b.btnPayMock.isEnabled = false
            b.tvHint.text = "Order sudah dibayar."
            b.tvCountdown.visibility = View.GONE
            return
        }

        if (expiresAt == null) {
            // Tidak ada expire, biarkan tombol aktif
            b.tvCountdown.text = "Tanpa batas waktu"
            b.btnPayMock.isEnabled = true
            return
        }

        val millisLeft = expiresAt.toDate().time - System.currentTimeMillis()
        if (millisLeft <= 0L) {
            // Sudah kadaluarsa — set expired (MOCK via rules tester)
            expireOrderIfAwaiting(orderId)
            b.btnPayMock.isEnabled = false
            b.tvCountdown.text = "Waktu habis"
            b.tvHint.text = "Order kadaluarsa. Buat order baru dari Checkout."
            return
        }

        startCountdown(orderId, millisLeft)
    }

    private fun startCountdown(orderId: String, millis: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(millis, 1000) {
            override fun onTick(ms: Long) {
                val m = (ms / 1000) / 60
                val s = (ms / 1000) % 60
                b.tvCountdown.text = String.format("Sisa waktu: %02d:%02d", m, s)
            }
            override fun onFinish() {
                MainScope().launch {
                    expireOrderIfAwaiting(orderId)
                    b.btnPayMock.isEnabled = false
                    b.tvCountdown.text = "Waktu habis"
                    b.tvHint.text = "Order kadaluarsa. Buat order baru dari Checkout."
                }
            }
        }.start()
    }

    private suspend fun expireOrderIfAwaiting(orderId: String) {
        val user = auth.currentUser ?: return
        try {
            db.runTransaction { tx ->
                val ref = db.collection("orders").document(orderId)
                val snap = tx.get(ref)
                if (!snap.exists()) return@runTransaction
                val status = snap.getString("status") ?: "pending"
                if (status == "awaiting_payment") {
                    tx.update(ref, mapOf(
                        "status" to "expired",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
        } catch (_: Exception) { /* noop in dev */ }
    }

    private suspend fun payMock(orderId: String) {
        val user = auth.currentUser ?: run {
            Snackbar.make(b.root, "Silakan login dulu", Snackbar.LENGTH_LONG).show(); return
        }
        try {
            val orderRef = db.collection("orders").document(orderId)
            val snap = orderRef.get().await()
            val status = snap.getString("status") ?: "pending"
            val expiresAt = snap.getTimestamp("expiresAt")
            if (status != "awaiting_payment") {
                Snackbar.make(b.root, "Order tidak dalam status pembayaran", Snackbar.LENGTH_LONG).show()
                return
            }
            if (expiresAt != null && expiresAt.toDate().time <= System.currentTimeMillis()) {
                expireOrderIfAwaiting(orderId)
                Snackbar.make(b.root, "Order kadaluarsa", Snackbar.LENGTH_LONG).show()
                return
            }

            db.runTransaction { tx ->
                val order = tx.get(orderRef).data ?: throw IllegalStateException("Order kosong")
                @Suppress("UNCHECKED_CAST")
                val items = order["items"] as? List<Map<String, Any?>> ?: emptyList()

                // 1) Kurangi stok per varian
                for (it in items) {
                    val productId = (it["productId"] as? String).orEmpty()
                    val variantId = (it["variantId"] as? String).orEmpty()
                    val qty = ((it["qty"] as? Number)?.toInt()) ?: 0
                    val pricePerUnit = ((it["price"] as? Number)?.toDouble()) ?: 0.0

                    val varRef = db.collection("products").document(productId)
                        .collection("variants").document(variantId)
                    val varSnap = tx.get(varRef)
                    val stock = (varSnap.getLong("stock") ?: 0L).toInt()
                    if (stock < qty) throw IllegalStateException("Stok kurang untuk salah satu item")
                    tx.update(varRef, "stock", stock - qty)

                    // 2) Naikkan metrik produk (Best Sellers)
                    val productRef: DocumentReference = db.collection("products").document(productId)
                    // Pakai increment agar tahan race condition; lastSoldAt untuk tie-breaker
                    tx.update(
                        productRef,
                        mapOf(
                            "salesCount" to FieldValue.increment(qty.toLong()),
                            "salesAmount" to FieldValue.increment((pricePerUnit * qty).toLong()), // opsional: jika mau rupiah bulat
                            "lastSoldAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 3) Tandai order paid (mock)
                tx.update(
                    orderRef,
                    mapOf(
                        "status" to "paid",
                        "payment" to mapOf("mock" to true, "paidAt" to FieldValue.serverTimestamp()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            // Kosongkan cart (best-effort)
            try {
                val docs = db.collection("users").document(user.uid).collection("cart").get().await()
                val batch = db.batch()
                docs.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            } catch (_: Exception) {}

            Snackbar.make(b.root, "Pembayaran mock berhasil", Snackbar.LENGTH_LONG).show()
            findNavController().navigate(R.id.homeFragment)
        } catch (e: Exception) {
            Snackbar.make(b.root, e.message ?: "Gagal memproses pembayaran", Snackbar.LENGTH_LONG).show()
        }
    }
}