package com.bsuirlabs.softwaredesign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        MainFragment.OnFragmentInteractionListener, ProfileFragment.OnFragmentInteractionListener,
        ProfileEditFragment.OnFragmentInteractionListener, LoginFragment.OnFragmentInteractionListener,
        RegistrationFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {}

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = resources.getInteger(R.integer.screen_orientation)

        setSupportActionBar(toolbar)
        navController = findNavController(R.id.nav_host_fragment)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.nav_home)

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.mainFragment)
            }
            R.id.nav_profile -> {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    navController.navigate(R.id.profileFragment)
                } else {
                    navController.navigate(R.id.loginFragment)
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun startAuthActivity() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
