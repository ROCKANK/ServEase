package com.example.serviceapp.domain

import java.io.Serializable

data class BookingModel(
    var bookingId: String = "",
    var userId: String = "",
    var userName: String = "",
    var userEmail: String = "",
    var userPhone: String = "",
    var userLat: Double = 0.0,
    var userLon: Double = 0.0,
    var serviceId: String = "",
    var serviceTitle: String = "",
    var providerId: String = "",
    var status: String = "Pending",
    var serviceType: String = "inHome",
    var price: Int = 0,
    var timestamp: Long = System.currentTimeMillis()
) : Serializable