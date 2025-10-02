package com.example.projekpemmob.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.projekpemmob.databinding.ItemCartRowBinding
import com.example.projekpemmob.util.PriceFormatter

data class CartRow(
    val id: String,
    val productId: String,
    val variantId: String,
    val name: String,
    val thumbnailUrl: String,
    val size: String,
    val region: String,
    val price: Double,
    val qty: Int
)

class CartAdapter(
    private val onMinus: (CartRow) -> Unit,
    private val onPlus: (CartRow) -> Unit,
    private val onRemove: (CartRow) -> Unit,
    private val onItemClick: (CartRow) -> Unit   // NEW: klik pada kartu
) : ListAdapter<CartRow, CartAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CartRow>() {
            override fun areItemsTheSame(o: CartRow, n: CartRow) = o.id == n.id
            override fun areContentsTheSame(o: CartRow, n: CartRow) = o == n
        }
    }

    inner class VH(val b: ItemCartRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemCartRowBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getItem(position)
        with(holder.b) {
            ivThumb.load(row.thumbnailUrl)
            tvName.text = row.name
            tvVariant.text = "${row.region} • ${row.size}"
            tvPrice.text = PriceFormatter.rupiah(row.price)
            tvQty.text = row.qty.toString()

            // Aksi tombol
            btnMinus.setOnClickListener { onMinus(row) }
            btnPlus.setOnClickListener { onPlus(row) }
            btnRemove.setOnClickListener { onRemove(row) }

            // Klik area kartu (root)
            root.setOnClickListener { onItemClick(row) }
        }
    }
}