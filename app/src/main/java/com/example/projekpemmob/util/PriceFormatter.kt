package com.example.projekpemmob.util

import java.text.NumberFormat
import java.util.Locale

object PriceFormatter {
    private val idLocale = Locale("in", "ID")
    private val nf = NumberFormat.getCurrencyInstance(idLocale)
    fun rupiah(amount: Double): String {
        return try { nf.format(amount) } catch (_: Exception) { "Rp ${"%,.0f".format(amount)}" }
    }
}