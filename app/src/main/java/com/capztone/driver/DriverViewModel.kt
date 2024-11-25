package com.capztone.driver

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.capztone.utils.FirebaseAuthUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var mAuth: FirebaseAuth
    private val _driverLocationSaved = MutableLiveData<Boolean>()
    val driverLocationSaved: LiveData<Boolean>
        get() = _driverLocationSaved

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        firebaseDatabase = FirebaseDatabase.getInstance()
       mAuth = FirebaseAuthUtil.auth
    }

    fun saveDriverLocation() {
        val user = mAuth.currentUser
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val userId = user?.uid ?: ""
                        val locationData = getLocationData(location)
                        val formattedDriverId = "$userId"
                        firebaseDatabase.getReference("Driver Location").child(formattedDriverId)
                            .setValue(locationData)
                            .addOnCompleteListener {
                                _driverLocationSaved.postValue(it.isSuccessful)
                            }
                    }
                }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val context = getApplication<Application>().applicationContext
        return ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLocationData(location: Location): Map<String, Any> {
        val locationMap = HashMap<String, Any>()
        locationMap["latitude"] = location.latitude
        locationMap["longitude"] = location.longitude
        // You can add more information like timestamp, accuracy, etc. depending on your requirements
        return locationMap
    }

    private fun generateDriverId(): String {
        return UUID.randomUUID().toString()
    }
}
