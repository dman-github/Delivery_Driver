package com.okada.android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.okada.android.Utils.UserUtils
import com.okada.android.databinding.ActivityDriverHomeBinding
import java.util.HashMap

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDriverHomeBinding
    private lateinit var navView: NavigationView
    private lateinit var img_avatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private lateinit var drawerLayout: DrawerLayout
    private var imageUri: Uri? = null
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
        storageReference = FirebaseStorage.getInstance().reference
        waitingDialog = AlertDialog.Builder(this).setMessage(getString(R.string.waiting_msg))
            .setCancelable(false).create()
        drawerLayout = findViewById(R.id.drawer_layout)
        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_exit) {
                var builder = AlertDialog.Builder(this@DriverHomeActivity)
                builder.setTitle(R.string.menu_logout)
                    .setMessage(R.string.sign_out_msg)
                    .setNegativeButton(R.string.cancel_string) { dialogInterface, _ -> dialogInterface.dismiss() }
                    .setPositiveButton(R.string.menu_logout) { dialogInterface, _ ->

                        FirebaseAuth.getInstance().signOut()
                        /*val intent =
                            Intent(this@DriverHomeActivity, SplashScreenActivity::class.java)
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)*/
                        finish()
                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener {
                    val priColor = MaterialColors.getColor(this, android.R.attr.colorPrimary, Color.WHITE);
                    val accColor = MaterialColors.getColor(this, android.R.attr.colorAccent, Color.WHITE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(priColor)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(accColor)
                }

                dialog.show()
            } else {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            return@setNavigationItemSelectedListener true
        }
        val headerView = navView.getHeaderView(0)
        val text_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val text_email = headerView.findViewById<View>(R.id.txt_email) as TextView
        val text_star = headerView.findViewById<View>(R.id.txt_star) as TextView
        img_avatar = headerView.findViewById(R.id.img_avatar)
        Common.currentUser?.let { user ->
            text_name.text = Common.buildWelcomeMessage()
            text_email.text = user.email
            text_star.text = StringBuilder().append(user.rating)
            if (user.avatar.isNotEmpty()) {
                Glide.with(this)
                    .load(user.avatar)
                    .into(img_avatar)
            }
        }
        img_avatar.setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            val chooserIntent =
                Intent.createChooser(intent, getString(R.string.select_picture_string))
            resultLauncher.launch(chooserIntent)
        }
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { data ->
                    imageUri = data
                    img_avatar.setImageURI(imageUri)
                    showDialogUpload()
                }
            }
        }

    private fun showDialogUpload() {
        var builder = AlertDialog.Builder(this@DriverHomeActivity)
        builder.setTitle(R.string.change_avatar)
            .setMessage(R.string.change_avatar_msg)
            .setNegativeButton(R.string.cancel_string) { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton(R.string.change_str) { _, _ ->
                imageUri?.let { uri ->
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        waitingDialog.show()
                        val avatarFolder = storageReference.child("avatars/$uid")
                        avatarFolder.putFile(uri)
                            .addOnFailureListener { e ->
                                e.message?.let { Snackbar.make(drawerLayout, it, Snackbar.LENGTH_LONG).show() }
                                waitingDialog.dismiss()
                            }.addOnCompleteListener{task->
                                if (task.isSuccessful) {
                                    avatarFolder.downloadUrl.addOnSuccessListener {uri ->
                                        //Get the location of the image in Firebase store and save it in the RT database
                                        val updateData = HashMap<String,Any>()
                                        updateData.put("avatar", uri.toString())
                                        UserUtils.UpdateUser(drawerLayout, updateData)
                                    }
                                }
                                Handler(Looper.getMainLooper()).postDelayed({
                                    waitingDialog.dismiss()
                                }, 1000)
                            }.addOnProgressListener {taskSnapshot ->
                                val progress = (100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount)
                                Log.i("App_Info", "taskSnapshot  ${taskSnapshot.bytesTransferred}    progress: $progress")
                                waitingDialog.setMessage(StringBuilder(getString(R.string.uploading_msg))
                                    .append(progress)
                                    .append("%"))
                            }
                    }
                }
            }.setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val priColor = MaterialColors.getColor(this, android.R.attr.colorPrimary, Color.WHITE);
            val accColor = MaterialColors.getColor(this, android.R.attr.colorAccent, Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(priColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(accColor)
        }

        dialog.show()
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