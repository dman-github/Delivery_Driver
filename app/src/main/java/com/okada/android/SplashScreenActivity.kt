package com.okada.android

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.okada.android.Model.DriverInfoModel
import com.okada.android.Utils.UserUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit


class SplashScreenActivity : AppCompatActivity() {
    companion object {
        private val LOGIN_REQUEST_CODE = 0x1122
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference
    private lateinit var progressBar : ProgressBar
    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null) {
            firebaseAuth.removeAuthStateListener { listener }
        }
        super.onStop()
    }

    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe{
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        progressBar = findViewById(R.id.progress_bar)
        initialise()
    }

    private fun initialise() {
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)

        providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener {instance ->
            val user = instance.currentUser
            var userId = instance.uid
            if (user != null) {
                /*Toast.makeText(this@SplashScreenActivity,
                    "Welcome: New User, id: $userId", Toast.LENGTH_SHORT)
                    .show();*/
                FirebaseMessaging.getInstance().token
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "SplashScreen: ${e.message}", Toast.LENGTH_SHORT
                        ).show();
                    }.addOnSuccessListener {token->
                        UserUtils.updateToken(this@SplashScreenActivity, token)
                    }
                checkUserFromFirebase();
            } else {
                showLoginLayout()
            }
        }
        // Check permissions needed in new Android version for Push notifications
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 1)
            };
        }

    }

    private fun checkUserFromFirebase() {
        // Use Let to check Nullable types
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            driverInfoRef
                .child(it)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(dataSnapShot: DataSnapshot) {
                        if (dataSnapShot.exists()) {
                            /*Toast.makeText(this@SplashScreenActivity,
                                "User already registered!", Toast.LENGTH_SHORT)
                                .show();*/
                            val model = dataSnapShot.getValue(DriverInfoModel::class.java)
                            gotoHomeActivity(model)
                        } else {
                            showRegisterLayout()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SplashScreenActivity,
                                  "Error: $error", Toast.LENGTH_SHORT)
                                  .show();
                    }

                })
        }
    }

    private fun gotoHomeActivity(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this,R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edtFirstName = itemView.findViewById<View>(R.id.edit_first_name) as TextInputEditText
        val edtLastName = itemView.findViewById<View>(R.id.edit_last_name) as TextInputEditText
        val edtEmail = itemView.findViewById<View>(R.id.email_address) as TextInputEditText
        val edtId = itemView.findViewById<View>(R.id.id_number) as TextInputEditText
        val buttonContinue = itemView.findViewById<View>(R.id.btn_register) as Button

        FirebaseAuth.getInstance().currentUser?.email?.let {email->
            if (emailValidation(email)) {
                // View
                builder.setView(itemView)
                val dialog = builder.create()
                dialog.show()
                //Event
                buttonContinue.setOnClickListener {
                    if (TextUtils.isEmpty(edtFirstName.text.toString())) {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "${resources.getString(R.string.error_name)}: ${resources.getString(R.string.error_firstname_message)}", Toast.LENGTH_SHORT)
                            .show();
                        return@setOnClickListener
                    } else if (TextUtils.isEmpty(edtLastName.text.toString())) {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "${resources.getString(R.string.error_name)}: ${resources.getString(R.string.error_lastname_message)}", Toast.LENGTH_SHORT)
                            .show();
                        return@setOnClickListener
                    } else if (TextUtils.isEmpty(edtEmail.text.toString())) {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "${resources.getString(R.string.error_name)}: ${resources.getString(R.string.error_email_message)}", Toast.LENGTH_SHORT)
                            .show();
                        return@setOnClickListener

                    } else if (TextUtils.isEmpty(edtId.text.toString())) {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "${resources.getString(R.string.error_name)}: ${resources.getString(R.string.error_id_message)}", Toast.LENGTH_SHORT)
                            .show();
                        return@setOnClickListener
                    } else {
                        var model = DriverInfoModel()
                        model.firstName =  edtFirstName.text.toString()
                        model.lastName = edtLastName.text.toString()
                        model.email = edtEmail.text.toString()
                        model.id = edtId.text.toString()
                        model.rating = 0.0

                        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                            driverInfoRef.child("$uid")
                                .setValue(model)
                                .addOnFailureListener{e->
                                    Toast.makeText(
                                        this@SplashScreenActivity,
                                        "${resources.getString(R.string.error_name)}: ${e.message}", Toast.LENGTH_SHORT)
                                        .show();
                                    progressBar.visibility = View.GONE
                                }.addOnSuccessListener {
                                    Toast.makeText(
                                        this@SplashScreenActivity,
                                        "Registered Succesfully!", Toast.LENGTH_SHORT)
                                        .show();
                                    dialog.dismiss()
                                    progressBar.visibility = View.GONE
                                    gotoHomeActivity(model)
                                }
                        }
                    }
                }
            }

        }
    }

    private fun emailValidation(email: String?): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setEmailButtonId(R.id.btn_email_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            , LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (requestCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this@SplashScreenActivity,
                    "Error: ${response?.error}", Toast.LENGTH_SHORT)
                    .show();

            }

        }
    }
}
