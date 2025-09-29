package com.example.projekpemmob.data.model

data class ProductDoc(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val basePrice: Double = 0.0,
    val minPrice: Double = 0.0,
    val isBestSeller: Boolean = false,
    val active: Boolean = true
)

data class VariantDoc(
    val id: String = "",
    val region: String = "EU",
    val size: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val active: Boolean = true
)