package com.example.serviceapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.serviceapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

open class BaseActivity : AppCompatActivity() {

    companion object {
        const val LOCATION_REQUEST_CODE = 2001
    }

    private var isLocationDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.lightBrown)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun setContentView(layoutResID: Int) {
        val baseView = layoutInflater.inflate(R.layout.activity_base, null)
        val container = baseView.findViewById<FrameLayout>(R.id.container)
        layoutInflater.inflate(layoutResID, container, true)
        super.setContentView(baseView)
        setupBottomBar()
        setupBackButton(true)
        saveFcmToken()
    }

    fun setBaseView(view: View, showBackButton: Boolean = true) {
        val baseView = layoutInflater.inflate(R.layout.activity_base, null)
        val container = baseView.findViewById<FrameLayout>(R.id.container)
        container.addView(view)
        super.setContentView(baseView)
        setupBottomBar()
        setupBackButton(showBackButton)
        saveFcmToken()
    }

    override fun onResume() {
        super.onResume()
        checkLocationStatus()
    }

    private fun checkLocationStatus() {
        if (isLocationDialogShowing) return
        val permissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showLocationPermissionDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_REQUEST_CODE
                )
            }
        } else {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!gpsEnabled) showGpsDisabledDialog()
        }
    }

    private fun showLocationPermissionDialog() {
        isLocationDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle("Location Required")
            .setMessage("ServEase needs your location to show nearby services. Please enable location permission in Settings and click the Top location Icon to filter.")
            .setCancelable(false)
            .setPositiveButton("Open Settings") { _, _ ->
                isLocationDialogShowing = false
                startActivity(Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                ))
            }
            .setNegativeButton("Exit App") { _, _ ->
                isLocationDialogShowing = false
                finishAffinity()
            }
            .show()
    }

    private fun showGpsDisabledDialog() {
        isLocationDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle("Enable Location")
            .setMessage("Location is disabled. Please enable location services for this feature.")
            .setCancelable(false)
            .setPositiveButton("Enable") { _, _ ->
                isLocationDialogShowing = false
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { _, _ ->
                isLocationDialogShowing = false
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationStatus()
            } else {
                showLocationPermissionDialog()
            }
        }
    }

    private fun saveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseDatabase.getInstance().reference
                .child("Users").child(uid).child("fcmToken")
                .setValue(token)
        }
    }

    private fun setupBackButton(show: Boolean) {
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        if (show) {
            backBtn.visibility = View.VISIBLE
            backBtn.setOnClickListener { finish() }
        } else {
            backBtn.visibility = View.GONE
        }
    }

    private fun setupBottomBar() {
        val homeBtn     = findViewById<ImageView>(R.id.homeBtn)
        val explorerBtn = findViewById<ImageView>(R.id.explorerBtn)
        val bookmarkBtn = findViewById<ImageView>(R.id.bookmarkBtn)
        val profileBtn  = findViewById<ImageView>(R.id.profileBtn)
        val adminBtn    = findViewById<ImageView>(R.id.adminBtn)

        val homeBtnLayout     = findViewById<LinearLayout>(R.id.homeBtnLayout)
        val explorerBtnLayout = findViewById<LinearLayout>(R.id.explorerBtnLayout)
        val bookmarkBtnLayout = findViewById<LinearLayout>(R.id.bookmarkBtnLayout)
        val profileBtnLayout  = findViewById<LinearLayout>(R.id.profileBtnLayout)
        val adminBtnLayout    = findViewById<LinearLayout>(R.id.adminBtnLayout)

        val homeIndicator     = findViewById<View>(R.id.homeIndicator)
        val explorerIndicator = findViewById<View>(R.id.explorerIndicator)
        val bookmarkIndicator = findViewById<View>(R.id.bookmarkIndicator)
        val profileIndicator  = findViewById<View>(R.id.profileIndicator)
        val adminIndicator    = findViewById<View>(R.id.adminIndicator)

        val purple = ContextCompat.getColor(this, R.color.lightPurple)
        val grey   = ContextCompat.getColor(this, android.R.color.darker_gray)

        fun resetAll() {
            listOf(homeBtn, explorerBtn, bookmarkBtn, profileBtn, adminBtn)
                .forEach { it.setColorFilter(grey) }
            listOf(homeIndicator, explorerIndicator, bookmarkIndicator,
                profileIndicator, adminIndicator)
                .forEach { it.visibility = View.INVISIBLE }
        }

        fun setActive(btn: ImageView, indicator: View) {
            resetAll()
            btn.setColorFilter(purple)
            indicator.visibility = View.VISIBLE
        }

        getUserRole { role ->
            when (role) {
                "admin" -> {
                    explorerBtnLayout.visibility = View.GONE
                    bookmarkBtnLayout.visibility = View.GONE
                    adminBtnLayout.visibility    = View.VISIBLE

                    when (this) {
                        is AdminUsersActivity     -> setActive(adminBtn, adminIndicator)
                        is ProfileActivity        -> setActive(profileBtn, profileIndicator)
                        else                      -> setActive(homeBtn, homeIndicator)
                    }

                    homeBtnLayout.setOnClickListener {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                    }
                    adminBtnLayout.setOnClickListener {
                        startActivity(Intent(this, AdminUsersActivity::class.java))
                    }
                    profileBtnLayout.setOnClickListener {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                }

                "provider" -> {
                    explorerBtnLayout.visibility = View.VISIBLE
                    bookmarkBtnLayout.visibility = View.GONE
                    adminBtnLayout.visibility    = View.GONE

                    when (this) {
                        is ProviderActivity  -> setActive(explorerBtn, explorerIndicator)
                        is ProfileActivity   -> setActive(profileBtn, profileIndicator)
                        else                 -> setActive(homeBtn, homeIndicator)
                    }

                    homeBtnLayout.setOnClickListener {
                        startActivity(Intent(this, ProviderDashboardActivity::class.java))
                    }
                    explorerBtnLayout.setOnClickListener {
                        startActivity(Intent(this, ProviderActivity::class.java))
                    }
                    profileBtnLayout.setOnClickListener {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                }

                else -> {
                    explorerBtnLayout.visibility = View.GONE
                    bookmarkBtnLayout.visibility = View.VISIBLE
                    adminBtnLayout.visibility    = View.GONE

                    when (this) {
                        is BookmarkActivity  -> setActive(bookmarkBtn, bookmarkIndicator)
                        is ProfileActivity   -> setActive(profileBtn, profileIndicator)
                        else                 -> setActive(homeBtn, homeIndicator)
                    }

                    homeBtnLayout.setOnClickListener {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    bookmarkBtnLayout.setOnClickListener {
                        startActivity(Intent(this, BookmarkActivity::class.java))
                    }
                    profileBtnLayout.setOnClickListener {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                }
            }
        }
    }

    private fun getUserRole(callback: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            callback("user")
            return
        }
        FirebaseDatabase.getInstance().reference
            .child("Users").child(uid).child("role")
            .get()
            .addOnSuccessListener { callback(it.value?.toString() ?: "user") }
            .addOnFailureListener { callback("user") }
    }
}