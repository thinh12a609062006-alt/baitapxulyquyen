package com.example.baitapxulyquyen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Các trạng thái của chức năng Location */
sealed interface LocationUiState {
    data object Idle : LocationUiState                       // chưa làm gì
    data object Loading : LocationUiState                    // đang lấy vị trí
    data class Success(val info: LocationInfo) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

/**
 * Cách dùng: gọi hàm này 1 lần trong màn hình có nút Allow.
 * Nó trả về 1 hàm; khi gọi hàm đó (khi nhấn Allow) thì bắt đầu xin quyền.
 */
@Composable
fun rememberLocationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LocationUiState>(LocationUiState.Idle) }

    // Hàm lấy vị trí + địa chỉ (chỉ gọi sau khi đã có quyền)
    fun fetchLocation() {
        state = LocationUiState.Loading
        scope.launch {
            val location = getCurrentLocation(context)
            if (location == null) {
                state = LocationUiState.Error(
                    "Không lấy được vị trí. Hãy bật GPS (Location) trên điện thoại rồi thử lại."
                )
            } else {
                val address = getAddressFromLocation(
                    context, location.latitude, location.longitude
                )
                state = LocationUiState.Success(
                    LocationInfo(location.latitude, location.longitude, address)
                )
            }
        }
    }

    // Bộ xin quyền của Android: kết quả trả về trong lambda này
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocation()
        } else {
            state = LocationUiState.Error("Bạn đã từ chối quyền vị trí.")
        }
    }

    // Hiển thị kết quả bằng hộp thoại (không đụng vào giao diện cũ)
    when (val s = state) {
        LocationUiState.Idle -> Unit

        LocationUiState.Loading -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Đang lấy vị trí...") },
            text = { CircularProgressIndicator() }
        )

        is LocationUiState.Success -> {
            // Đổi tọa độ thành điểm trên bản đồ
            val point = GeoPoint(s.info.latitude, s.info.longitude)

            AlertDialog(
                onDismissRequest = { state = LocationUiState.Idle },
                confirmButton = {
                    TextButton(onClick = { state = LocationUiState.Idle }) { Text("Đóng") }
                },
                title = { Text("Vị trí hiện tại") },
                text = {
                    Column {
                        Text("Vĩ độ: ${s.info.latitude}")
                        Spacer(Modifier.height(4.dp))
                        Text("Kinh độ: ${s.info.longitude}")
                        Spacer(Modifier.height(4.dp))
                        Text("Địa chỉ: ${s.info.address ?: "Không tìm thấy địa chỉ"}")
                        Spacer(Modifier.height(12.dp))

                        // Bản đồ OpenStreetMap (View của Android nhúng vào Compose)
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            factory = { ctx ->
                                // Bắt buộc: khai báo tên app để server bản đồ không chặn
                                Configuration.getInstance().userAgentValue = ctx.packageName

                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK) // kiểu bản đồ chuẩn
                                    setMultiTouchControls(true)             // zoom bằng 2 ngón
                                    controller.setZoom(17.0)                // mức zoom
                                    controller.setCenter(point)             // zoom đến vị trí của bạn

                                    // Ghim đánh dấu vị trí
                                    val marker = Marker(this)
                                    marker.position = point
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    marker.title = "Bạn đang ở đây"
                                    overlays.add(marker)
                                }
                            }
                        )
                    }
                }
            )
        }

        is LocationUiState.Error -> AlertDialog(
            onDismissRequest = { state = LocationUiState.Idle },
            confirmButton = {
                TextButton(onClick = { state = LocationUiState.Idle }) { Text("Đóng") }
            },
            title = { Text("Thông báo") },
            text = { Text(s.message) }
        )
    }

    // Hàm trả về: gọi khi nhấn Allow
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}