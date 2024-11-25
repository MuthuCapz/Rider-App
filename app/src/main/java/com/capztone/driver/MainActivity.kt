package com.capztone.driver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.capztone.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: DriverViewModel by viewModels()
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var googleSignInClient: GoogleSignInClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Database
          mAuth = FirebaseAuthUtil.auth

        if (!isInternetAvailable()) {
            showNoInternetDialog()
        }

        // Get the current user
        val currentUser = mAuth.currentUser
        databaseReference = FirebaseDatabase.getInstance().reference.child("OrderDetails")
        swipeRefreshLayout = findViewById(R.id.swipe)
        // Set up pull-to-refresh logic
        setupSwipeToRefresh()

        val logoutImageView: ImageView = findViewById(R.id.logout)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        firebaseDatabase = FirebaseDatabase.getInstance()
        logoutImageView.setOnClickListener {
            showLogoutConfirmationDialog()
        }
// Initialize GoogleSignInOptions
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id)) // Your web client ID
            .requestEmail()
            .build()

        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // Initialize orderAdapter
        orderRecyclerView = findViewById(R.id.orderRecyclerView)
        progressBar = findViewById(R.id.progressBar)


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


        getLastLocation()

        // Show the ProgressBar when some task starts
        showLoading()

        // Simulate a task (e.g., fetching data)
        performTask()
        viewModel.driverLocationSaved.observe(this) { success ->
            if (success) {

            } else {
                Toast.makeText(
                    this,
                    "Failed to save driver location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        // This method closes the app when the back button is pressed in MainActivity
        finishAffinity()
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // Show progress bar and refresh data
            refreshData()
        }

        // Optional: Set color scheme for the progress spinner
        swipeRefreshLayout.setColorSchemeColors(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
        )
    }
    private fun refreshData() {
        // Simulate data refresh or fetch new data from Firebase
        Handler(Looper.getMainLooper()).postDelayed({
            // Stop showing the loading spinner after the task is done
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, "refreshed!", Toast.LENGTH_SHORT).show()

            // Perform your actual data fetch task here (e.g., fetchOrders again)
            performTask()

        }, 1500) // Simulate 2-second refresh
    }
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    private fun showNoInternetDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_no_internet, null)

        // Create the dialog using MaterialAlertDialogBuilder
        val builder = MaterialAlertDialogBuilder(this)
        builder.setView(dialogView) // Set the custom layout
            .setCancelable(false) // Prevent closing the dialog

        val dialog = builder.create()

        // Initialize the buttons in the custom dialog
        val retryButton: Button = dialogView.findViewById(R.id.dialog_button_yes)
        val exitButton: Button = dialogView.findViewById(R.id.dialog_button_no)

        retryButton.setOnClickListener {
            if (isInternetAvailable()) {
                dialog.dismiss() // Dismiss the dialog if the internet is available
                // Proceed with network operations if needed
            } else {
                isInternetAvailable() // Check internet and show dialog again if still no internet
            }
        }

        exitButton.setOnClickListener {
            finish() // Close the app if the user chooses to exit
        }

        // Show the dialog if there's no internet
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isInternetAvailable()
        }
    }
    override fun onStart() {
        super.onStart()
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }


    private fun performTask() {
        // Simulate a delay (e.g., network request)
        // You can replace this with your actual task logic
        progressBar.postDelayed({
            // Hide the ProgressBar once the task is complete
            hideLoading()
        }, 2500) // Simulating a 3-second task
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun getLastLocation() {
        val currentUserId = mAuth.currentUser?.uid
        if (currentUserId != null) {
            // Reference to the driver's location based on their ID in Firebase
            val driverLocationRef = firebaseDatabase.getReference("Driver Location/$currentUserId")

            driverLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Get latitude and longitude from the snapshot
                    val latitude = snapshot.child("latitude").getValue(Double::class.java)
                    val longitude = snapshot.child("longitude").getValue(Double::class.java)

                    // Check if the values are not null
                    if (latitude != null && longitude != null) {
                        // Create a Location object with the retrieved coordinates
                        val driverLocation = Location("").apply {
                            this.latitude = latitude
                            this.longitude = longitude
                        }

                        // Fetch the locality from latitude and longitude
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        try {
                            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
                            if (addressList != null && addressList.isNotEmpty()) {
                                val address = addressList[0]
                                val locality = address.locality ?: "Unknown Location"
                                // Inside the onDataChange method, after updating the locationTextView
                                findViewById<TextView>(R.id.locationTextView).apply {
                                    text = locality
                                    setOnClickListener {
                                        // Navigate to DriverLocation Activity
                                        val intent = Intent(this@MainActivity, DriverLocation::class.java)
                                        startActivity(intent)
                                    }
                                }

                                // Display locality in the TextView
                                findViewById<TextView>(R.id.locationTextView).text = locality
                            } else {
                                Toast.makeText(this@MainActivity, "No address found for this location", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(this@MainActivity, "Geocoder service not available", Toast.LENGTH_SHORT).show()
                        }

                        // Fetch target distance and orders
                        fetchDriverDistance { targetDistance ->
                            fetchOrders(driverLocation, targetDistance)
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to retrieve driver location from Firebase", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error fetching driver location: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
        }
    }



    private fun fetchDriverDistance(onDistanceFetched: (Int) -> Unit) {
        val driverDistanceRef = firebaseDatabase.getReference("Delivery Details/Driver Distance")
        driverDistanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val distanceString = snapshot.getValue(String::class.java) // Get the value as a String
                val distance = distanceString?.toIntOrNull() ?: 5 // Convert to Int, default to 5 if conversion fails
                onDistanceFetched(distance)
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch driver distance", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchOrders(driverLocation: Location, targetDistance: Int) {
        // Fetch order details from Firebase and listen for real-time updates
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()
                var hasValidOrders = false // Flag to check if there are any valid orders

                for (orderSnapshot in snapshot.children) {
                    val orderId = orderSnapshot.key // Get order ID
                    val order = orderSnapshot.getValue(Order::class.java)
                    val driverId = orderSnapshot.child("Driver Id").value as? String // Retrieve the DriverId from the database
                    val confirmationStatus = orderSnapshot.child("Confirmation").value as? String // Retrieve the Confirmation status

                    // Check if the order is confirmed and the driver is matched
                    if (orderId != null && order != null && confirmationStatus == "Order Confirmed" && isDriverMatched(driverId)) {
                        val distance = calculateDistanceToDeliveryLocation(driverLocation, order.address)
                        if (distance <= targetDistance) {
                            orders.add(order) // Add order if within distance
                            hasValidOrders = true // Mark that there are valid orders
                        }
                    }
                }

                // Sort orders: "Order Delivered" last, then by orderDate (reverse), then by selectedSlot
                orders.sortWith(compareBy<Order> {
                    // Prioritize order status
                    when (it.status) {
                        "Order Delivered" -> 1 // Place "Order Delivered" last
                        "Order Picked" -> 0     // Place "Order Picked" first
                        "Order Confirmed" -> 0   // Place "Order Confirmed" next
                        else -> 2 // Any other status will be in between
                    }
                }.thenByDescending { it.orderDate } // Reverse sort for orderDate
                    .thenBy { extractStartTime(it.selectedSlot) }) // Sort by selectedSlot

                handleOrderResults(orders, hasValidOrders)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error retrieving orders: ${error.message}")
            }
        })
    }

    private fun extractStartTime(slot: String): String {
        // Assuming the format is "HH:mm a - HH:mm a"
        return slot.split(" - ").first().trim() // Extracting the start time
    }

    private fun isDriverMatched(driverId: String?): Boolean {
        val currentUserId = mAuth.currentUser?.uid
        return driverId == null || driverId == currentUserId
    }
    private fun calculateDistanceToDeliveryLocation(driverLocation: Location, deliveryAddress: String?): Float {
        if (deliveryAddress.isNullOrEmpty()) return Float.MAX_VALUE

        return try {
            val geocoder = Geocoder(applicationContext)
            val addressList = geocoder.getFromLocationName(deliveryAddress, 1)
            if (addressList?.isNotEmpty() == true) {
                val address = addressList?.get(0)
                val deliveryLocation = Location("").apply {
                    if (address != null) {
                        latitude = address.latitude
                    }
                    if (address != null) {
                        longitude = address.longitude
                    }
                }
                driverLocation.distanceTo(deliveryLocation) / 1000 // Distance in kilometers
            } else {
                Float.MAX_VALUE
            }
        } catch (e: IOException) {
            Log.e("GeocoderError", "Geocoding failed: ${e.message}")
            Float.MAX_VALUE
        }
    }


    private fun handleOrderResults(orders: List<Order>, hasValidOrders: Boolean) {
        when {
            orders.isEmpty() -> {
                startActivity(Intent(this@MainActivity, OrderEmptyActivity::class.java))
            }
            hasValidOrders -> {
                orderAdapter.submitList(orders) // Update RecyclerView with valid orders
            }
            else -> {
                startActivity(Intent(this@MainActivity, LocationNotAvailable::class.java))
            }
        }
    }


    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_logout_confirmation, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()

        dialogView.findViewById<View>(R.id.btnDialogYes).setOnClickListener {
            // Perform logout action
            logout()
            alertDialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnDialogNo).setOnClickListener {
            // Dismiss the dialog
            alertDialog.dismiss()
        }

        alertDialog.show()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    private fun logout() {
        // Sign out from Firebase
        mAuth.signOut()

        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // After signing out from Google, redirect to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}