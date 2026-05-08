package com.example.tectest.ui.home

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
// import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.tectest.R
import com.example.tectest.databinding.ActivityHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    // private lateinit var appBarConfiguration: AppBarConfiguration // Only needed if HomeActivity has its own Toolbar that interacts with NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment
        navController = navHostFragment.navController

        val navView: BottomNavigationView = binding.navView

        // This connects the BottomNavigationView to the NavController.
        // It handles selecting the correct tab when navigating and navigating to the correct start destination when a tab is clicked.
        navView.setupWithNavController(navController)

        navView.setOnItemReselectedListener { item ->
            val reselectedDestinationId = item.itemId
            navController.popBackStack(reselectedDestinationId, false, saveState = true)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("HomeActivity", "Destination changed to: ${destination.label} (ID: ${destination.id})")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}