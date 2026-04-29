package com.example.serviceapp.repository

import com.example.serviceapp.domain.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String,
        callback: (Boolean) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    callback(false)
                    return@addOnSuccessListener
                }

                val user = UserModel(
                    id      = uid,
                    name    = name,
                    email   = email,
                    role    = role,
                    phone   = phone,
                    blocked = false
                )

                db.child("Users").child(uid).setValue(user)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(false)
            }
    }

    fun login(
        email: String,
        password: String,
        callback: (String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    callback(null)
                    return@addOnSuccessListener
                }
                db.child("Users").child(uid).get()
                    .addOnSuccessListener {
                        val role = it.child("role").value?.toString()
                        callback(role)
                    }
                    .addOnFailureListener { callback(null) }
            }
            .addOnFailureListener { callback(null) }
    }

    fun saveGoogleUser(
        uid: String,
        map: HashMap<String, Any>,
        callback: (Boolean) -> Unit
    ) {
        db.child("Users").child(uid)
            .setValue(map)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}