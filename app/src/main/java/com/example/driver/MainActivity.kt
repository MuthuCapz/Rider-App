package com.example.driver


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().reference.child("OrderDetails")
        // Initialize RecyclerView

        orderRecyclerView = findViewById(R.id.orderRecyclerView)
        orderAdapter = OrderAdapter(this)

        // Set up RecyclerView
        orderRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = orderAdapter
        }

        // Fetch order details from Firebase
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()
                for (orderSnapshot in snapshot.children) {
                    val order = orderSnapshot.getValue(Order::class.java)
                    if (order != null) {
                        // Handle null values for customerAddress
                        if (order.address == null) {
                            order.address = ""
                        }
                        orders.add(order)
                    }
                }
                Log.d("AdminActivity", "Number of orders retrieved: ${orders.size}")
                orderAdapter.submitList(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}