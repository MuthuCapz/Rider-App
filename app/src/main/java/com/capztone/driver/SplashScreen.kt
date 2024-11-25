package com.capztone.driver

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.capztone.driver.databinding.ActivitySplashScreenBinding
import com.capztone.utils.FirebaseAuthUtil
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
        mAuth = FirebaseAuthUtil.auth
        binding.imageView4.setAnimation("login.json")
        binding.imageView4.playAnimation()

        val translateAnimation = TranslateAnimation(0f, 500f, 500f, 0f)
        translateAnimation.duration = 1000

        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_animation)

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translateAnimation)
        animationSet.addAnimation(rotateAnimation)

        binding.iconTxtt1.startAnimation(zoomInAnimation)

        Handler().postDelayed({
            checkUserInRidersDetails()
        }, 3300)
    }

    private fun checkUserInRidersDetails() {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val ridersDetailsRef = database.getReference("Riders Details").child(userId)

            ridersDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User exists in "Riders Details", now check "Driver Location"
                        checkUserInDriverLocation(userId)
                    } else {
                        // User does not exist in "Riders Details", navigate to LoginActivity
                        startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
                    finish()
                }
            })
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun checkUserInDriverLocation(userId: String) {
        val driverLocationRef = database.getReference("Driver Location").child(userId)

        driverLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User exists in "Driver Location", navigate to MainActivity
                    startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                } else {
                    // User does not exist in "Driver Location", navigate to DriverLocationActivity
                    startActivity(Intent(this@SplashScreen, DriverLocation::class.java))
                }
                finish()
            }

            override fun onCancelled(error: DatabaseError) {
                startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
                finish()
            }
        })
    }
}
