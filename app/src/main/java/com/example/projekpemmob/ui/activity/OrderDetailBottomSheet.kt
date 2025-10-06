package com.example.projekpemmob.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.projekpemmob.R
import com.example.projekpemmob.util.PriceFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderDetailBottomSheet : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(orderId: String): OrderDetailBottomSheet {
            val frag = OrderDetailBottomSheet()
            val args = Bundle()
            args.putString("orderId", orderId)
            frag.arguments = args
            return frag
        }
    }

    private val db get() = Firebase.firestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottomsheet_order_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val orderId = arguments?.getString("orderId") ?: return

        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)
        val tvSubtotal = view.findViewById<TextView>(R.id.tvSubtotal)
        val tvShipping = view.findViewById<TextView>(R.id.tvShipping)
        val tvReceiver = view.findViewById<TextView>(R.id.tvReceiver)
        val tvAddress = view.findViewById<TextView>(R.id.tvAddress)
        val tvCourier = view.findViewById<TextView>(R.id.tvCourier)
        val layoutItems = view.findViewById<LinearLayout>(R.id.layoutItems)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyItems)

        MainScope().launch {
            val doc = db.collection("orders").document(orderId).get().await()
            if (!doc.exists()) {
                tvOrderId.text = "Order: $orderId"
                tvStatus.text = "Tidak ditemukan"
                tvTotal.text = "-"
                tvReceiver.text = "-"
                tvAddress.text = "-"
                tvCourier.text = "-"
                tvSubtotal.text = "-"
                tvShipping.text = "-"
                tvEmpty.visibility = View.VISIBLE
                return@launch
            }
            tvOrderId.text = "Order #${orderId.takeLast(6)}"
            tvStatus.text = doc.getString("status")?.replaceFirstChar { it.uppercase() } ?: "-"
            tvTotal.text = "Total: " + PriceFormatter.rupiah(doc.getDouble("total") ?: 0.0)
            tvSubtotal.text = "Subtotal: " + PriceFormatter.rupiah(doc.getDouble("subtotal") ?: 0.0)
            tvShipping.text = "Ongkir: " + PriceFormatter.rupiah(doc.getDouble("shippingCost") ?: 0.0)

            val shipping = doc.get("shipping") as? Map<*, *>
            if (shipping != null) {
                tvReceiver.text = "Penerima: " + (shipping["receiver"] as? String ?: "-")
                tvAddress.text = "Alamat: ${(shipping["addressLine"] as? String ?: "-")}, ${(shipping["city"] as? String ?: "-")}, ${(shipping["postalCode"] as? String ?: "-")}"
                tvCourier.text = "Kurir: " + (shipping["courier"] as? String ?: "-")
            } else {
                tvReceiver.text = "-"
                tvAddress.text = "-"
                tvCourier.text = "-"
            }

            layoutItems.removeAllViews()
            @Suppress("UNCHECKED_CAST")
            val items = doc.get("items") as? List<Map<String, Any?>>
            if (items.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                tvEmpty.visibility = View.GONE
                items.forEach { item ->
                    val name = item["name"] as? String ?: "-"
                    val size = item["size"] as? String ?: "-"
                    val region = item["region"] as? String ?: ""
                    val qty = (item["qty"] as? Number)?.toInt() ?: 1
                    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                    val tv = TextView(requireContext())
                    tv.text = "$name $region $size x$qty - ${PriceFormatter.rupiah(price * qty)}"
                    tv.setPadding(0, 0, 0, 8)
                    layoutItems.addView(tv)
                }
            }
        }

        when (tvStatus.text.toString().lowercase()) {
            "paid" -> tvStatus.setTextColor(resources.getColor(R.color.green_600, null))
            "awaiting_payment", "pending" -> tvStatus.setTextColor(resources.getColor(R.color.amber_700, null))
            "cancelled" -> tvStatus.setTextColor(resources.getColor(R.color.red_700, null))
            "expired" -> tvStatus.setTextColor(resources.getColor(R.color.brown_600, null))
            else -> tvStatus.setTextColor(resources.getColor(R.color.blue_grey_500, null))
        }
    }
}