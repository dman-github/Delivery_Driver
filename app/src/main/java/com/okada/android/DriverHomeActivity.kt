package com.okada.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.okada.android.databinding.ActivityDriverHomeBinding
import com.vmadalin.easypermissions.EasyPermissions

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDriverHomeBinding
    private lateinit var navView: NavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarDriverHome.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    private fun init() {
        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_exit) {
                var builder = AlertDialog.Builder(this@DriverHomeActivity)
                builder.setTitle(R.string.menu_logout)
                    .setMessage(R.string.sign_out_msg)
                    .setNegativeButton(R.string.cancel_string) { dialogInterface, _ -> dialogInterface.dismiss() }
                    .setPositiveButton(R.string.menu_logout) {dialogInterface, _ ->

                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@DriverHomeActivity, SplashScreenActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener{
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(resources.getColor(R.color.app_yellow,null))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(resources.getColor(R.color.app_Light_grey,null))
                }

                dialog.show()
            }
            true
        }
        val headerView = navView.getHeaderView(0)
        val text_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val text_email = headerView.findViewById<View>(R.id.txt_email) as TextView
        val text_star = headerView.findViewById<View>(R.id.txt_star) as TextView
        text_name.setText(Common.buildWelcomeMessage())
        text_email.setText(Common.currentUser!!.email)
        text_star.setText(StringBuilder().append(Common.currentUser!!.rating))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}