package com.example.projekpemmob.core

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session_prefs", MODE_PRIVATE)

    fun signInAsGuest() {
        prefs.edit().putBoolean(KEY_IS_GUEST, true).apply()
    }
    fun clearGuest() {
        prefs.edit().remove(KEY_IS_GUEST).apply()
    }

    fun isGuest(): Boolean = prefs.getBoolean(KEY_IS_GUEST, false)
    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null
    fun canUseRestrictedFeature(): Boolean = isLoggedIn()

    companion object {
        private const val KEY_IS_GUEST = "is_guest"
    }
}