package com.example.serviceapp.activities

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.databinding.ActivityDetailBinding
import com.example.serviceapp.domain.BookingModel
import com.example.serviceapp.domain.ItemModel
import com.example.serviceapp.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DetailActivity : BaseActivity() {

    private var position: Int = 0
    private lateinit var binding: ActivityDetailBinding
    private lateinit var item: ItemModel

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setBaseView(binding.root)
        bundle()
    }

    private fun bundle() {
        binding.apply {
            val data = intent.getSerializableExtra("object") as? ItemModel
            if (data == null) {
                Toast.makeText(this@DetailActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            item = data
            position = intent.getIntExtra("position", 0)

            Glide.with(this@DetailActivity).load(item.picUrl).into(pic)
            pic.setBackgroundColor(resources.getColor(backgrounds[position]))

            titleTxt.text = item.title
            priceTxt.text = "₹${item.price}"
            oldPriceTxt.text = "₹${item.oldPrice}"
            oldPriceTxt.paintFlags = oldPriceTxt.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            if ((item.off ?: 0) > 0) {
                imageView7.visibility = View.VISIBLE
                offTxt.visibility = View.VISIBLE
                offTxt.text = "%${item.off}\nOff"
            } else {
                imageView7.visibility = View.GONE
                offTxt.visibility = View.GONE
            }

            if (!item.serviceType.isNullOrEmpty()) {
                typeTxt.visibility = View.VISIBLE
                typeTxt.text = if (item.serviceType == "inHome") "🏠 In-Home" else "💻 Digital"
            } else {
                typeTxt.visibility = View.GONE
            }

            price1.text = "₹${item.classicPrice}"
            price2.text = "₹${item.premiumPrice}"
            price3.text = "₹${item.platinumPrice}"

            oldprice1.text = "₹${item.classicOldPrice}"
            oldprice2.text = "₹${item.premiumOldPrice}"
            oldprice3.text = "₹${item.platinumOldPrice}"

            oldprice1.paintFlags = oldprice1.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            oldprice2.paintFlags = oldprice2.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            oldprice3.paintFlags = oldprice3.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            nameTxt.text = item.name
            jobTxt.text = item.job
            aboutTxt.text = item.description

            Glide.with(this@DetailActivity).load(item.profilePic).into(profilePic)

            backBtn.setOnClickListener { finish() }
            callBtn.setOnClickListener {
                dialNumber(this@DetailActivity, item.phone.toString())
            }
            messageBtn.setOnClickListener {
                sendSms(this@DetailActivity, item.phone.toString(),
                    "Hello! I'm interested in your service.")
            }
            bookNowBtn.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_booking_confirm, null)
                dialogView.findViewById<TextView>(R.id.confirmTitle).text = item.title
                dialogView.findViewById<TextView>(R.id.confirmType).text =
                    if (item.serviceType == "inHome") "🏠 In-Home" else "💻 Digital"
                dialogView.findViewById<TextView>(R.id.confirmPrice).text = "₹${item.price}"

                if ((item.off ?: 0) > 0) {
                    dialogView.findViewById<TextView>(R.id.confirmOff).text = "${item.off}% Off"
                    dialogView.findViewById<TextView>(R.id.confirmOff).visibility = View.VISIBLE
                } else {
                    dialogView.findViewById<TextView>(R.id.confirmOff).visibility = View.GONE
                }

                Glide.with(this@DetailActivity)
                    .load(item.picUrl)
                    .into(dialogView.findViewById(R.id.confirmImage))

                AlertDialog.Builder(this@DetailActivity)
                    .setView(dialogView)
                    .setPositiveButton("Confirm Booking") { _, _ ->
                        addToCart() // proceed with booking
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                favBtn.setOnClickListener {
                    Toast.makeText(this@DetailActivity,
                        "Please login first", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val uid = user.uid
            val db = FirebaseDatabase.getInstance().reference
            val key = if (!item.id.isNullOrEmpty()) item.id!!
            else item.title?.replace(" ", "_") ?: System.currentTimeMillis().toString()
            item.id = key
            var isBookmarked = false

            db.child("Bookmarks").child(uid).child(key).get()
                .addOnSuccessListener { snapshot ->
                    isBookmarked = snapshot.exists()
                    if (isBookmarked) favBtn.setImageResource(R.drawable.favcon1)
                    else favBtn.setImageResource(R.drawable.favicon)
                }

            favBtn.setOnClickListener {
                val ref = db.child("Bookmarks").child(uid).child(key)
                if (isBookmarked) {
                    ref.removeValue()
                    favBtn.setImageResource(R.drawable.favicon)
                    isBookmarked = false
                    Toast.makeText(this@DetailActivity,
                        "Removed from bookmarks", Toast.LENGTH_SHORT).show()
                } else {
                    item.id = key
                    ref.setValue(item)
                    favBtn.setImageResource(R.drawable.favcon1)
                    isBookmarked = true
                    Toast.makeText(this@DetailActivity, "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun addToCart() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val key = if (!item.id.isNullOrEmpty()) item.id!!
        else item.title?.replace(" ", "_") ?: System.currentTimeMillis().toString()
        item.id = key

        if (item.serviceType == "inHome") {
            val locationHelper = LocationHelper(this)
            if (locationHelper.hasPermission()) {
                locationHelper.getCurrentLocation(
                    onSuccess = { lat, lon -> saveBooking(user.uid, key, lat, lon) },
                    onFail = { saveBooking(user.uid, key, 0.0, 0.0) }
                )
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Location needed")
                    .setMessage("This is an in-home service. Allow location so provider can find you?")
                    .setPositiveButton("Allow") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_PERMISSION_REQUEST
                        )
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        saveBooking(user.uid, key, 0.0, 0.0)
                    }
                    .show()
            }
        } else {
            saveBooking(user.uid, key, 0.0, 0.0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addToCart()
        }
    }

    private fun saveBooking(uid: String, key: String, userLat: Double, userLon: Double) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase.getInstance().reference

        db.child("Users").child(uid).get().addOnSuccessListener { snapshot ->
            val userName = snapshot.child("name").getValue(String::class.java) ?: "User"
            val userPhone = snapshot.child("phone").getValue(String::class.java) ?: ""

            val bookingId = db.child("Bookings").push().key!!
            val timestamp = System.currentTimeMillis()

            item.bookingId = bookingId
            item.timestamp = timestamp

            db.child("Cart").child(uid).child(key).setValue(item)

            val booking = BookingModel(
                bookingId    = bookingId,
                userId       = uid,
                userName     = userName,
                userEmail    = user.email ?: "",
                userPhone    = userPhone,
                userLat      = userLat,
                userLon      = userLon,
                serviceId    = key,
                serviceTitle = item.title ?: "",
                providerId   = item.providerId,
                status       = "Pending",
                serviceType  = item.serviceType,
                price = item.price?.toInt() ?: 0,
                timestamp    = timestamp
            )

            db.child("Bookings").child(item.providerId).child(bookingId)
                .setValue(booking)
                .addOnSuccessListener {
                    Toast.makeText(this, "Booked! Check your cart.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Booking failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun dialNumber(context: Context, phoneNumber: String) {
        val uri = Uri.parse("tel:${Uri.encode(phoneNumber.trim())}")
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Dialer not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phoneNumber: String, body: String = "") {
        val uri = Uri.parse("smsto:${Uri.encode(phoneNumber.trim())}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "SMS App not found", Toast.LENGTH_SHORT).show()
        }
    }

    private val backgrounds = listOf(
        R.color.pink,
        R.color.green,
        R.color.brown,
        R.color.blue,
    )
}