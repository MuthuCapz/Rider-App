
package com.capztone.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.capztone.driver.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var googleSignInClient: GoogleSignInClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Database
        mAuth = FirebaseAuth.getInstance()
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
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

        // Check if the location permission is granted, if not request it
        if (checkLocationPermission()) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }
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

        }, 2000) // Simulate 2-second refresh
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
        val driverDistanceRef = firebaseDatabase.getReference("Admins/spXRl1jY4yTlhDKZJzLicp8E9kc2/Driver Distance")
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
                var allOrdersOutsideDistance = true // Flag to check if all orders are outside distance

                for (orderSnapshot in snapshot.children) {
                    val orderId = orderSnapshot.key // Get order ID
                    val order = orderSnapshot.getValue(Order::class.java)
                    val driverId = orderSnapshot.child("Driver Id").value as? String // Retrieve the DriverId from the database

                    if (orderId != null && order != null) {
                        val currentUserId = mAuth.currentUser?.uid // Get current driver's ID

                        // Retrieve orders based on DriverId logic
                        if (driverId == null || driverId == currentUserId) {
                            val deliveryAddress = order.address
                            if (deliveryAddress != null) {
                                // Get latitude and longitude of delivery address
                                val geocoder = Geocoder(applicationContext)
                                val addressList = geocoder.getFromLocationName(deliveryAddress, 1)
                                if (addressList != null && addressList.isNotEmpty()) {
                                    val address = addressList[0]
                                    val deliveryLocation = Location("").apply {
                                        latitude = address.latitude
                                        longitude = address.longitude
                                    }

                                    // Calculate distance between driver and delivery address
                                    val distance = driverLocation.distanceTo(deliveryLocation) / 1000 // in km

                                    // Check if the distance is within the target distance
                                    if (distance <= targetDistance) {
                                        orders.add(order) // Add order if within distance
                                        hasValidOrders = true // Mark that there are valid orders
                                        allOrdersOutsideDistance = false // At least one order is within distance
                                    }
                                }
                            }
                        }
                    }
                }

                // Determine navigation based on orders retrieved
                when {
                    orders.isEmpty() -> {
                        // No valid orders found
                        startActivity(Intent(this@MainActivity, OrderEmptyActivity::class.java))
                    }
                    allOrdersOutsideDistance -> {
                        // All orders are outside the specified distance
                        startActivity(Intent(this@MainActivity, LocationNotAvailable::class.java))
                    }
                    hasValidOrders -> {
                        // Valid orders within distance found
                        orderAdapter.submitList(orders) // Update RecyclerView with valid orders
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Log.e("FirebaseError", "Error retrieving orders: ${error.message}")
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