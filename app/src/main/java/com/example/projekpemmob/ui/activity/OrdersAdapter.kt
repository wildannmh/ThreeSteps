package com.example.projekpemmob.ui.activity

import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projekpemmob.R
import com.example.projekpemmob.databinding.ItemOrderRowBinding
import com.example.projekpemmob.util.PriceFormatter
import java.util.Date
import java.util.Locale

data class OrderRow(
    val id: String,
    val total: Double,
    val status: String, // pending | awaiting_payment | paid | cancelled | expired | ...
    val itemsCount: Int,
    val createdAt: Date?,
    val itemsLabel: String // contoh: "Nike Air Max, Converse Chuck 70 +1 lainnya"
)

class OrdersAdapter(
    private val onClick: (OrderRow) -> Unit
) : ListAdapter<OrderRow, OrdersAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<OrderRow>() {
            override fun areItemsTheSame(oldItem: OrderRow, newItem: OrderRow) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: OrderRow, newItem: OrderRow) = oldItem == newItem
        }
    }

    inner class VH(val b: ItemOrderRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemOrderRowBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = getItem(position)
        with(holder.b) {
            // Tampilkan Order #XXXXXX (6 char terakhir)
            val shortId = if (row.id.length > 6) row.id.takeLast(6) else row.id
            tvOrderId.text = "Order #$shortId"

            // Status chip berwarna
            tvStatus.text = row.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            tvStatus.backgroundTintList = ColorStateList.valueOf(statusColor(tvStatus.context, row.status))
            tvStatus.setTextColor(ContextCompat.getColor(tvStatus.context, android.R.color.white))

            // Total, daftar item, dan metadata
            tvTotal.text = PriceFormatter.rupiah(row.total)
            tvItems.text = row.itemsLabel

            val whenText = row.createdAt?.let {
                DateUtils.getRelativeTimeSpanString(it.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
            } ?: "-"
            tvMeta.text = "${row.itemsCount} item${if (row.itemsCount == 1) "" else "s"} • $whenText"

            root.setOnClickListener { onClick(row) }
        }
    }

    private fun statusColor(ctx: android.content.Context, status: String): Int {
        return when (status.lowercase()) {
            "paid" -> 0xFF2E7D32.toInt()        // green 600
            "awaiting_payment", "pending" -> 0xFFF9A825.toInt() // amber 700
            "cancelled" -> 0xFFC62828.toInt()   // red 700
            "expired" -> 0xFF6D4C41.toInt()     // brown 600
            else -> 0xFF607D8B.toInt()          // blue grey 500
        }
    }
}