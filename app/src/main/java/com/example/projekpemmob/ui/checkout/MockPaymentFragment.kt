package com.example.projekpemmob.ui.checkout

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
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
        val subtotal = doc.getDouble("subtotal") ?: 0.0
        val shippingCost = doc.getDouble("shippingCost") ?: 0.0
        val status = doc.getString("status") ?: "pending"
        val expiresAt = doc.getTimestamp("expiresAt")
        val shipping = doc.get("shipping") as? Map<*, *>
        val items = doc.get("items") as? List<Map<String, Any?>>

        // Tampilkan info pengiriman
        if (shipping != null) {
            val receiver = shipping["receiver"] as? String ?: "-"
            val phone = shipping["phone"] as? String ?: "-"
            val addressLine = shipping["addressLine"] as? String ?: "-"
            val city = shipping["city"] as? String ?: "-"
            val postal = shipping["postalCode"] as? String ?: "-"
            val courier = shipping["courier"] as? String ?: "-"
            b.tvReceiver.text = "Penerima: $receiver ($phone)"
            b.tvAddress.text = "Alamat: $addressLine, $city, $postal"
            b.tvCourier.text = "Kurir: $courier"
        } else {
            b.tvReceiver.text = "Penerima: -"
            b.tvAddress.text = "Alamat: -"
            b.tvCourier.text = "Kurir: -"
        }

        // Tampilkan subtotal, ongkir, total
        b.tvSubtotal.text = "Subtotal: ${PriceFormatter.rupiah(subtotal)}"
        b.tvShipping.text = "Ongkir: ${PriceFormatter.rupiah(shippingCost)}"
        b.tvTotal.text = "Total: ${PriceFormatter.rupiah(total)}"

        // Tampilkan daftar item
        b.layoutItems.removeAllViews()
        items?.forEach { item ->
            val name = item["name"] as? String ?: "-"
            val size = item["size"] as? String ?: "-"
            val region = item["region"] as? String ?: ""
            val qty = (item["qty"] as? Number)?.toInt() ?: 1
            val price = (item["price"] as? Number)?.toDouble() ?: 0.0
            val itemView = TextView(requireContext())
            itemView.text = "$name $region $size x$qty - ${PriceFormatter.rupiah(price * qty)}"
            b.layoutItems.addView(itemView)
        }

        // Status dan tombol
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

                // 1. READ semua varian dahulu
                data class ItemData(
                    val varRef: DocumentReference,
                    val productRef: DocumentReference,
                    val qty: Int,
                    val pricePerUnit: Double,
                    val stock: Int
                )
                val allItemData = items.map { it ->
                    val productId = (it["productId"] as? String).orEmpty()
                    val variantId = (it["variantId"] as? String).orEmpty()
                    val qty = ((it["qty"] as? Number)?.toInt()) ?: 0
                    val pricePerUnit = ((it["price"] as? Number)?.toDouble()) ?: 0.0

                    val varRef = db.collection("products").document(productId)
                        .collection("variants").document(variantId)
                    val productRef = db.collection("products").document(productId)
                    val varSnap = tx.get(varRef)
                    val stock = (varSnap.getLong("stock") ?: 0L).toInt()
                    if (stock < qty) throw IllegalStateException("Stok kurang untuk salah satu item")

                    ItemData(varRef, productRef, qty, pricePerUnit, stock)
                }

                // 2. WRITE setelah semua read
                for (item in allItemData) {
                    tx.update(item.varRef, "stock", item.stock - item.qty)
                    tx.update(
                        item.productRef,
                        mapOf(
                            "salesCount" to FieldValue.increment(item.qty.toLong()),
                            "salesAmount" to FieldValue.increment((item.pricePerUnit * item.qty).toLong()),
                            "lastSoldAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 3. Tandai order paid (mock)
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

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _b = null
    }
}