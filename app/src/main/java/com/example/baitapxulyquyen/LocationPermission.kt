package com.example.baitapxulyquyen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch


sealed interface LocationUiState {
    data object Idle : LocationUiState
    data object Loading : LocationUiState
    data class Success(val info: LocationInfo) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

fun openInGoogleMaps(context: Context, lat: Double, lng: Double) {
    val label = Uri.encode("Bạn đang ở đây")
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    val appIntent = Intent(Intent.ACTION_VIEW, geoUri).setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(appIntent)
    } catch (e: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        )
        try {
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Không mở được Google Maps", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberLocationPermissionRequester(onDone: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnDone by rememberUpdatedState(onDone)
    var state by remember { mutableStateOf<LocationUiState>(LocationUiState.Idle) }

    fun closeAndFinish() {
        state = LocationUiState.Idle
        currentOnDone()
    }

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

    when (val s = state) {
        LocationUiState.Idle -> Unit

        LocationUiState.Loading -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Đang lấy vị trí...") },
            text = { CircularProgressIndicator() }
        )

        is LocationUiState.Success -> {
            val lat = s.info.latitude
            val lng = s.info.longitude
            // Địa chỉ trang Google Maps nhúng (không cần API key), ghim đúng tọa độ
            val mapUrl = "https://maps.google.com/maps?q=$lat,$lng&z=17&hl=vi&output=embed"

            Dialog(
                onDismissRequest = { closeAndFinish() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webViewClient = WebViewClient()
                                    loadUrl(mapUrl)
                                }
                            },
                            onRelease = { it.destroy() }
                        )

                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 40.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Bạn đang ở đây", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Vĩ độ: $lat")
                                Text("Kinh độ: $lng")
                                Spacer(Modifier.height(4.dp))
                                Text("Địa chỉ: ${s.info.address ?: "Không tìm thấy địa chỉ"}")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = { openInGoogleMaps(context, lat, lng) }) {
                                Text("Mở Google Maps")
                            }
                            OutlinedButton(onClick = { closeAndFinish() }) {
                                Text("Đóng")
                            }
                        }
                    }
                }
            }
        }

        is LocationUiState.Error -> AlertDialog(
            onDismissRequest = { closeAndFinish() },
            confirmButton = {
                TextButton(onClick = { closeAndFinish() }) { Text("Đóng") }
            },
            title = { Text("Thông báo") },
            text = { Text(s.message) }
        )
    }
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}