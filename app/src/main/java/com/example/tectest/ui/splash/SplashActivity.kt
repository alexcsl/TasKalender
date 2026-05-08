package com.example.tectest.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.tectest.R
import com.example.tectest.ui.home.HomeActivity
import com.example.tectest.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashScreenDuration: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.setFlags(
             WindowManager.LayoutParams.FLAG_FULLSCREEN,
             WindowManager.LayoutParams.FLAG_FULLSCREEN
         )

        // Using a Handler to delay navigation
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSessionAndNavigate()
        }, splashScreenDuration)
    }

    private fun checkUserSessionAndNavigate() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            // User is logged in, navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        } else {
            // User is not logged in, navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        // Finish SplashActivity so user can't navigate back to it
        finish()
    }
}