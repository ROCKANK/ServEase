package com.example.serviceapp.domain

import java.io.Serializable

data class ItemModel(
    var id: String = "",
    var bookingId: String = "",
    var timestamp: Long = 0L,
    var status: String = "Pending",
    var blocked: Boolean = false,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var serviceType: String = "inHome",
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val picUrl: String? = null,
    val profilePic: String? = null,
    val price: Long? = null,
    val oldPrice: Long? = null,
    val off: Long? = null,
    val classicPrice: Long? = null,
    val classicOldPrice: Long? = null,
    val premiumPrice: Long? = null,
    val premiumOldPrice: Long? = null,
    val platinumPrice: Long? = null,
    val platinumOldPrice: Long? = null,
    val name: String? = null,
    val job: String? = null,
    val categoryId: String? = null,
    val phone: String? = null,
    var providerId: String = ""
) : Serializable