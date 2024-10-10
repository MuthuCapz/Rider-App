package com.capztone.driver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.capztone.driver.databinding.ActivitySplashScreenBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()
        binding.imageView4.setAnimation("login.json")
        binding.imageView4.playAnimation()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        // Translate animation to move image from top to center
        val translateAnimation = TranslateAnimation(0f, 500f, 500f, 0f)
        translateAnimation.duration = 1000 // Set duration as needed

        // Rotate and zoom animations
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_animation)

        // Animate simultaneously
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translateAnimation)
        animationSet.addAnimation(rotateAnimation)

        binding.iconTxtt1.startAnimation(zoomInAnimation)



        Handler().postDelayed({
            checkUserInRidersDetails()
        }, 3300) // Adjust delay as needed
    }
    private fun checkUserInRidersDetails() {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val ridersDetailsRef = database.getReference("Riders Details").child(userId)

            // Check if the userId exists in "Riders Details"
            ridersDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User exists in "Riders Details", navigate to MainActivity
                        startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                    } else {
                        // User does not exist, navigate to LoginActivity
                        startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
                    }
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error, possibly navigate to login or show error
                    startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
                    finish()
                }
            })
        } else {
            // If no user is logged in, navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
