package com.capztone.driver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.driver.databinding.ActivityDriverDetailsBinding
import com.capztone.utils.FirebaseAuthUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class DriverDetails : AppCompatActivity() {
    private lateinit var binding: ActivityDriverDetailsBinding
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference
       mAuth = FirebaseAuthUtil.auth

        binding.selectImage.setOnClickListener {
            openGallery()
        }
        addFocusListeners()
        binding.submit.setOnClickListener {
            submitDetails()
        }
    }



    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            binding.selectedImageView.setImageURI(selectedImageUri)
        }
    }

    private fun submitDetails() {
        val userName = binding.userName.text.toString().trim()
        val email = binding.eMail.text.toString().trim()
        val phone = binding.phone.text.toString().trim()

        setDetailsCompleted(true)
        if (userName.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && selectedImageUri != null) {
            saveToFirebase(userName, email, phone)
        } else {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
        }
    }
    private fun addFocusListeners() {
        // Name validation on focus change
        binding.userName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateName(binding.userName.text.toString().trim())
            }
        }

        // Phone validation on focus change
        binding.phone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePhone(binding.phone.text.toString().trim())
            }
        }

        // Email validation on focus change
        binding.eMail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateEmail(binding.eMail.text.toString().trim())
            }
        }

    }
    private fun validateName(name: String) {
        if (name.isEmpty() || name.length < 3 || name.length > 20) {
            binding.userName.error = "Name must be between 3 and 20 characters"
        } else {
            binding.userName.error = null
        }
    }

    private fun validatePhone(phone: String) {
        if (phone.isEmpty() || phone.length != 10 || !phone.all { it.isDigit() }) {
            binding.phone.error = "Mobile number must be exactly 10 digits"
        } else {
            binding.phone.error = null
        }
    }

    private fun validateEmail(email: String) {
        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@gmail\\.com$".toRegex())) {
            binding.eMail.error = "Please enter a valid Gmail address"
        } else {
            binding.eMail.error = null
        }
    }


    private fun setDetailsCompleted(completed: Boolean) {
        // Store details completion flag in SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("details_completed", completed)
        editor.apply()
    }


    private fun saveToFirebase(userName: String, email: String, phone: String) {
        val user = mAuth.currentUser
        val databaseReference = FirebaseDatabase.getInstance().getReference("Riders Details")
        val imageName = System.currentTimeMillis().toString()
        val imageReference = storageReference.child("images/$imageName")

        imageReference.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageReference.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()

                    val detailsMap = HashMap<String, Any>()
                    detailsMap["userName"] = userName
                    detailsMap["email"] = email
                    detailsMap["phone"] = phone
                    detailsMap["imageUrl"] = imageUrl
                    val userId = user?.uid ?: ""

                    if (user != null) {
                        // Using updateChildren to only update provided fields, leaving other fields intact
                        databaseReference.child(userId).updateChildren(detailsMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Details saved successfully", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@DriverDetails, DriverLocation::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to update details: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}
