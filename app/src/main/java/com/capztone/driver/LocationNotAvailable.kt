package com.capztone.driver


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

import com.capztone.driver.databinding.ActivityLocationNotAvailableBinding


class LocationNotAvailable : AppCompatActivity() {
    private lateinit var binding: ActivityLocationNotAvailableBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLocationNotAvailableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.another.setOnClickListener {
            val intent = Intent(this, DriverLocation::class.java)
            startActivity(intent)
        }

    }
}
