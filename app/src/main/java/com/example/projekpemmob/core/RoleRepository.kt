package com.example.projekpemmob.core

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object RoleRepository {
    suspend fun getRole(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return "guest"
        val snap = Firebase.firestore.collection("users").document(uid).get().await()
        return snap.getString("role") ?: "buyer"
    }
}