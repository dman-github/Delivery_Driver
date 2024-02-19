package com.okada.android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit


class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Completable.timer(3, TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe({
                Toast.makeText(this@SplashScreenActivity,
                    "Splash screen run done!", Toast.LENGTH_SHORT)
                    .show();
            })
    }
}
