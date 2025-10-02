package com.example.projekpemmob.ui.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.projekpemmob.databinding.ItemSearchResultBinding
import com.example.projekpemmob.util.PriceFormatter

data class SearchRowItem(
    val id: String,
    val name: String,
    val price: Double,
    val thumbnailUrl: String
)

class SearchResultAdapter(
    private val onClick: (SearchRowItem) -> Unit,
    private val onAdd: (SearchRowItem) -> Unit
) : ListAdapter<SearchRowItem, SearchResultAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SearchRowItem>() {
            override fun areItemsTheSame(o: SearchRowItem, n: SearchRowItem) = o.id == n.id
            override fun areContentsTheSame(o: SearchRowItem, n: SearchRowItem) = o == n
        }
    }

    inner class VH(val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemSearchResultBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            ivThumb.load(item.thumbnailUrl)
            tvName.text = item.name
            tvPrice.text = PriceFormatter.rupiah(item.price)
            root.setOnClickListener { onClick(item) }
        }
    }
}