package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class GoogleLocationTracker(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
) {

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchSingleHighAccuracyLocation(): Location? {
        if (!hasLocationPermission()) return null

        val cancellationTokenSource = CancellationTokenSource()
        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(30_000L)
                .setDurationMillis(10_000L)
                .build()

            val currentLoc = fusedLocationClient
                .getCurrentLocation(request, cancellationTokenSource.token)
                .await()

            currentLoc ?: fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.w(TAG, "High accuracy location fetch fallback to lastLocation: ${e.message}")
            try {
                fusedLocationClient.lastLocation.await()
            } catch (inner: Exception) {
                Log.w(TAG, "Last location unavailable: ${inner.message}")
                null
            }
        } finally {
            cancellationTokenSource.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(
        intervalMillis: Long = DEFAULT_INTERVAL_MS,
        minUpdateIntervalMillis: Long = FASTEST_INTERVAL_MS,
        minUpdateDistanceMeters: Float = MIN_DISPLACEMENT_METERS
    ): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission is not granted"))
            return@callbackFlow
        }

        val cancellationTokenSource = CancellationTokenSource()

        // Emit immediate location fix via Google Location API if available
        fusedLocationClient.lastLocation
            .addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    trySend(lastLoc)
                } else {
                    val currentReq = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                    fusedLocationClient.getCurrentLocation(currentReq, cancellationTokenSource.token)
                        .addOnSuccessListener { currentLoc ->
                            if (currentLoc != null) {
                                trySend(currentLoc)
                            }
                        }
                }
            }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(minUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(minUpdateDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val latestLocation = result.lastLocation ?: result.locations.lastOrNull()
                if (latestLocation != null) {
                    trySend(latestLocation)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates", e)
            close(e)
            return@callbackFlow
        }

        awaitClose {
            cancellationTokenSource.cancel()
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    companion object {
        private const val TAG = "GoogleLocationTracker"
        private const val DEFAULT_INTERVAL_MS = 10_000L
        private const val FASTEST_INTERVAL_MS = 5_000L
        private const val MIN_DISPLACEMENT_METERS = 10f
        private const val EARTH_RADIUS_KM = 6371.0

        /**
         * Calculates distance in kilometers rounded to 1 decimal place using Android Location API
         * with Haversine fallback.
         */
        fun calculateDistanceKm(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double
        ): Double {
            return try {
                val results = FloatArray(1)
                Location.distanceBetween(startLat, startLng, endLat, endLng, results)
                val km = (results[0] / 1000.0)
                if (km.isNaN() || km == 0.0 && (startLat != endLat || startLng != endLng)) {
                    haversineKm(startLat, startLng, endLat, endLng)
                } else {
                    ((km * 10.0).roundToInt() / 10.0).coerceAtLeast(0.1)
                }
            } catch (e: Exception) {
                haversineKm(startLat, startLng, endLat, endLng)
            }
        }

        private fun haversineKm(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val km = EARTH_RADIUS_KM * c
            return ((km * 10.0).roundToInt() / 10.0).coerceAtLeast(0.1)
        }

        /**
         * Calculates true geographic bearing in degrees [0, 360) from (startLat, startLng)
         * to (endLat, endLng), where 0° is North, 90° is East, 180° is South, 270° is West.
         */
        fun calculateBearingDegrees(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double
        ): Float {
            val lat1 = Math.toRadians(startLat)
            val lat2 = Math.toRadians(endLat)
            val dLon = Math.toRadians(endLng - startLng)

            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            val brng = Math.toDegrees(atan2(y, x))
            return ((brng + 360.0) % 360.0).toFloat()
        }
    }
}
