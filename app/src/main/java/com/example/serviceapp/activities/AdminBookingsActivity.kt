package com.example.serviceapp.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.AdminBookingAdapter
import com.example.serviceapp.domain.BookingModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminBookingsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminBookingAdapter
    private val list = ArrayList<BookingModel>()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_admin_users, null)
        setBaseView(view)

        findViewById<TextView>(R.id.titleTxt).text = "All Bookings"
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminBookingAdapter(list)
        recyclerView.adapter = adapter

        loadBookings()
    }

    private fun loadBookings() {
        // ✅ Structure is Bookings/{providerId}/{bookingId}
        db.child("Bookings").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()

                // ✅ First loop: iterate provider nodes
                for (providerSnap in snapshot.children) {
                    // ✅ Second loop: iterate actual booking nodes
                    for (bookingSnap in providerSnap.children) {
                        try {
                            val booking = BookingModel(
                                bookingId    = bookingSnap.child("bookingId").getValue(String::class.java) ?: bookingSnap.key ?: "",
                                userId       = bookingSnap.child("userId").getValue(String::class.java) ?: "",
                                userName     = bookingSnap.child("userName").getValue(String::class.java) ?: "Unknown",
                                userEmail    = bookingSnap.child("userEmail").getValue(String::class.java) ?: "",
                                userPhone    = bookingSnap.child("userPhone").getValue(String::class.java) ?: "",
                                serviceId    = bookingSnap.child("serviceId").getValue(String::class.java) ?: "",
                                serviceTitle = bookingSnap.child("serviceTitle").getValue(String::class.java) ?: "Unknown Service",
                                providerId   = bookingSnap.child("providerId").getValue(String::class.java) ?: "",
                                status       = bookingSnap.child("status").getValue(String::class.java) ?: "Pending",
                                serviceType  = bookingSnap.child("serviceType").getValue(String::class.java) ?: "inHome",
                                price        = bookingSnap.child("price").getValue(Long::class.java)?.toInt() ?: 0,
                                userLat      = bookingSnap.child("userLat").getValue(Double::class.java) ?: 0.0,
                                userLon      = bookingSnap.child("userLon").getValue(Double::class.java) ?: 0.0,
                                timestamp    = bookingSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                            )
                            list.add(booking)
                        } catch (e: Exception) {
                            android.util.Log.e("ADMIN_BOOKINGS", "Error: ${e.message}")
                        }
                    }
                }

                adapter.notifyDataSetChanged()

                if (list.isEmpty()) {
                    Toast.makeText(
                        this@AdminBookingsActivity,
                        "No bookings found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AdminBookingsActivity,
                    "Failed to load bookings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}