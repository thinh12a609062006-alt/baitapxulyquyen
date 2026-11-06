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
import java.util.Locale
import kotlin.coroutines.resume

/** Kết quả hiển thị lên giao diện */
data class LocationInfo(
    val latitude: Double,   // vĩ độ
    val longitude: Double,  // kinh độ
    val address: String?    // địa chỉ (có thể null nếu không tìm được)
)

/** Lấy vị trí hiện tại. Trả về null nếu thất bại (vd: GPS đang tắt). */
@SuppressLint("MissingPermission") // ta đã kiểm tra quyền trước khi gọi hàm này
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

/** Đổi tọa độ thành địa chỉ (ví dụ: "123 Nguyễn Huệ, Quận 1, TP.HCM") */
suspend fun getAddressFromLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    try {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+: cách mới, bất đồng bộ
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
            // Android 12 trở xuống: cách cũ
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()?.getAddressLine(0)
        }
    } catch (e: Exception) {
        null
    }
}