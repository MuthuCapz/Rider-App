package com.capztone.driver

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capztone.driver.databinding.ActivityDriverLocationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import com.capztone.driver.R
import com.capztone.utils.FirebaseAuthUtil
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class DriverLocation : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private lateinit var binding: ActivityDriverLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: DatabaseReference
    private var pinnedAddress: String? = null
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Firebase Auth
       mAuth = FirebaseAuthUtil.auth

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check location services
        if (!isLocationEnabled()) {
            showLocationServicesDialog()
        } else {
            checkLocationPermissions()
        }

        // Set up the SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchLocation(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Set up the arrow icon click event
        binding.arrowIcon.setOnClickListener {
            val query = binding.searchView.query.toString()
            if (query.isNotEmpty()) {
                searchLocation(query)
            }
        }
        binding.buttonConfirmLocation.setOnClickListener {
            val userId = mAuth.currentUser?.uid
            if (userId != null && marker != null) {
                val latLng = marker!!.position
                val currentTime =
                    SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
                val currentUsername = mAuth.currentUser?.displayName ?: "unknown"

                val locationRef = database.child("Driver Location").child(userId)

                // Check if the location already exists in the database
                locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val locationData = mutableMapOf<String, Any>(
                            "latitude" to latLng.latitude,
                            "longitude" to latLng.longitude
                        )

                        if (dataSnapshot.exists()) {
                            // Update `updatedDate` and `updatedBy` fields
                            locationData["updatedDate"] = currentTime
                            locationData["updatedBy"] = currentUsername
                        } else {
                            // Set `createdDate` and `createdBy` for first-time location save
                            locationData["createdDate"] = currentTime
                            locationData["createdBy"] = currentUsername
                            locationData["updatedDate"] = currentTime
                            locationData["updatedBy"] = currentUsername
                        }

                        // Save or update the location in Firebase
                        locationRef.updateChildren(locationData).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showToast("Location saved successfully!")
                                startActivity(Intent(this@DriverLocation, MainActivity::class.java))
                                finish()
                            } else {
                                showToast("Failed to save location: ${task.exception?.message}")
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        showToast("Failed to access location data: ${databaseError.message}")
                    }
                })
            } else {
                showToast("No location to confirm!")
            }
        }
    }

        override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check location permissions and immediately move to the user's location
        checkLocationPermissions()

        // Allow the user to tap anywhere on the map to set a marker
        mMap.setOnMapClickListener { latLng ->
            marker?.remove()
            val markerIcon = BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map)
            )
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).draggable(true).icon(markerIcon)
            )

            // Get the address for the tapped location
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            pinnedAddress = if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "Address not found"
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    val markerIcon = BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(resources, R.drawable.map)
                    )

                    // Clear any previous markers and set a new one
                    marker?.remove()
                    marker = mMap.addMarker(
                        MarkerOptions().position(currentLocation).draggable(true).icon(markerIcon)
                    )

                    // Move and zoom the camera to the user's current location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                } else {
                    showToast("Unable to retrieve location.")
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(location, 1)
        if (addresses!!.isNotEmpty()) {
            val address = addresses[0]
            val latLng = LatLng(address.latitude, address.longitude)
            mMap.clear()
            val markerIcon = BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map)
            )
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).draggable(true).icon(markerIcon)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
            pinnedAddress = address.getAddressLine(0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationEnabled()) {
                    getCurrentLocation() // Fetch and move to the current location
                } else {
                    showLocationServicesDialog() // Prompt to enable location services
                }
            } else {
                showToast("Location permission denied")
            }
        }
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showLocationServicesDialog() {
        // Inflate the custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.custom_location_dialog, null)

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Optional: Prevent dialog from closing on outside touch
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Find views from the custom layout
        val btnDialogYes: AppCompatButton = dialogView.findViewById(R.id.btnDialogYes)
        val btnDialogNo: AppCompatButton = dialogView.findViewById(R.id.btnDialogNo)

        // Handle "Turn On" button click
        btnDialogYes.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE)
            dialog.dismiss() // Close the dialog
        }

        // Handle "Exit" button click
        btnDialogNo.setOnClickListener {
            dialog.dismiss()
            finish()// Close the dialog
            // Optionally, handle cancellation if needed
        }

        // Show the dialog
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (isLocationEnabled()) {
                // If location services are turned on, fetch the current location
                getCurrentLocation()
            } else {
                showLocationServicesDialog()
            }
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_SETTINGS_REQUEST_CODE = 2
    }
}
