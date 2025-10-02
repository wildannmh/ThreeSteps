package com.example.projekpemmob.ui.seller

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.projekpemmob.R
import com.example.projekpemmob.core.CloudinaryUploader
import com.example.projekpemmob.databinding.FragmentEditProductBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EditProductFragment : Fragment(R.layout.fragment_edit_product) {
    private var _b: FragmentEditProductBinding? = null
    private val b get() = _b!!
    private val args by navArgs<EditProductFragmentArgs>()

    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val db get() = Firebase.firestore

    private var productId: String? = null
    private var thumbnailUrl: String? = null

    // Adapters per region
    private lateinit var adapterEU: SizeStockAdapter
    private lateinit var adapterUS: SizeStockAdapter
    private lateinit var adapterUK: SizeStockAdapter

    private val brandOptions by lazy {
        resources.getStringArray(R.array.brand_list).toList()
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            lifecycleScope.launch {
                setLoading(true)
                try {
                    val url = withContext(Dispatchers.IO) { CloudinaryUploader.uploadImageBlocking(requireContext(), uri) }
                    thumbnailUrl = url
                    b.ivPreview.load(url)
                    Snackbar.make(b.root, "Gambar terunggah", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(b.root, e.message ?: "Upload gagal", Snackbar.LENGTH_LONG).show()
                } finally { setLoading(false) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentEditProductBinding.bind(view)

        productId = args.productId.ifBlank { null }

        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        // Brand dropdown
        b.actBrand.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, brandOptions)
        )

        // Setup adapters & RV
        adapterEU = SizeStockAdapter()
        adapterUS = SizeStockAdapter()
        adapterUK = SizeStockAdapter()

        b.rvEU.layoutManager = LinearLayoutManager(requireContext())
        b.rvUS.layoutManager = LinearLayoutManager(requireContext())
        b.rvUK.layoutManager = LinearLayoutManager(requireContext())

        b.rvEU.adapter = adapterEU
        b.rvUS.adapter = adapterUS
        b.rvUK.adapter = adapterUK

        // Switch show/hide sections
        b.switchEU.setOnCheckedChangeListener { _, checked -> b.sectionEU.isVisible = checked }
        b.switchUS.setOnCheckedChangeListener { _, checked -> b.sectionUS.isVisible = checked }
        b.switchUK.setOnCheckedChangeListener { _, checked -> b.sectionUK.isVisible = checked }

        // Add size buttons
        b.btnAddEU.setOnClickListener {
            val s = b.etEUSize.text?.toString()?.trim().orEmpty()
            if (s.isBlank()) { toast("Isi size EU dulu"); return@setOnClickListener }
            if (!adapterEU.addRowUnique(s)) toast("Size EU $s sudah ada")
            b.etEUSize.text = null
        }
        b.btnAddUS.setOnClickListener {
            val s = b.etUSSize.text?.toString()?.trim().orEmpty()
            if (s.isBlank()) { toast("Isi size US dulu"); return@setOnClickListener }
            if (!adapterUS.addRowUnique(s)) toast("Size US $s sudah ada")
            b.etUSSize.text = null
        }
        b.btnAddUK.setOnClickListener {
            val s = b.etUKSize.text?.toString()?.trim().orEmpty()
            if (s.isBlank()) { toast("Isi size UK dulu"); return@setOnClickListener }
            if (!adapterUK.addRowUnique(s)) toast("Size UK $s sudah ada")
            b.etUKSize.text = null
        }

        // Load product (if editing)
        productId?.let { id ->
            db.collection("products").document(id).get()
                .addOnSuccessListener { d ->
                    b.etName.setText(d.getString("name").orEmpty())
                    b.etDesc.setText(d.getString("description").orEmpty())
                    b.etBasePrice.setText((d.getDouble("basePrice") ?: 0.0).toString())
                    b.switchActive.isChecked = d.getBoolean("active") ?: true
                    thumbnailUrl = d.getString("thumbnailUrl")
                    b.ivPreview.load(thumbnailUrl)

                    // Prefill brand
                    val brand = d.getString("brand").orEmpty()
                    if (brand.isNotBlank()) b.actBrand.setText(brand, false)
                }

            // Load existing variants -> prefill per region
            db.collection("products").document(id).collection("variants").get()
                .addOnSuccessListener { qs ->
                    val eu = mutableListOf<SizeStockRow>()
                    val us = mutableListOf<SizeStockRow>()
                    val uk = mutableListOf<SizeStockRow>()
                    qs.documents.forEach { d ->
                        val region = d.getString("region") ?: return@forEach
                        val size = d.getString("size") ?: return@forEach
                        val price = (d.getDouble("price") ?: 0.0).toString()
                        val stock = ((d.getLong("stock") ?: 0L).toInt()).toString()
                        val row = SizeStockRow(size, priceText = price, stockText = stock)
                        when (region) {
                            "EU" -> eu.add(row)
                            "US" -> us.add(row)
                            "UK" -> uk.add(row)
                        }
                    }
                    if (eu.isNotEmpty()) {
                        b.switchEU.isChecked = true
                        adapterEU.setRows(eu.sortedBy { it.size })
                    }
                    if (us.isNotEmpty()) {
                        b.switchUS.isChecked = true
                        adapterUS.setRows(us.sortedBy { it.size })
                    }
                    if (uk.isNotEmpty()) {
                        b.switchUK.isChecked = true
                        adapterUK.setRows(uk.sortedBy { it.size })
                    }
                }
        }

        b.btnSave.setOnClickListener { saveProductAndVariants() }
    }

    private fun brandSlug(s: String) = s.trim().lowercase().replace(Regex("\\s+"), "_")

    private fun saveProductAndVariants() {
        val name = b.etName.text?.toString()?.trim().orEmpty()
        val brand = b.actBrand.text?.toString()?.trim().orEmpty()
        val desc = b.etDesc.text?.toString()?.trim().orEmpty()
        val basePrice = b.etBasePrice.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (name.isBlank()) { toast("Nama wajib diisi"); return }
        val allowed = resources.getStringArray(R.array.brand_list).toList()
        if (brand.isBlank() || !allowed.contains(brand)) {
            toast("Pilih merek dari daftar"); return
        }

        setLoading(true)

        val productData = hashMapOf(
            "ownerId" to uid,
            "name" to name,
            "nameLower" to name.lowercase(),
            "brand" to brand,
            "brandKey" to brandSlug(brand),
            "description" to desc,
            "basePrice" to basePrice,
            "minPrice" to basePrice,
            "thumbnailUrl" to (thumbnailUrl ?: ""),
            "active" to b.switchActive.isChecked,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        val ref = productId?.let { db.collection("products").document(it) }
            ?: db.collection("products").document().also {
                productData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                productData["salesCount"] = 0L // default agar ikut urutan dengan benar
            }

        ref.set(productData, SetOptions.merge())
            .addOnSuccessListener {
                lifecycleScope.launch {
                    try {
                        val allPrices = mutableListOf<Double>()

                        if (b.switchEU.isChecked) {
                            allPrices += upsertVariants("EU", ref.id, basePrice, adapterEU.currentRows())
                        }
                        if (b.switchUS.isChecked) {
                            allPrices += upsertVariants("US", ref.id, basePrice, adapterUS.currentRows())
                        }
                        if (b.switchUK.isChecked) {
                            allPrices += upsertVariants("UK", ref.id, basePrice, adapterUK.currentRows())
                        }

                        val minP = (allPrices.minOrNull() ?: basePrice)
                        db.collection("products").document(ref.id).set(
                            mapOf("minPrice" to minP, "updatedAt" to FieldValue.serverTimestamp()),
                            SetOptions.merge()
                        ).await()

                        toast("Produk & varian tersimpan")
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        toast(e.message ?: "Gagal menyimpan varian")
                    } finally {
                        setLoading(false)
                    }
                }
            }
            .addOnFailureListener {
                setLoading(false); toast(it.localizedMessage ?: "Gagal menyimpan produk")
            }
    }

    private suspend fun upsertVariants(
        region: String,
        productId: String,
        basePrice: Double,
        rows: List<SizeStockRow>
    ): List<Double> {
        val batch = db.batch()
        val ref = db.collection("products").document(productId).collection("variants")
        val pricesUsed = mutableListOf<Double>()

        rows.forEach { row ->
            val size = row.size
            val stock = row.stockText.toIntOrNull() ?: 0
            val price = row.priceText.toDoubleOrNull() ?: basePrice
            pricesUsed += price

            val id = "$region-$size"
            val data = hashMapOf(
                "region" to region,
                "size" to size,
                "price" to price,
                "stock" to stock,
                "active" to true,
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            batch.set(ref.document(id), data, SetOptions.merge())
        }
        batch.commit().await()
        return pricesUsed
    }

    private fun setLoading(v: Boolean) {
        b.btnPickImage.isEnabled = !v
        b.btnSave.isEnabled = !v
        b.progress.isGone = !v
        b.progress.isVisible = v
    }

    private fun toast(msg: String) {
        Snackbar.make(b.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}