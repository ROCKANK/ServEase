package com.example.serviceapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.serviceapp.R
import com.example.serviceapp.domain.BookingModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingRequestActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var booking: BookingModel
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_booking_request, null)
        setBaseView(view)

        booking = intent.getSerializableExtra("booking") as BookingModel

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(null)

        val bookingIdTxt    = findViewById<TextView>(R.id.bookingIdTxt)
        val timeTxt         = findViewById<TextView>(R.id.timeTxt)
        val userNameTxt     = findViewById<TextView>(R.id.userNameTxt)
        val userPhoneTxt    = findViewById<TextView>(R.id.userPhoneTxt)
        val userEmailTxt    = findViewById<TextView>(R.id.userEmailTxt)
        val serviceTitleTxt = findViewById<TextView>(R.id.serviceTitleTxt)
        val serviceTypeTxt  = findViewById<TextView>(R.id.serviceTypeTxt)
        val priceTxt        = findViewById<TextView>(R.id.priceTxt)
        val statusTxt       = findViewById<TextView>(R.id.statusTxt)
        val completeBtn     = findViewById<Button>(R.id.completeBtn)
        val mapContainer    = findViewById<View>(R.id.mapContainer)
        val emailContainer  = findViewById<View>(R.id.emailContainer)

        val shortId = if (booking.bookingId.length > 8)
            "#${booking.bookingId.takeLast(8).uppercase()}"
        else "#${booking.bookingId.uppercase()}"
        bookingIdTxt.text = shortId

        timeTxt.text = if (booking.timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            "🕐 ${sdf.format(Date(booking.timestamp))}"
        } else ""

        userNameTxt.text = booking.userName
        userEmailTxt.text = booking.userEmail

        if (booking.userPhone.isNotEmpty()) {
            userPhoneTxt.visibility = View.VISIBLE
            userPhoneTxt.text = "📞 ${booking.userPhone}"
            userPhoneTxt.setOnClickListener {
                val uri = Uri.parse("tel:${booking.userPhone}")
                startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
        } else {
            userPhoneTxt.visibility = View.GONE
        }

        serviceTitleTxt.text = booking.serviceTitle
        priceTxt.text = "₹${booking.price}"
        updateStatusUI(statusTxt, booking.status)

        if (booking.serviceType == "inHome") {
            mapContainer.visibility = View.VISIBLE
            emailContainer.visibility = View.GONE
            serviceTypeTxt.text = "🏠 In-Home Service"
            mapView.getMapAsync(this)
        } else {
            mapContainer.visibility = View.GONE
            emailContainer.visibility = View.VISIBLE
            serviceTypeTxt.text = "💻 Digital Service"
        }

        completeBtn.visibility = if (booking.status == "Pending") View.VISIBLE else View.GONE
        completeBtn.setOnClickListener { markAsCompleted(statusTxt, completeBtn) }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (booking.userLat != 0.0 && booking.userLon != 0.0) {
            val userLocation = LatLng(booking.userLat, booking.userLon)
            map.addMarker(MarkerOptions()
                .position(userLocation)
                .title("Customer Location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
        }
    }

    private fun markAsCompleted(statusTxt: TextView, completeBtn: Button) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("Bookings").child(uid).child(booking.bookingId)
            .child("status").setValue("Completed")
            .addOnSuccessListener {
                booking.status = "Completed"
                updateStatusUI(statusTxt, "Completed")
                completeBtn.visibility = View.GONE

                FirebaseDatabase.getInstance().reference
                    .child("Cart").child(booking.userId).child(booking.serviceId)
                    .child("status").setValue("Completed")

                Toast.makeText(this, "Marked as Completed!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStatusUI(statusTxt: TextView, status: String) {
        statusTxt.text = status
        statusTxt.setBackgroundResource(
            if (status == "Completed") R.drawable.status_completed_bg
            else R.drawable.status_pending_bg
        )
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}