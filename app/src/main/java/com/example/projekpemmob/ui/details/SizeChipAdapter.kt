package com.example.projekpemmob.ui.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projekpemmob.databinding.ItemSizeChipBinding

data class SizeChip(
    val size: String,
    val stock: Int,
    val variantId: String,
    val price: Double,
    val selected: Boolean = false
)

class SizeChipAdapter(
    private val onSelect: (SizeChip) -> Unit
) : ListAdapter<SizeChip, SizeChipAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SizeChip>() {
            override fun areItemsTheSame(oldItem: SizeChip, newItem: SizeChip) =
                oldItem.variantId == newItem.variantId

            override fun areContentsTheSame(oldItem: SizeChip, newItem: SizeChip) =
                oldItem == newItem
        }
    }

    inner class VH(val b: ItemSizeChipBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemSizeChipBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chip = getItem(position)
        with(holder.b) {
            tvLabel.text = chip.size

            val enabled = chip.stock > 0
            root.isEnabled = enabled
            root.alpha = if (enabled) 1f else 0.4f

            // State selected untuk background & text selector
            card.isSelected = chip.selected
            tvLabel.isSelected = chip.selected

            // Elevasi saat selected agar ada bayangan
            val elev = if (chip.selected)
                root.resources.getDimension(com.example.projekpemmob.R.dimen.size_chip_elev_selected)
            else 0f
            card.cardElevation = elev

            root.setOnClickListener {
                if (enabled) onSelect(chip)
            }
        }
    }
}