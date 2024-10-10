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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class DriverDetails : AppCompatActivity() {
    private lateinit var binding: ActivityDriverDetailsBinding
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference
        auth = FirebaseAuth.getInstance()

        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
        binding.selectImage.setOnClickListener {
            openGallery()
        }

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
        val address = binding.address.text.toString().trim()
        setDetailsCompleted(true)
        if (userName.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty() && selectedImageUri != null) {
            saveToFirebase(userName, email, phone, address)
        } else {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setDetailsCompleted(completed: Boolean) {
        // Store details completion flag in SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("details_completed", completed)
        editor.apply()
    }


    private fun saveToFirebase(userName: String, email: String, phone: String, address: String) {
        val user = auth.currentUser
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
                    detailsMap["address"] = address
                    detailsMap["imageUrl"] = imageUrl
                    val userId = user?.uid ?: ""

                    if (user != null) {
                        databaseReference.child(userId).setValue(detailsMap)
                    }

                    Toast.makeText(this, "Details saved successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@DriverDetails, MainActivity::class.java))
                    finish()
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
