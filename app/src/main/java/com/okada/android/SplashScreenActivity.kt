package com.okada.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
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
        initialise()
    }

    private fun initialise() {
        providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener {instance ->
            // This lis
            val user = instance.currentUser
            var userId = instance.uid
            if (user != null) {
                Toast.makeText(this@SplashScreenActivity,
                    "Welcome: New User, id: $userId", Toast.LENGTH_SHORT)
                    .show();
            } else {
                print("About to show login")
                showLoginLayout()
            }
        }

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
