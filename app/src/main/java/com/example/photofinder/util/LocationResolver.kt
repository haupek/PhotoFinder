package com.example.photofinder.util

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Resolves a search center for the radius search, fully on-device:
 *  - a fresh device location (active fix, with last-known fallback), and
 *  - a place name via the Android Geocoder (may require connectivity).
 */
class LocationResolver(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Requests a fresh single location fix (Android 11+), falling back to the
     * last known location if a fresh fix is unavailable or on older versions.
     */
    suspend fun freshLocation(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

            if (provider != null) {
                val fresh = suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    val executor = Executors.newSingleThreadExecutor()
                    try {
                        manager.getCurrentLocation(provider, signal, executor) { location ->
                            cont.resume(location?.let { it.latitude to it.longitude })
                        }
                    } catch (e: SecurityException) {
                        cont.resume(null)
                    }
                }
                if (fresh != null) return fresh
            }
        }
        return lastKnownLocation()
    }

    /** Best-effort last known location across the available providers. */
    suspend fun lastKnownLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            val location = try {
                manager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            }
            if (location != null) return@withContext location.latitude to location.longitude
        }
        null
    }

    /** Resolves coordinates to a human-readable place name via the Geocoder. */
    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val matches = geocoder.getFromLocation(lat, lon, 1)
            val a = matches?.firstOrNull() ?: return@withContext null
            val locality = a.locality ?: a.subAdminArea ?: a.adminArea
            listOfNotNull(locality, a.countryName).joinToString(", ").ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    /** Resolves a place name to coordinates via the Geocoder. */
    @Suppress("DEPRECATION")
    suspend fun geocode(placeName: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val query = placeName.trim()
        if (query.isEmpty()) return@withContext null
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val matches = geocoder.getFromLocationName(query, 1)
            val first = matches?.firstOrNull() ?: return@withContext null
            first.latitude to first.longitude
        } catch (e: Exception) {
            null
        }
    }
}
