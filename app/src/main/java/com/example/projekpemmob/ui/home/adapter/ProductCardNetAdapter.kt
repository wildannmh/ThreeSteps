package com.example.projekpemmob.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.ItemProductCardBinding
import com.example.projekpemmob.util.PriceFormatter

data class ProductCardItem(
    val id: String,
    val name: String,
    val minPrice: Double,
    val thumbnailUrl: String,
    val isFavorite: Boolean = false
)

class ProductCardNetAdapter(
    private val onCardClick: (ProductCardItem) -> Unit,
    private val onFavToggle: (ProductCardItem) -> Unit,
    private val fullWidth: Boolean = false // Favorites page bisa full width
) : ListAdapter<ProductCardItem, ProductCardNetAdapter.VH>(DIFF) {

    companion object {
        val DIFF: DiffUtil.ItemCallback<ProductCardItem> = object : DiffUtil.ItemCallback<ProductCardItem>() {
            override fun areItemsTheSame(o: ProductCardItem, n: ProductCardItem) = o.id == n.id
            override fun areContentsTheSame(o: ProductCardItem, n: ProductCardItem) = o == n
        }
    }

    inner class VH(val b: ItemProductCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemProductCardBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            // Optional: pakai full width (untuk halaman Favorites)
            if (fullWidth) {
                val lp = root.layoutParams ?: RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                lp.width = LayoutParams.MATCH_PARENT
                lp.height = LayoutParams.WRAP_CONTENT
                root.layoutParams = lp
            }

            ivThumb.load(item.thumbnailUrl)
            tvName.text = item.name
            tvPrice.text = PriceFormatter.rupiah(item.minPrice)

            // Set icon heart sesuai status
            btnFav.setImageResource(if (item.isFavorite) R.drawable.ic_heart_filled_24 else R.drawable.ic_heart_24)
            btnFav.contentDescription = if (item.isFavorite) "Unfavorite" else "Favorite"

            root.setOnClickListener { onCardClick(item) }
            btnFav.setOnClickListener { onFavToggle(item) }
        }
    }
}