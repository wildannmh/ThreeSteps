package com.example.projekpemmob.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.projekpemmob.R
import com.example.projekpemmob.core.SessionManager
import com.example.projekpemmob.core.requireLogin
import com.example.projekpemmob.data.remote.FirestoreProducts
import com.example.projekpemmob.databinding.FragmentDetailsBinding
import com.example.projekpemmob.util.PriceFormatter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DetailsFragment : Fragment(R.layout.fragment_details) {
    private var _b: FragmentDetailsBinding? = null
    private val b get() = _b!!
    private val args by navArgs<DetailsFragmentArgs>()
    private val db get() = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val session by lazy { SessionManager(requireContext().applicationContext) }

    private var productId: String = ""
    private var productName: String = ""
    private var productThumb: String = ""
    private var basePrice: Double = 0.0
    private var minPrice: Double = 0.0

    private var currentRegion: String = "EU"
    private var selected: SizeChip? = null
    private lateinit var sizeAdapter: SizeChipAdapter

    private var cartBadgeReg: ListenerRegistration? = null
    private var favDocReg: ListenerRegistration? = null
    private var isFavorite: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentDetailsBinding.bind(view)
        productId = args.productId

        b.topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
        b.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_cart -> {
                    findNavController().navigate(R.id.cartFragment)
                    true
                }
                else -> false
            }
        }
        setupCartBadge()

        sizeAdapter = SizeChipAdapter { chip ->
            selected = chip
            applySelection(chip.variantId)
            renderBuyPrice()
            renderStock()
        }
        b.rvSizes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvSizes.adapter = sizeAdapter

        b.tvEU.setOnClickListener { switchRegion("EU") }
        b.tvUS.setOnClickListener { switchRegion("US") }
        b.tvUK.setOnClickListener { switchRegion("UK") }

        b.btnFav.setOnClickListener { requireLogin(session) { toggleFavorite() } }

        b.btnCart.setOnClickListener { requireLogin(session) { addToCart() } }

        b.btnBuy.setOnClickListener { requireLogin(session) { buyNow() } }

        loadProduct()
        initFavoriteState()
    }

    private fun setupCartBadge() {
        val user = auth.currentUser ?: return
        cartBadgeReg?.remove()
        cartBadgeReg = db.collection("users").document(user.uid).collection("cart")
            .addSnapshotListener { qs, _ ->
                val hasItems = (qs?.size() ?: 0) > 0
            }
    }

    private fun initFavoriteState() {
        favDocReg?.remove()
        val uid = auth.currentUser?.uid ?: run {
            isFavorite = false
            syncFavoriteIcon()
            return
        }
        favDocReg = db.collection("users").document(uid)
            .collection("favorites").document(productId)
            .addSnapshotListener { doc, _ ->
                isFavorite = doc?.exists() == true
                syncFavoriteIcon()
            }
    }

    private fun syncFavoriteIcon() {
        val iconRes = if (isFavorite) R.drawable.ic_heart_filled_24 else R.drawable.ic_heart_24
        b.btnFav.setIconResource(iconRes)
    }

    private fun toggleFavorite() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid).collection("favorites").document(productId)
        if (isFavorite) {
            ref.delete()
        } else {
            val price = selected?.price ?: minPrice.takeIf { it > 0 } ?: basePrice
            val data = mapOf(
                "productId" to productId,
                "name" to productName,
                "thumbnailUrl" to productThumb,
                "price" to price,
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(data)
        }
    }

    private fun loadProduct() {
        lifecycleScope.launch {
            val p = FirestoreProducts.getProduct(productId)
            if (p == null) {
                Snackbar.make(b.root, "Produk tidak ditemukan", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            productName = p.name
            productThumb = p.thumbnailUrl
            basePrice = p.basePrice
            minPrice = p.minPrice

            b.ivHero.load(productThumb)
            b.tvName.text = productName
            b.tvBest.visibility = if (p.isBestSeller) View.VISIBLE else View.GONE
            b.tvPrice.text = PriceFormatter.rupiah(minPrice)
            b.tvDesc.text = p.description

            val euDef = async { FirestoreProducts.getVariantsByRegion(productId, "EU") }
            val usDef = async { FirestoreProducts.getVariantsByRegion(productId, "US") }
            val ukDef = async { FirestoreProducts.getVariantsByRegion(productId, "UK") }
            val eu = euDef.await()
            val us = usDef.await()
            val uk = ukDef.await()

            b.tvEU.visibility = if (eu.isEmpty()) View.GONE else View.VISIBLE
            b.tvUS.visibility = if (us.isEmpty()) View.GONE else View.VISIBLE
            b.tvUK.visibility = if (uk.isEmpty()) View.GONE else View.VISIBLE

            currentRegion = when {
                eu.isNotEmpty() -> "EU"
                us.isNotEmpty() -> "US"
                uk.isNotEmpty() -> "UK"
                else -> "EU"
            }
            updateRegionTabsUI()

            val initial = when (currentRegion) {
                "EU" -> eu
                "US" -> us
                else -> uk
            }
            val chips = initial.sortedBy { it.size }.map { v -> SizeChip(v.size, v.stock, v.id, v.price) }
            sizeAdapter.submitList(chips)

            if (chips.isNotEmpty()) {
                selected = chips.first()
                applySelection(selected!!.variantId)
                renderBuyPrice()
                renderStock()
            } else {
                b.tvStock.text = "Sisa stok: 0"
                b.btnBuy.isEnabled = false
                b.btnCart.isEnabled = false
            }
        }
    }

    private fun switchRegion(region: String) {
        if (region == currentRegion) return
        currentRegion = region
        updateRegionTabsUI()
        lifecycleScope.launch {
            val variants = FirestoreProducts.getVariantsByRegion(productId, currentRegion)
            val chips = variants.sortedBy { it.size }.map { v -> SizeChip(v.size, v.stock, v.id, v.price) }
            selected = null
            sizeAdapter.submitList(chips)
            b.btnBuy.text = "Buy Now"
            b.tvStock.text = "Sisa stok: -"
            b.btnBuy.isEnabled = false
            b.btnCart.isEnabled = false
        }
    }

    private fun updateRegionTabsUI() {
        b.tvEU.alpha = if (currentRegion == "EU") 1f else 0.6f
        b.tvUS.alpha = if (currentRegion == "US") 1f else 0.6f
        b.tvUK.alpha = if (currentRegion == "UK") 1f else 0.6f
    }

    private fun applySelection(variantId: String) {
        val newList = sizeAdapter.currentList.map { it.copy(selected = it.variantId == variantId) }
        sizeAdapter.submitList(newList)
    }

    private fun renderBuyPrice() {
        val price = selected?.price ?: minPrice.takeIf { it > 0 } ?: basePrice
        b.btnBuy.text = "Buy Now ${PriceFormatter.rupiah(price)}"
        b.tvPrice.text = PriceFormatter.rupiah(price)
    }

    private fun renderStock() {
        val stock = selected?.stock ?: 0
        b.tvStock.text = "Sisa stok: $stock"

        b.btnBuy.isEnabled = stock > 0
        b.btnCart.isEnabled = stock > 0

        b.btnBuy.alpha = if (stock > 0) 1f else 0.5f
    }

    private fun addToCart() {
        val user = auth.currentUser ?: return
        val sel = selected ?: run {
            Snackbar.make(b.root, "Pilih size dulu", Snackbar.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                addOrMergeCart(user.uid, sel, incrementBy = 1)

                val snackbar = Snackbar.make(b.root, "Item ditambahkan di keranjang", Snackbar.LENGTH_SHORT)

                // MENGATUR ANCHOR VIEW KE PARENT DARI FLOATING FOOTER CARD
                // Ini adalah trik terbaik untuk CoordinatorLayout agar Snackbar muncul di atas footer
                snackbar.anchorView = b.btnBuy.rootView.parent as View
                snackbar.show()

            } catch (e: Exception) {
                Snackbar.make(b.root, e.message ?: "Gagal menambahkan ke keranjang", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun buyNow() {
        val user = auth.currentUser ?: return
        val sel = selected ?: run {
            Snackbar.make(b.root, "Pilih size dulu", Snackbar.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                addOrMergeCart(user.uid, sel, incrementBy = 1)
                findNavController().navigate(R.id.action_details_to_checkout)
            } catch (e: Exception) {
                Snackbar.make(b.root, e.message ?: "Gagal memproses Buy Now", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun addOrMergeCart(uid: String, sel: SizeChip, incrementBy: Int) {
        val cartCol = db.collection("users").document(uid).collection("cart")
        val existing = cartCol
            .whereEqualTo("productId", productId)
            .whereEqualTo("variantId", sel.variantId)
            .limit(1)
            .get()
            .await()

        val docId = if (!existing.isEmpty) existing.documents.first().id else "${productId}_${sel.variantId}"
        val cartRef = cartCol.document(docId)
        val variantRef = db.collection("products").document(productId)
            .collection("variants").document(sel.variantId)

        db.runTransaction { tx ->
            val varSnap = tx.get(variantRef)
            if (!varSnap.exists()) throw IllegalStateException("Varian tidak ditemukan")
            val stock = (varSnap.getLong("stock") ?: 0L).toInt()

            val cartSnap = tx.get(cartRef)
            val currentQty = (cartSnap.getLong("qty") ?: 0L).toInt()
            val newQty = currentQty + incrementBy
            if (newQty > stock) {
                throw IllegalStateException("Stok tidak cukup. Maksimal $stock item di keranjang.")
            }

            val updates = mutableMapOf<String, Any>(
                "productId" to productId,
                "variantId" to sel.variantId,
                "name" to productName,
                "thumbnailUrl" to productThumb,
                "region" to currentRegion,
                "size" to sel.size,
                "price" to sel.price,
                "qty" to newQty,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!cartSnap.exists()) {
                updates["createdAt"] = FieldValue.serverTimestamp()
            }
            tx.set(cartRef, updates, SetOptions.merge())
        }.await()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favDocReg?.remove(); favDocReg = null
        cartBadgeReg?.remove(); cartBadgeReg = null
        _b = null
    }
}
