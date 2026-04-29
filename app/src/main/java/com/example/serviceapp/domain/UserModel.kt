package com.example.serviceapp.domain

data class UserModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val phone: String = "",
    val blocked: Boolean = false,
    val profilePic: String = "",
    val fcmToken: String = ""
)