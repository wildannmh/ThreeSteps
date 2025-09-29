package com.example.projekpemmob.data.remote

import com.example.projekpemmob.data.model.ProductDoc
import com.example.projekpemmob.data.model.VariantDoc
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object FirestoreProducts {
    private val db get() = Firebase.firestore

    fun queryPopular(limit: Long = 10): Query =
        db.collection("products")
            .whereEqualTo("active", true)
            .orderBy("popularityScore", Query.Direction.DESCENDING)
            .limit(limit)

    fun queryNewArrivals(limit: Long = 10): Query =
        db.collection("products")
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

    suspend fun getProduct(productId: String): ProductDoc? {
        val d = db.collection("products").document(productId).get().await()
        if (!d.exists()) return null
        return ProductDoc(
            id = d.id,
            name = d.getString("name").orEmpty(),
            description = d.getString("description").orEmpty(),
            thumbnailUrl = d.getString("thumbnailUrl").orEmpty(),
            basePrice = d.getDouble("basePrice") ?: 0.0,
            minPrice = d.getDouble("minPrice") ?: (d.getDouble("basePrice") ?: 0.0),
            isBestSeller = d.getBoolean("isBestSeller") ?: false,
            active = d.getBoolean("active") ?: true
        )
    }

    suspend fun getVariantsByRegion(productId: String, region: String): List<VariantDoc> {
        val qs = db.collection("products").document(productId)
            .collection("variants")
            .whereEqualTo("active", true)
            .whereEqualTo("region", region)
            .get()
            .await()

        return qs.documents.map { d ->
            VariantDoc(
                id = d.id,
                region = d.getString("region").orEmpty(),
                size = d.getString("size").orEmpty(),
                price = d.getDouble("price") ?: 0.0,
                stock = (d.getLong("stock") ?: 0L).toInt(),
                active = d.getBoolean("active") ?: true
            )
        }
    }
}