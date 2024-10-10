package com.capztone.driver

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
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(private val context: Context, private val username: String?) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var orderList: List<Order> = emptyList()


    fun submitList(newList: List<Order>) {
        orderList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return OrderViewHolder(inflater, parent, context, username)
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
        private val context: Context,
        private val username: String?

    ) : RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item_order, parent, false)) {

        private var orderIdTextView: TextView = itemView.findViewById(R.id.itemPushKey)
        private var userUidTextView: TextView = itemView.findViewById(R.id.userUid)

        private var addressTextView: TextView = itemView.findViewById(R.id.address)
        private var selectedSlotTextView: TextView = itemView.findViewById(R.id.slot)
        private var shopnameTextView: TextView = itemView.findViewById(R.id.shopname)
        private var foodnameTextView: TextView = itemView.findViewById(R.id.foodname)
        private var foodquantityTextView: TextView = itemView.findViewById(R.id.foodquantity)
        private var orderdateTextView: TextView = itemView.findViewById(R.id.date)
        private var cancellationMessageTextView: TextView = itemView.findViewById(R.id.cancel)
        private var btnViewDetails: Button = itemView.findViewById(R.id.accept)
        private var btnConfirmed: Button = itemView.findViewById(R.id.conform)
        private var btnDelivered: Button = itemView.findViewById(R.id.delivered)
        private var drop: ImageView = itemView.findViewById(R.id.drop)
        private var deliveryMessageTextView: TextView = itemView.findViewById(R.id.delivery_message)
        private var cardView: CardView = itemView.findViewById(R.id.order)
        private var firebaseDatabase: DatabaseReference = FirebaseDatabase.getInstance().reference
        private var estimatedTimeSet = false
        fun bind(order: Order) {
            orderIdTextView.text = "${order.itemPushKey}"
            userUidTextView.text = "${order.userUid}"

            addressTextView.text = "${order.address}"
            selectedSlotTextView.text = "${order.selectedSlot}"

            // Remove brackets by using joinToString() to convert lists to comma-separated strings
            shopnameTextView.text = order.shopNames.joinToString(", ")
            foodnameTextView.text = order.foodNames.joinToString(", ")
            foodquantityTextView.text = order.foodQuantities.joinToString(", ")

            orderdateTextView.text = "${order.orderDate}"

            val statusReference = firebaseDatabase.child("status").child(order.itemPushKey)
            statusReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val status = dataSnapshot.child("message").value?.toString() ?: ""
                    deliveryMessageTextView.visibility = View.VISIBLE
                    deliveryMessageTextView.text = status

                    when (status) {
                        "Order delivered" -> {
                            btnConfirmed.isEnabled = false
                            btnViewDetails.isEnabled = false
                            btnDelivered.isEnabled = false
                            btnViewDetails.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.navy
                                )
                            )
                            btnConfirmed.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.navy
                                )
                            )
                            btnDelivered.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.navy
                                )
                            )
                            itemView.alpha = 0.5f
                        }

                        "Order picked" -> {
                            btnViewDetails.isEnabled = false
                            btnConfirmed.isEnabled = false
                            btnViewDetails.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.lnavy
                                )
                            )
                            btnConfirmed.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.lnavy
                                )
                            )
                        }

                        "Order confirmed" -> {
                            btnConfirmed.isEnabled = false
                            btnConfirmed.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.lnavy
                                )
                            )

                        }

                        else -> {
                            btnViewDetails.isEnabled = true
                            btnConfirmed.isEnabled = true
                            btnDelivered.isEnabled = true
                            btnViewDetails.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.navy
                                )
                            )
                            btnConfirmed.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.navy
                                )
                            )
                            btnDelivered.setBackgroundColor(
                                ContextCompat.getColor(
                                    context, R.color.navy
                                )
                            )
                            itemView.alpha = 1.0f
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(
                        "Firebase", "Error fetching status from Firebase: ${databaseError.message}"
                    )
                }
            })



            drop.setOnClickListener { showPopupMenu(order.itemPushKey) }

            val slotTimeRange = order.selectedSlot
            val currentTime = getCurrentTime()

            val isWithinSlotTime = isCurrentTimeInSlot(slotTimeRange, currentTime)

            // Handle button states based on time comparison
            if (!isWithinSlotTime) {
                btnViewDetails.setOnClickListener {
                    Toast.makeText(
                        context, "Please click slot during time only", Toast.LENGTH_SHORT
                    ).show()
                }
                btnConfirmed.setOnClickListener {
                    Toast.makeText(
                        context, "Please click slot during time only", Toast.LENGTH_SHORT
                    ).show()
                }
                btnDelivered.setOnClickListener {
                    Toast.makeText(
                        context, "Please click slot during time only", Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Existing functionality when the current time is within the slot
                btnViewDetails.setOnClickListener {
                    val fullAddress = order.address // This contains name, address, and phone number
                    val validAddress = extractValidAddress(fullAddress)
                    val currentStatus = deliveryMessageTextView.text.toString()
                    if (currentStatus == "Order confirmed") {
                        // Proceed with confirmation logic
                        saveMessageToFirebase(order.itemPushKey, "Order picked", order.shopNames)
                        launchGoogleMapsDirections(validAddress)
                        saveMessageToFirebase(order.itemPushKey, "Order picked", order.shopNames)

                        btnConfirmed.isEnabled = false
                        btnViewDetails.isEnabled = false
                        btnViewDetails.setBackgroundColor(
                            ContextCompat.getColor(
                                context, R.color.lnavy
                            )
                        )
                    } else {
                        // Show toast if the status is not "Order confirmed"
                        Toast.makeText(context, "You can only accept an order after it's confirmed.", Toast.LENGTH_SHORT).show()
                    }

                }
                btnConfirmed.setOnClickListener {


                }


                btnConfirmed.setOnClickListener {
                    if (!estimatedTimeSet) {
                        Toast.makeText(
                            context, "Please set estimated time first", Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    val orderId = order.itemPushKey // Get the current item's order ID
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // G

                    if (currentUserId == null) {
                        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Reference to "Order Details" in Firebase
                    val orderDetailsRef = firebaseDatabase.child("OrderDetails")

                    // Check if the order ID exists in Firebase
                    orderDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            var orderMatched = false

                            // Loop through each child (order ID) under "Order Details"
                            for (orderSnapshot in dataSnapshot.children) {
                                val firebaseOrderId = orderSnapshot.key // Get the order ID from Firebase

                                if (firebaseOrderId == orderId) {
                                    // Order ID matches, store the user ID under this order ID in Firebase
                                    orderDetailsRef.child(orderId).child("Driver Id").setValue(currentUserId)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Order confirmed and user ID saved", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to save user ID", Toast.LENGTH_SHORT).show()
                                        }
                                    orderMatched = true
                                    break // Exit the loop once a match is found
                                }
                            }

                            if (!orderMatched) {
                                Toast.makeText(context, "Order ID not found in Firebase", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("Firebase", "Error fetching order details: ${databaseError.message}")
                        }
                    })
                    saveMessageToFirebase(order.itemPushKey, "Order confirmed", order.shopNames)
                    btnConfirmed.isEnabled = false
                    btnConfirmed.setBackgroundColor(ContextCompat.getColor(context, R.color.lnavy))
                }


                btnDelivered.setOnClickListener {

                    val currentStatus = deliveryMessageTextView.text.toString()
                    if (currentStatus == "Order picked") {
                        // Proceed with delivery logic
                        saveMessageToFirebase(order.itemPushKey, "Order delivered", order.shopNames)
                        btnConfirmed.isEnabled = false
                        btnViewDetails.isEnabled = false
                        btnDelivered.isEnabled = false
                        btnDelivered.setBackgroundColor(ContextCompat.getColor(context, R.color.lnavy))
                    } else {
                        // Show toast if the status is not "Order accepted"
                        Toast.makeText(context, "You can only deliver an order after it's picked.", Toast.LENGTH_SHORT).show()
                    }

                }
            }

            if (order.cancellationMessage.isNotBlank()) {
                cancellationMessageTextView.visibility = View.VISIBLE
                cancellationMessageTextView.text = order.cancellationMessage

                cardView.isEnabled = false
                cardView.alpha = 0.5f

                disableButtonsCompletely() // Disable buttons completely when order is cancelled
            } else {
                cancellationMessageTextView.visibility = View.GONE
                cardView.isEnabled = true
                cardView.alpha = 1.0f

            }

        }

        fun extractValidAddress(fullAddress: String): String {
            // Split the address by spaces
            val parts = fullAddress.split(" ")

            // Ensure the address has at least a valid structure (name, possible street, city/state, and phone number)
            if (parts.size >= 4) {
                // Try to identify the phone number by detecting a '+' or 10-digit pattern (e.g., +91 6380152803 or 9876543210)
                val phoneIndex = parts.indexOfFirst { it.contains("+") || it.length == 10 }

                // If a phone number is found, extract the valid address components before the phone number
                if (phoneIndex != -1 && phoneIndex >= 2) {
                    // Join all parts before the phone number, skipping the first name part (index 0 and 1)
                    val addressParts = parts.subList(1, phoneIndex).joinToString(" ")

                    return addressParts.trim()  // Return the combined address before the phone number
                }
            }

            // Fallback to returning the full address if format is unexpected
            return fullAddress
        }

        // Function to get the current time in 24-hour format (e.g., "09:50")
        private fun getCurrentTime(): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date())
        }

        // Function to check if the current time is within the given slot time range
        private fun isCurrentTimeInSlot(slotTimeRange: String, currentTime: String): Boolean {
            // Extract start and end times from the slotTimeRange (e.g., "10:00 am - 12:00 pm")
            val timeParts = slotTimeRange.split("-")
            if (timeParts.size != 2) return false // Invalid slot format

            val startTime = timeParts[0].trim() // e.g., "10:00 am"
            val endTime = timeParts[1].trim() // e.g., "12:00 pm"

            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

            return try {
                val currentTimeDate = sdf.parse(currentTime)
                val startTimeDate = sdf.parse(startTime)
                val endTimeDate = sdf.parse(endTime)

                currentTimeDate != null && startTimeDate != null && endTimeDate != null && currentTimeDate.after(
                    startTimeDate
                ) && currentTimeDate.before(endTimeDate)
            } catch (e: Exception) {
                false
            }
        }


        private fun showPopupMenu(orderId: String) {
            val popupMenu = PopupMenu(context, drop)
            popupMenu.menuInflater.inflate(R.menu.payoutaddress, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
                when (menuItem.itemId) {
                    R.id.menu_15mins -> {
                        saveMenuItemToFirebase(orderId, "15 mins")
                        estimatedTimeSet = true // Mark estimated time as set
                    }

                    R.id.menu_30mins -> {
                        saveMenuItemToFirebase(orderId, "30 mins")
                        estimatedTimeSet = true // Mark estimated time as set
                    }

                    R.id.menu_45mins -> {
                        saveMenuItemToFirebase(orderId, "45 mins")
                        estimatedTimeSet = true // Mark estimated time as set
                    }

                    R.id.menu_1hr -> {
                        saveMenuItemToFirebase(orderId, "1 hr")
                        estimatedTimeSet = true // Mark estimated time as set
                    }
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

            messageReference.setValue(messageData).addOnSuccessListener {
                Log.d("Firebase", "Estimated time saved to Firebase for order $orderId: $time")
            }.addOnFailureListener { e ->
                Log.e(
                    "Firebase", "Error saving estimated time to Firebase for order $orderId: $e"
                )
            }
        }

        private fun launchGoogleMapsDirections(destination: String) {
            val uri = "google.navigation:q=$destination"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        }

        private fun saveMessageToFirebase(
            orderid: String, message: String, shopName: List<String>
        ) {
            val firebaseDatabase = FirebaseDatabase.getInstance().reference
            val messageReference = firebaseDatabase.child("status").child(orderid)

            val messageData = HashMap<String, Any>()
            messageData["message"] = message
            messageData["shop_name"] = shopName // Store the shop name
            username?.let { messageData["username"] = it }

            if (message == "Order delivered") {
                messageData["timestamp"] = getTimeStamp()

                // Iterate through shopName list and save delivered message to each shop's delivery path
                shopName.forEach { shop ->
                    val shopDeliveryReference =
                        firebaseDatabase.child("${shop} delivery").child(orderid)
                    shopDeliveryReference.setValue(messageData).addOnSuccessListener {
                        Log.d(
                            "Firebase", "Delivered message saved to ${shop} delivery: $message"
                        )
                    }.addOnFailureListener { e ->
                        Log.e(
                            "Firebase", "Error saving delivered message to ${shop} delivery: $e"
                        )
                    }
                }
            }

            messageReference.setValue(messageData).addOnSuccessListener {
                Log.d("Firebase", "Message saved to Firebase: $message")
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error saving message to Firebase: $e")
            }
        }

        private fun getTimeStamp(): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date())
        }

        private fun disableButtonsCompletely() {

            btnViewDetails.isEnabled = false
            btnViewDetails.isClickable = false // Disable clickability
            btnConfirmed.isEnabled = false
            btnConfirmed.isClickable = false // Disable clickability
            btnDelivered.isEnabled = false
            btnDelivered.isClickable = false // Disable clickability
        }
    }
}