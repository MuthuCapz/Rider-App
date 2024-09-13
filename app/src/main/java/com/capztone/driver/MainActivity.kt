
package com.capztone.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: DriverViewModel by viewModels()
    private lateinit var firebaseDatabase: FirebaseDatabase

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Database
        mAuth = FirebaseAuth.getInstance()

        // Get the current user
        val currentUser = mAuth.currentUser
        databaseReference = FirebaseDatabase.getInstance().reference.child("OrderDetails")
        val logoutImageView: ImageView = findViewById(R.id.logout)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        firebaseDatabase = FirebaseDatabase.getInstance()
        logoutImageView.setOnClickListener {
            logout()
        }

        // Initialize orderAdapter
        orderRecyclerView = findViewById(R.id.orderRecyclerView)

        currentUser?.let { user ->
            val userName = user.displayName
            val email = user.email




            // Pass username to OrderAdapter
            orderAdapter = OrderAdapter(this, userName ?: "")
        }

        // Set up RecyclerView
        orderRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = orderAdapter
        }

        // Check if the location permission is granted, if not request it
        if (checkLocationPermission()) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }

        viewModel.driverLocationSaved.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    "Driver location saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Failed to save driver location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getLastLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        viewModel.saveDriverLocation()
                        fetchDriverDistance { distance ->
                            fetchOrders(it, distance)
                        }
                    } ?: run {
                        Toast.makeText(
                            this,
                            "Failed to get location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun fetchDriverDistance(onDistanceFetched: (Int) -> Unit) {
        val driverDistanceRef = firebaseDatabase.getReference("Admin/spXRl1jY4yTlhDKZJzLicp8E9kc2/DriverDistance")
        driverDistanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val distance = snapshot.getValue(Int::class.java) ?: 5 // Default to 5 km if not set
                onDistanceFetched(distance)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch driver distance", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchOrders(driverLocation: Location, targetDistance: Int) {
        // Fetch order details from Firebase
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()

                for (orderSnapshot in snapshot.children) {
                    val orderId = orderSnapshot.key // Get order ID
                    val order = orderSnapshot.getValue(Order::class.java)

                    if (orderId != null && order != null) {
                        val deliveryAddress = order.address
                        if (deliveryAddress != null) {
                            // Get latitude and longitude of delivery address
                            val geocoder = Geocoder(applicationContext)
                            val addressList = geocoder.getFromLocationName(deliveryAddress, 1)
                            if (addressList != null) {
                                if (addressList.isNotEmpty()) {
                                    val address = addressList?.get(0)
                                    val deliveryLocation = Location("").apply {
                                        if (address != null) {
                                            latitude = address.latitude
                                        }
                                        if (address != null) {
                                            longitude = address.longitude
                                        }
                                    }

                                    // Calculate distance between driver and delivery address
                                    val distance = driverLocation.distanceTo(deliveryLocation) / 1000 // in km

                                    if (distance <= targetDistance) {
                                        orders.add(order)
                                    }
                                }
                            }
                        }
                    }
                }

                // Update RecyclerView with filtered orders
                orderAdapter.submitList(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radius of the Earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c // Distance in km
    }


    private fun logout() {
        mAuth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}