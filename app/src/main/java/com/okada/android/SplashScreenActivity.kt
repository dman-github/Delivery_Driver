package com.okada.android


import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity



class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
        Log.i("App_Info", "SplashScreenActivity onCreate")
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
