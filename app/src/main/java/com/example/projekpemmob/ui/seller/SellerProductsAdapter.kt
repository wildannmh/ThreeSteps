package com.example.projekpemmob.ui.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.ItemSellerProductBinding

data class SellerProductItem(
    val id: String,
    val name: String,
    val minPrice: Double,
    val thumbnailUrl: String
)

class SellerProductsAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<SellerProductItem, SellerProductsAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SellerProductItem>() {
            override fun areItemsTheSame(o: SellerProductItem, n: SellerProductItem) = o.id == n.id
            override fun areContentsTheSame(o: SellerProductItem, n: SellerProductItem) = o == n
        }
    }

    inner class VH(val b: ItemSellerProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemSellerProductBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        // Fallback gambar: jika URL kosong -> pakai drawable
        val model: Any = item.thumbnailUrl.takeIf { it.isNotBlank() } ?: R.drawable.ic_shoe_blue
        holder.b.ivThumb.load(model)

        holder.b.tvName.text = item.name
        holder.b.tvPrice.text = "$" + String.format("%.2f", item.minPrice)

        holder.b.root.setOnClickListener { onClick(item.id) }
    }
}