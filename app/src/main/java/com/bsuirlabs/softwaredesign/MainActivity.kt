package com.bsuirlabs.softwaredesign

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        MainFragment.OnFragmentInteractionListener, ProfileFragment.OnFragmentInteractionListener,
        ProfileEditFragment.OnFragmentInteractionListener, LoginFragment.OnFragmentInteractionListener,
        RegistrationFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {}

    private lateinit var navController: NavController

    private val MainFragmentPageNumber = "1"
    private val ProfileFragmentPageNumber = "2"
    private val RssFragmentPageNumber = "3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startAuthActivity()
            return
        }

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
        setProfileEmail(currentUser.email.toString())

        val userProfileListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userProfile = dataSnapshot.getValue(UserProfile::class.java)
                if (userProfile != null) {
                    val fullName = """${userProfile.firstName} ${userProfile.lastName}"""
                    getNameTextViewFromNavView().text = fullName
                    if (!userProfile.image.isBlank()) {
                        val imageReference = FirebaseStorage.getInstance()
                                .getReference(userProfile.image)
                        GlideApp.with(applicationContext)
                                .load(imageReference)
                                .into(getProfileImageViewFromNavView())
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(ContentValues.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        FirebaseDatabase.getInstance().reference.child(currentUser.uid).addValueEventListener(userProfileListener)

        openFragmentFromIntentPath()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?.childFragmentManager?.fragments?.get(0)
        if (currentFragment != null
                && navController.currentDestination?.id == R.id.profileEditFragment
                && (currentFragment as ProfileEditFragment).dataChanged) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.unsaved_data_message))
                    .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                        startActivity(Intent(this, AboutActivity::class.java))
                    }.setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            builder.show()
            return false
        }
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.action_change_rss -> {
                navController.navigate(R.id.action_rssFragment_to_changeRssSourceFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.mainFragment)
            }
            R.id.nav_profile -> {
                navController.navigate(R.id.profileFragment)
            }
            R.id.nav_rss -> {
                navController.navigate(R.id.rssFragment)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun startAuthActivity() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun setProfileEmail(email: String) {
        val headerView = nav_view.getHeaderView(0)
        val textView = headerView.findViewById(R.id.nav_header_profile_email) as TextView
        textView.text = email
    }

    fun getNameTextViewFromNavView() : TextView {
        val headerView = nav_view.getHeaderView(0)
        return headerView.findViewById(R.id.nav_header_profile_name) as TextView
    }

    fun getProfileImageViewFromNavView() : ImageView {
        val headerView = nav_view.getHeaderView(0)
        return headerView.findViewById(R.id.nav_header_profile_icon) as ImageView
    }

    private fun openFragmentFromIntentPath() {
        if (intent?.data != null) {
            val number = intent?.data.toString().split("/").last()
            when (number) {
                MainFragmentPageNumber -> {
                    navController.navigate(R.id.mainFragment)
                }
                ProfileFragmentPageNumber -> {
                    navController.navigate(R.id.profileFragment)
                }
                RssFragmentPageNumber -> {
                    navController.navigate(R.id.rssFragment)
                }
            }
        }
    }

    fun cleanArticlesCache () {
        getSharedPreferences("data", Context.MODE_PRIVATE)
                .edit()
                .putString("articles", "")
                .apply()
    }
}
