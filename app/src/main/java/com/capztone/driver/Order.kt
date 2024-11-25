package com.capztone.driver

data class Order(
    val itemPushKey: String,
    val userUid: String,
    val userName: String,
    var address: String,
    var selectedSlot: String,
    var cancellationMessage: String,
    val shopNames: List<String> = emptyList(),
    var foodNames: List<String> = emptyList(),
    var foodQuantities: List<Int> = emptyList(),
    var orderDate: String,
    var isDelivered: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String

    // Add other order details here
) {
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        "",
        emptyList(),
        emptyList(), // Added missing empty list for foodNames
        emptyList(), // Added missing empty list for foodQuantities
        "",
        false,
        null,
        null,
        ""
    )
}
