package com.example.baitapxulyquyen

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume


data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)


@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location -> cont.resume(location) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}


suspend fun getAddressFromLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    return getAddressByGeocoder(context, latitude, longitude)
        ?: getAddressByNominatim(latitude, longitude)
}

private suspend fun getAddressByGeocoder(
    context: Context,
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    try {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull()?.getAddressLine(0))
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(null)
                        }
                    })
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()?.getAddressLine(0)
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun getAddressByNominatim(
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL(
            "https://nominatim.openstreetmap.org/reverse" +
                    "?format=jsonv2&lat=$latitude&lon=$longitude&accept-language=vi"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "baitapxulyquyen/1.0")
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        JSONObject(text).optString("display_name").ifBlank { null }
    } catch (e: Exception) {
        null
    }
}