package com.example.runtracker.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.FrameLayout
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.runtracker.R
import com.example.runtracker.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var navHostFragment: NavHostFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar : MaterialToolbar = findViewById(R.id.toolbar)

        navigateToTrackingFragmentIfNeeded(intent)

        setSupportActionBar(toolbar) //ID OF CUSTOM TOOLBAR WE SPECIFIED
        val bottomNavigationView : BottomNavigationView = findViewById(R.id.bottomNavigationView)
        // MAIN CHANGER MADE AFTER THIS

//        val flFragment : FrameLayout = findViewById(R.id.flFragment)
//        val navHostFragment : Fragment = flFragment.findFragment()
//
//        val navController = findNavController(R.id.navHostFragment)

        navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnItemReselectedListener{
            // NO OPERATION
        }

        navHostFragment.findNavController().addOnDestinationChangedListener{
            _, destination, _ ->
            when(destination.id){
                R.id.settingsFragment,R.id.runFragment,R.id.statisticsFragment ->
                    bottomNavigationView.visibility = View.VISIBLE
                else-> bottomNavigationView.visibility = View.GONE
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            navHostFragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }
}