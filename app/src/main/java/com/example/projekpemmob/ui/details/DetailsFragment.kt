package com.example.projekpemmob.ui.details

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
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
import com.example.projekpemmob.data.repository.StockRepository
import com.example.projekpemmob.databinding.FragmentDetailsBinding
import com.example.projekpemmob.util.PriceFormatter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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

        // Header lokal
        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnOpenCart.setOnClickListener { findNavController().navigate(R.id.cartFragment) }
        setupCartBadge()

        // Size chips
        sizeAdapter = SizeChipAdapter { chip ->
            selected = chip
            applySelection(chip.variantId)
            renderBuyPrice()
        }
        b.rvSizes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.rvSizes.adapter = sizeAdapter

        // Region tabs
        b.tvEU.setOnClickListener { switchRegion("EU") }
        b.tvUS.setOnClickListener { switchRegion("US") }
        b.tvUK.setOnClickListener { switchRegion("UK") }

        // Buttons
        b.btnFav.setOnClickListener { requireLogin(session) { toggleFavorite() } }
        b.btnCart.setOnClickListener { requireLogin(session) { addToCart() } }
        b.btnBuy.setOnClickListener { requireLogin(session) { buyNow() } }

        // Load produk
        loadProduct()
        // Sync ikon favorit realtime
        initFavoriteState()
    }

    private fun setupCartBadge() {
        b.badgeDot.visibility = View.GONE
        val user = auth.currentUser ?: return
        cartBadgeReg?.remove()
        cartBadgeReg = db.collection("users").document(user.uid).collection("cart")
            .addSnapshotListener { qs, _ ->
                val hasItems = (qs?.size() ?: 0) > 0
                b.badgeDot.visibility = if (hasItems) View.VISIBLE else View.GONE
            }
    }

    private fun initFavoriteState() {
        favDocReg?.remove()
        val uid = auth.currentUser?.uid ?: run {
            // Belum login -> default outline
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
        b.btnFav.icon = AppCompatResources.getDrawable(requireContext(), iconRes)
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

            // Set judul header = nama sepatu
            b.tvHeaderTitle.text = productName.ifBlank { "Details" }

            b.ivHero.load(productThumb)
            b.tvName.text = productName
            b.tvBest.visibility = if (p.isBestSeller) View.VISIBLE else View.GONE
            b.tvPrice.text = PriceFormatter.rupiah(minPrice)
            b.tvDesc.text = p.description

            // Cek varian per region
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

    private fun addToCart() {
        val uid = auth.currentUser?.uid ?: return
        val sel = selected ?: run {
            Snackbar.make(b.root, "Pilih size dulu", Snackbar.LENGTH_SHORT).show()
            return
        }
        val item = hashMapOf(
            "productId" to productId,
            "variantId" to sel.variantId,
            "name" to productName,
            "thumbnailUrl" to productThumb,
            "region" to currentRegion,
            "size" to sel.size,
            "price" to sel.price,
            "qty" to 1,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("cart").document()
            .set(item)
            .addOnSuccessListener {
                Snackbar.make(b.root, "Masuk keranjang", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(b.root, it.localizedMessage ?: "Gagal menambahkan ke keranjang", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun buyNow() {
        val sel = selected ?: run {
            Snackbar.make(b.root, "Pilih size dulu", Snackbar.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val orderId = StockRepository.placeOrderAndDecrement(productId, sel.variantId, qty = 1, priceEach = sel.price)
                Snackbar.make(b.root, "Order $orderId dibuat", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(b.root, e.message ?: "Gagal membuat order", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favDocReg?.remove(); favDocReg = null
        cartBadgeReg?.remove(); cartBadgeReg = null
        _b = null
    }
}