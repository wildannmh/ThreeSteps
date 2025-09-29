package com.example.projekpemmob.ui.seller

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.projekpemmob.databinding.ItemSizeStockBinding

data class SizeStockRow(
    val size: String,
    var priceText: String = "",
    var stockText: String = ""
)

class SizeStockAdapter(
    private val rows: MutableList<SizeStockRow> = mutableListOf()
) : RecyclerView.Adapter<SizeStockAdapter.VH>() {

    inner class VH(val b: ItemSizeStockBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemSizeStockBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = rows[position]
        val vb = holder.b

        vb.tvSize.text = item.size

        (vb.etPrice.tag as? TextWatcher)?.let { vb.etPrice.removeTextChangedListener(it) }
        (vb.etStock.tag as? TextWatcher)?.let { vb.etStock.removeTextChangedListener(it) }

        if (vb.etPrice.text?.toString() != item.priceText) vb.etPrice.setText(item.priceText)
        if (vb.etStock.text?.toString() != item.stockText) vb.etStock.setText(item.stockText)

        val priceWatcher = vb.etPrice.addTextChangedListener(afterTextChanged = { s ->
            item.priceText = s?.toString()?.trim().orEmpty()
        })
        vb.etPrice.tag = priceWatcher

        val stockWatcher = vb.etStock.addTextChangedListener(afterTextChanged = { s ->
            item.stockText = s?.toString()?.trim().orEmpty()
        })
        vb.etStock.tag = stockWatcher
    }

    override fun getItemCount() = rows.size

    fun setRows(newRows: List<SizeStockRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    fun addRowUnique(size: String): Boolean {
        if (rows.any { it.size == size }) return false
        rows.add(SizeStockRow(size))
        notifyItemInserted(rows.lastIndex)
        return true
    }

    fun currentRows(): List<SizeStockRow> = rows
}