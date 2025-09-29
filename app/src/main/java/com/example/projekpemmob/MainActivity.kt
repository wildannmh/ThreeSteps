package com.example.projekpemmob

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.projekpemmob.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var navController: NavController

    // Header & BottomNav hanya di 4 halaman ini
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.favoriteFragment,
        R.id.activityFragment,
        R.id.profileFragment
    )

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db get() = Firebase.firestore
    private var cartBadgeReg: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController

        // Bottom nav auto navigate + highlight
        b.bottomNavigationView.setupWithNavController(navController)
        b.bottomNavigationView.setOnItemReselectedListener { /* no-op */ }

        // btnHeaderLeft: logo → ke Home
        b.btnHeaderLeft.setOnClickListener { navController.navigate(R.id.homeFragment) }
        // Cart button
        b.btnHeaderCart.setOnClickListener { navController.navigate(R.id.cartFragment) }

        // Atur visibilitas bars + judul dari label destination
        navController.addOnDestinationChangedListener { _, dest, _ ->
            val showBars = topLevelDestinations.contains(dest.id)
            b.headerContainer.isVisible = showBars
            b.bottomNavigationView.isVisible = showBars

            // Judul mengikuti label fragment (nav_graph)
            b.tvHeaderTitle.text = when (dest.id) {
                R.id.homeFragment -> "Home"
                R.id.favoriteFragment -> "Favorite"
                R.id.activityFragment -> "Activity"
                R.id.profileFragment -> "Profile"
                else -> dest.label?.toString().orEmpty()
            }
        }

        setupCartBadge()
    }

    private fun setupCartBadge() {
        b.headerBadgeDot.isVisible = false
        val user = auth.currentUser ?: return
        cartBadgeReg?.remove()
        cartBadgeReg = db.collection("users").document(user.uid).collection("cart")
            .addSnapshotListener { qs, _ ->
                b.headerBadgeDot.isVisible = (qs?.size() ?: 0) > 0
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cartBadgeReg?.remove()
    }
}