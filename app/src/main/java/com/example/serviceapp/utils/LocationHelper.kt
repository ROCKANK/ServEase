package com.example.serviceapp.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onSuccess: (Double, Double) -> Unit, onFail: () -> Unit) {
        if (!hasPermission()) {
            onFail()
            return
        }
        if (!isLocationEnabled()) {
            onFail()
            return
        }

        val cancellationToken = CancellationTokenSource()
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        )
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    fusedClient.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) onSuccess(last.latitude, last.longitude)
                            else onFail()
                        }
                        .addOnFailureListener { onFail() }
                }
            }
            .addOnFailureListener { onFail() }
    }

    companion object {
        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val result = FloatArray(1)
            android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, result)
            return (result[0] / 1000).toDouble()
        }
    }
}