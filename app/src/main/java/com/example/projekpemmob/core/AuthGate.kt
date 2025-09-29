package com.example.projekpemmob.core

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.projekpemmob.R

fun Fragment.requireLogin(session: SessionManager, onAllowed: () -> Unit) {
    if (session.canUseRestrictedFeature()) {
        onAllowed()
    } else {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Login diperlukan")
            .setMessage("Anda perlu login untuk menggunakan fitur ini.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Sign In") { _, _ ->
                findNavController().navigate(R.id.signInFragment)
            }
            .show()
    }
}