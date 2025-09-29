package com.example.projekpemmob.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object StockRepository {
    private val db get() = Firebase.firestore

    /**
     * Membuat order sederhana dan mengurangi stok varian secara atomik.
     * Akan throw IllegalStateException jika stok tidak cukup atau belum login.
     */
    suspend fun placeOrderAndDecrement(
        productId: String,
        variantId: String,
        qty: Int,
        priceEach: Double
    ): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Not logged in")

        val orderRef = db.collection("orders").document()

        db.runTransaction { tr ->
            val variantRef = db.collection("products").document(productId)
                .collection("variants").document(variantId)

            val snap = tr.get(variantRef)
            val currentStock = (snap.getLong("stock") ?: 0L).toInt()
            if (currentStock < qty) {
                throw IllegalStateException("Stock tidak cukup (tersisa $currentStock)")
            }

            // Kurangi stok
            tr.update(
                variantRef,
                mapOf(
                    "stock" to (currentStock - qty),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Buat order minimal
            val subtotal = priceEach * qty
            val orderData = hashMapOf(
                "userId" to uid,
                "status" to "pending", // pending|paid|shipped|completed|canceled
                "items" to listOf(
                    mapOf(
                        "productId" to productId,
                        "variantId" to variantId,
                        "qty" to qty,
                        "price" to priceEach
                    )
                ),
                "subtotal" to subtotal,
                "grandTotal" to subtotal,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            tr.set(orderRef, orderData)
            null
        }.await()

        return orderRef.id
    }
}