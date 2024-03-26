package com.example.driver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.example.driver.R
import androidx.cardview.widget.CardView

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(private val context: Context) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var orderList: List<Order> = emptyList()



    fun submitList(newList: List<Order>) {
        orderList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return OrderViewHolder(inflater, parent, context)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]
        holder.bind(order)
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    class OrderViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        private val context: Context
    ) : RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item_order, parent, false)) {

        private var orderIdTextView: TextView = itemView.findViewById(R.id.itemPushKey)
        private var userUidTextView: TextView = itemView.findViewById(R.id.userUid)
        private var customerNameTextView: TextView = itemView.findViewById(R.id.userName)
        private var addressTextView: TextView = itemView.findViewById(R.id.address)
        private var phoneTextView: TextView = itemView.findViewById(R.id.phone)
        private var cancellationMessageTextView: TextView = itemView.findViewById(R.id.cancel) // Added TextView
        private var btnViewDetails: Button = itemView.findViewById(R.id.accept)
        private var btnConfirmed: Button = itemView.findViewById(R.id.conform)
        private var btnDelivered: Button = itemView.findViewById(R.id.delivered)
        private var drop: ImageView = itemView.findViewById(R.id.drop)
        // Inside OrderViewHolder class

        private var deliveryMessageTextView: TextView = itemView.findViewById(R.id.delivery_message)
        private var cardView: CardView = itemView.findViewById(R.id.order)
        private var firebaseDatabase: DatabaseReference = FirebaseDatabase.getInstance().reference




        fun bind(order: Order) {
            orderIdTextView.text = "Order ID: ${order.itemPushKey}"
            userUidTextView.text = "User ID: ${order.userUid}"
            customerNameTextView.text = "Customer Name: ${order.userName}"
            addressTextView.text = "Customer Address: ${order.address}"
            phoneTextView.text = "Customer phone: ${order.phone}"

            // Display cancellation message if available
            if (order.cancellationMessage.isNotBlank()) {
                cancellationMessageTextView.visibility = View.VISIBLE
                cancellationMessageTextView.text = "${order.cancellationMessage}"

                // If there is a cancellation message, disable all three buttons
                btnViewDetails.isEnabled = false
                btnConfirmed.isEnabled = false
                btnDelivered.isEnabled = false
            } else {
                cancellationMessageTextView.visibility = View.GONE

                // If there is no cancellation message, enable all three buttons
                btnViewDetails.isEnabled = true
                btnConfirmed.isEnabled = true
                btnDelivered.isEnabled = true
            }

            val statusReference = firebaseDatabase.child("status").child(order.itemPushKey)
            statusReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val status = dataSnapshot.child("message").value.toString()
                    deliveryMessageTextView.visibility = View.VISIBLE
                    deliveryMessageTextView.text = status

                    if (status == "Order delivered") {
                        cardView.isEnabled = false
                        itemView.alpha = 0.5f // Optional: Reduce opacity to indicate disabled state
                    }

                }


                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Error fetching status from Firebase: ${databaseError.message}")
                }
            })
            drop.setOnClickListener { showPopupMenu(order.itemPushKey) }

            // Set click listener for the "View Details" button
            btnViewDetails.setOnClickListener {
                // Launch Google Maps directions
                launchGoogleMapsDirections(order.address)

                // Save message to Firebase
                saveMessageToFirebase(order.itemPushKey, "Order picked")

                btnViewDetails.isEnabled = false
                btnViewDetails.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))




            }

            // Set click listener for the "Confirmed" button
            btnConfirmed.setOnClickListener {
                // Save message to Firebase
                saveMessageToFirebase(order.itemPushKey, "Order confirmed")
                btnConfirmed.isEnabled = false
                btnConfirmed.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))





            }

            // Set click listener for the "Delivered" button
            btnDelivered.setOnClickListener {
                // Save message to Firebase
                saveMessageToFirebase(order.itemPushKey, "Order delivered")

                btnDelivered.isEnabled = false

                btnDelivered.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))


            }
        }


        private fun showPopupMenu(orderId: String) {
            val popupMenu = PopupMenu(context, drop)
            popupMenu.menuInflater.inflate(R.menu.payoutaddress, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
                // Handle menu item click here
                when (menuItem.itemId) {
                    R.id.menu_15mins -> saveMenuItemToFirebase(orderId, "15 mins")
                    R.id.menu_30mins -> saveMenuItemToFirebase(orderId, "30 mins")
                    R.id.menu_45mins -> saveMenuItemToFirebase(orderId, "45 mins")
                    R.id.menu_1hr -> saveMenuItemToFirebase(orderId, "1 hr")
                }
                true
            }

            popupMenu.show()
        }

        private fun saveMenuItemToFirebase(orderId: String, time: String) {
            val firebaseDatabase = FirebaseDatabase.getInstance().reference
            val messageReference = firebaseDatabase.child("Estimated Time").child(orderId)

            val messageData = HashMap<String, Any>()
            messageData["estimated_time"] = time
            messageData["timestamp"] = getTimeStamp()

            messageReference.setValue(messageData)
                .addOnSuccessListener {
                    Log.d("Firebase", "Estimated time saved to Firebase for order $orderId: $time")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error saving estimated time to Firebase for order $orderId: $e")
                }
        }

        private fun launchGoogleMapsDirections(destination: String) {
            val uri = "google.navigation:q=$destination"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        }


        private fun saveMessageToFirebase(orderid: String, message: String) {
            // Assuming you have initialized Firebase in your application
            val firebaseDatabase = FirebaseDatabase.getInstance().reference
            val messageReference = firebaseDatabase.child("status").child(orderid)

            val messageData = HashMap<String, Any>()
            messageData["message"] = message
            messageData["timestamp"] = getTimeStamp()

            messageReference.setValue(messageData)
                .addOnSuccessListener {
                    Log.d("Firebase", "Message saved to Firebase: $message")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error saving message to Firebase: $e")
                }
        }

        private fun getTimeStamp(): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}