package com.example.serviceapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.serviceapp.R
import com.google.firebase.database.FirebaseDatabase

class AdminDashboardActivity : BaseActivity() {

    private lateinit var usersTxt: TextView
    private lateinit var providersTxt: TextView
    private lateinit var bookingsTxt: TextView
    private lateinit var servicesTxt: TextView
    private lateinit var reportsTxt: TextView
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_admin_dashboard, null)
        setBaseView(view)

        usersTxt     = findViewById(R.id.usersTxt)
        providersTxt = findViewById(R.id.providersTxt)
        bookingsTxt  = findViewById(R.id.bookingsTxt)
        servicesTxt  = findViewById(R.id.servicesTxt)
        reportsTxt   = findViewById(R.id.reportsTxt)

        loadCounts()

        findViewById<CardView>(R.id.usersLayout).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java).apply {
                putExtra("type", "user")
            })
        }
        findViewById<CardView>(R.id.providersLayout).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java).apply {
                putExtra("type", "provider")
            })
        }
        findViewById<CardView>(R.id.bookingsLayout).setOnClickListener {
            startActivity(Intent(this, AdminBookingsActivity::class.java))
        }
        findViewById<CardView>(R.id.servicesLayout).setOnClickListener {
            startActivity(Intent(this, AdminServicesActivity::class.java))
        }
        findViewById<CardView>(R.id.reportsLayout).setOnClickListener {
            startActivity(Intent(this, AdminReportsActivity::class.java))
        }
    }

    private fun loadCounts() {
        // Users + Providers
        db.child("Users").get().addOnSuccessListener { snapshot ->
            var userCount = 0
            var providerCount = 0
            for (snap in snapshot.children) {
                val role = snap.child("role").value?.toString()?.ifEmpty { "user" } ?: "user"
                val email = snap.child("email").getValue(String::class.java)
                if (!email.isNullOrEmpty() && email != "user") {
                    if (role == "provider") providerCount++ else userCount++
                }
                Log.d("ADMIN_DEBUG", "User: ${snap.key}, role: $role")
            }
            usersTxt.text = "Users: $userCount"
            providersTxt.text = "Providers: $providerCount"
        }

        // ✅ Bookings — structure is Bookings/{providerId}/{bookingId}
        // so we need to count all bookings across all providers
        db.child("Bookings").get().addOnSuccessListener { snapshot ->
            var totalBookings = 0L
            for (providerSnap in snapshot.children) {
                // each child of providerId is a bookingId
                totalBookings += providerSnap.childrenCount
            }
            bookingsTxt.text = "Bookings: $totalBookings"
        }

        // Services
        db.child("Items").get().addOnSuccessListener { snapshot ->
            servicesTxt.text = "Services: ${snapshot.childrenCount}"
        }

        // Reports
        db.child("Reports").get().addOnSuccessListener { snapshot ->
            reportsTxt.text = "Reports: ${snapshot.childrenCount}"
        }
    }
}