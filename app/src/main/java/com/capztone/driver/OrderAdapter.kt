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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class     OrderAdapter(private val context: Context, private val username: String?) :
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
            shopnameTextView.text = "${order.shopNames}"
            foodnameTextView.text = "${order.foodNames}"
            foodquantityTextView.text = "${order.foodQuantities}"
            orderdateTextView.text = "${order.orderDate}"

            val statusReference = firebaseDatabase.child("status").child(order.itemPushKey)
            statusReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val status = dataSnapshot.child("message").value?.toString() ?: ""
                    deliveryMessageTextView.visibility = View.VISIBLE
                    deliveryMessageTextView.text = status
                    // Slot check before enabling buttons
                    val slotTimeRange = selectedSlotTextView.text.toString()
                    val isInSlotTime = isCurrentTimeInSlot(slotTimeRange)

                    btnViewDetails.isEnabled = isInSlotTime
                    btnConfirmed.isEnabled = isInSlotTime
                    btnDelivered.isEnabled = isInSlotTime

                    when (status) {

                        "Order delivered" -> {
                            btnConfirmed.isEnabled = false
                            btnViewDetails.isEnabled = false
                            btnDelivered.isEnabled = false
                            btnViewDetails.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                            btnConfirmed.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                            btnDelivered.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                            itemView.alpha = 0.5f
                        }
                        "Order picked" -> {
                            btnViewDetails.isEnabled = false
                            btnConfirmed.isEnabled = false
                            btnViewDetails.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))
                            btnConfirmed.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))
                        }
                        "Order comfirmed" -> {
                            btnConfirmed.isEnabled = false
                            btnConfirmed.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))
                            itemView.alpha = 0.5f
                        }

                        else -> {
                            btnViewDetails.isEnabled = true
                            btnConfirmed.isEnabled = true
                            btnDelivered.isEnabled = true
                            btnViewDetails.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                            btnConfirmed.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                            btnDelivered.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                            itemView.alpha = 1.0f
                        }


                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Error fetching status from Firebase: ${databaseError.message}")
                }
            })

            drop.setOnClickListener { showPopupMenu(order.itemPushKey) }

            btnViewDetails.setOnClickListener {
                val slotTimeRange = selectedSlotTextView.text.toString()
                if (isCurrentTimeInSlot(slotTimeRange)) {
                    launchGoogleMapsDirections(order.address)
                    saveMessageToFirebase(order.itemPushKey, "Order picked", order.shopNames)
                    btnViewDetails.isEnabled = false
                    btnConfirmed.isEnabled = false
                } else {
                    showSlotTimeToast()
                }
            }

            btnConfirmed.setOnClickListener {
                if (!estimatedTimeSet) {
                    Toast.makeText(context, "Please set estimated time first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val slotTimeRange = selectedSlotTextView.text.toString()
                if (isCurrentTimeInSlot(slotTimeRange)) {
                    saveMessageToFirebase(order.itemPushKey, "Order confirmed", order.shopNames)
                    btnConfirmed.isEnabled = false
                    btnConfirmed.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.greenlightt
                        )
                    )
                } else {
                    showSlotTimeToast()
                }
            }


            btnDelivered.setOnClickListener {
                val slotTimeRange = selectedSlotTextView.text.toString()
                if (isCurrentTimeInSlot(slotTimeRange)) {
                    saveMessageToFirebase(order.itemPushKey, "Order delivered", order.shopNames)
                    btnDelivered.isEnabled = false
                    btnViewDetails.isEnabled = false
                    btnDelivered.isEnabled = false

                    btnDelivered.setBackgroundColor(ContextCompat.getColor(context, R.color.greenlightt))

                } else {
                    showSlotTimeToast()
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

        private fun isCurrentTimeInSlot(slotTimeRange: String): Boolean {
            try {
                val slotTimes = slotTimeRange.split("-")
                val startTime = slotTimes[0].trim()
                val endTime = slotTimes[1].trim()

                val currentTime = getCurrentTime()

                val startTimeParsed = SimpleDateFormat("hh:mma", Locale.getDefault()).parse(startTime)
                val endTimeParsed = SimpleDateFormat("hh:mma", Locale.getDefault()).parse(endTime)

                return currentTime.after(startTimeParsed) && currentTime.before(endTimeParsed)
            } catch (e: Exception) {
                Log.e("OrderAdapter", "Error parsing slot time: $slotTimeRange")
            }
            return false
        }

        private fun getCurrentTime(): Date {
            return Calendar.getInstance().time
        }

        private fun showSlotTimeToast() {
            Toast.makeText(context, "Not slot time, please click during slot time only", Toast.LENGTH_SHORT).show()
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

            messageReference.setValue(messageData)
                .addOnSuccessListener {
                    Log.d("Firebase", "Estimated time saved to Firebase for order $orderId: $time")
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "Firebase",
                        "Error saving estimated time to Firebase for order $orderId: $e"
                    )
                }
        }

        private fun launchGoogleMapsDirections(destination: String) {
            val uri = "google.navigation:q=$destination"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        }

        private fun saveMessageToFirebase(orderid: String, message: String, shopName: List<String>) {
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
                    val shopDeliveryReference = firebaseDatabase.child("${shop} delivery").child(orderid)
                    shopDeliveryReference.setValue(messageData)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Delivered message saved to ${shop} delivery: $message")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error saving delivered message to ${shop} delivery: $e")
                        }
                }
            }

            messageReference.setValue(messageData)
                .addOnSuccessListener {
                    Log.d("Firebase", "Message saved to Firebase: $message")
                }
                .addOnFailureListener { e ->
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