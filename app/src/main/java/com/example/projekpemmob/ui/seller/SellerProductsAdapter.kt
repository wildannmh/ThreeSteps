package com.example.projekpemmob.ui.seller

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.projekpemmob.databinding.ItemSellerProductBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class SellerProductItem(
    val id: String,
    val name: String,
    val minPrice: Double,
    val thumbnailUrl: String
)

class SellerProductsAdapter(
    private val onEdit: (String) -> Unit
) : ListAdapter<SellerProductItem, SellerProductsAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SellerProductItem>() {
            override fun areItemsTheSame(a: SellerProductItem, b: SellerProductItem) = a.id == b.id
            override fun areContentsTheSame(a: SellerProductItem, b: SellerProductItem) = a == b
        }
    }

    inner class VH(val b: ItemSellerProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemSellerProductBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            tvName.text = item.name
            tvPrice.text = "Rp${"%,.0f".format(item.minPrice)}"
            ivThumb.load(item.thumbnailUrl.ifBlank { null }) {
                placeholder(com.example.projekpemmob.R.drawable.ic_shoe_blue)
                error(com.example.projekpemmob.R.drawable.ic_shoe_blue)
            }

            // Edit produk jika klik item (bukan tombol hapus)
            root.setOnClickListener { onEdit(item.id) }

            // Hapus produk dengan konfirmasi
            btnDelete.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Hapus Produk")
                    .setMessage("Yakin ingin menghapus produk \"${item.name}\"?")
                    .setPositiveButton("Hapus") { dialog, _ ->
                        dialog.dismiss()
                        // Hapus dari Firestore
                        Firebase.firestore.collection("products").document(item.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(holder.itemView.context, "Produk dihapus", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(holder.itemView.context, "Gagal hapus: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
}