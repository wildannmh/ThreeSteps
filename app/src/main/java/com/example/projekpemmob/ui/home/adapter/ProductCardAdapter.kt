package com.example.projekpemmob.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projekpemmob.data.Product
import com.example.projekpemmob.databinding.ItemProductCardBinding
import com.example.projekpemmob.util.PriceFormatter

class ProductCardAdapter(
    private val onCardClick: (Product) -> Unit,
    private val onFavClick: (Product) -> Unit,
    private val smallCard: Boolean = false
) : ListAdapter<Product, ProductCardAdapter.VH>(DIFF) {

    companion object {
        val DIFF: DiffUtil.ItemCallback<Product> = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(val b: ItemProductCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemProductCardBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item: Product = getItem(position)
        val b = holder.b

        // Sesuaikan dengan item_product_card.xml (ivThumb, tvName, tvPrice, btnFav)
        b.ivThumb.setImageResource(item.imageRes)
        b.tvName.text = item.name
        b.tvPrice.text = PriceFormatter.rupiah(item.price)

        if (smallCard) {
            val lp = b.root.layoutParams
            lp?.height = b.root.resources.getDimensionPixelSize(com.example.projekpemmob.R.dimen.card_small_height)
            b.root.layoutParams = lp
        }

        b.root.setOnClickListener { onCardClick(item) }
        b.btnFav.setOnClickListener { onFavClick(item) }
    }
}