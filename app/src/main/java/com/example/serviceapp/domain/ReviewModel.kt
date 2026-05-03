package com.example.serviceapp.domain

import java.io.Serializable

data class ReviewModel(
    var reviewId: String = "",
    var userId: String = "",
    var userName: String = "",
    var userPic: String = "",
    var serviceId: String = "",
    var rating: Float = 0f,
    var body: String = "",
    var timestamp: Long = System.currentTimeMillis()
) : Serializable