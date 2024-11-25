package com.capztone.driver


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity


import com.capztone.driver.databinding.ActivityOrderEmptyBinding


class OrderEmptyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderEmptyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
        binding = ActivityOrderEmptyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.another.setOnClickListener {
            val intent = Intent(this, DriverLocation::class.java)
            startActivity(intent)
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        // This method closes the app when the back button is pressed in MainActivity
        finishAffinity()
    }
}
