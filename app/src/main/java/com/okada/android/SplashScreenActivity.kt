package com.okada.android


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
        init()
        Log.i("App_Info", "SplashScreenActivity onCreate")
    }

    fun init() {
        // Check permissions needed in new Android version for Push notifications
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            };
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i("App_Info", "SplashScreenActivity onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.i("App_Info", "SplashScreenActivity onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("App_Info", "SplashScreenActivity onDestroy")
    }
}
