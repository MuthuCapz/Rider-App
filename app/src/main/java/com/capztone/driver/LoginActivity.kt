package com.capztone.driver

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.capztone.driver.databinding.ActivityLoginBinding
import com.capztone.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuthUtil.auth
        configureGoogleSignIn()

        // Sign out from Google to ensure the account selection dialog appears
        mGoogleSignInClient.signOut()


        binding.googleLoginbutton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    if (isNewUser) {
                        startActivity(Intent(this@LoginActivity, DriverDetails::class.java))
                        finish()
                    } else {
                        val user = mAuth.currentUser
                        user?.let { currentUser ->
                            val userId = currentUser.uid
                            val userRef = FirebaseDatabase.getInstance().getReference("Riders Details").child(userId)

                            // Set the login details
                            val currentTime = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(
                                Date()
                            )
                            val currentUsername = currentUser.displayName ?: "unknown"

                            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // Rider details exist, update only updatedDate and updatedBy
                                        userRef.child("loginUpdatedDate").setValue(currentTime)
                                        userRef.child("loginUpdatedBy").setValue(currentUsername)
                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    } else {
                                        // First login, set createdDate and createdBy
                                        userRef.child("loginCreatedDate").setValue(currentTime)
                                        userRef.child("loginCreatedBy").setValue(currentUsername)
                                        userRef.child("loginUpdatedDate").setValue(currentTime)
                                        userRef.child("loginUpdatedBy").setValue(currentUsername)
                                        startActivity(Intent(this@LoginActivity, DriverDetails::class.java))
                                    }
                                    finish()
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    // Handle potential errors here
                                }
                            })
                        }
                    }
                }
            }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
            }
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
