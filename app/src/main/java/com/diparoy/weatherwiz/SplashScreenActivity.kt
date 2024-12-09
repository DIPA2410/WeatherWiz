package com.diparoy.weatherwiz

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()

        window.navigationBarColor = resources.getColor(R.color.sky_blue)

        var handler = Handler()
        handler.postDelayed(
            Runnable { startActivity(Intent(this,MainActivity::class.java))
                finish()},5000
        )
    }
}