package com.example.driver

data class Order(
    val itemPushKey: String,
    val userUid: String,
    val userName: String,
    var address:String,
    var phone: String,
    var cancellationMessage:String,
    var isDelivered: Boolean = false

    // Add other order details here
) {



    // Secondary constructor to handle Firebase deserialization
    constructor() : this("","" ,"", "", "","")
}
