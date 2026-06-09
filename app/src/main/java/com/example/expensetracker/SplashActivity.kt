package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animate the dots
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)

        // Fade animation for dots
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 500
        val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        fadeOut.duration = 500

        // Sequence of dot animations
        dot1.startAnimation(fadeIn)
        Handler(Looper.getMainLooper()).postDelayed({
            dot2.startAnimation(fadeIn)
        }, 300)
        Handler(Looper.getMainLooper()).postDelayed({
            dot3.startAnimation(fadeIn)
        }, 600)

        // Wait 2.5 seconds, then go to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Add slide animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }, 5000)
    }
}