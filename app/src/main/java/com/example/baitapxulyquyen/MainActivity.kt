package com.example.baitapxulyquyen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.baitapxulyquyen.ui.theme.BaitapxulyquyenTheme

class MainActivity : ComponentActivity() {

    private var openedNotification by mutableStateOf<NotificationData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel(this)
        readNotificationIntent(intent)

        setContent {
            BaitapxulyquyenTheme {
                LoginScreen(
                    openedNotification = openedNotification,
                    onCloseNotification = { openedNotification = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotificationIntent(intent)
    }
    private fun readNotificationIntent(intent: Intent?) {
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Thông báo"
        openedNotification = NotificationData(title, content)

        intent.removeExtra(EXTRA_TITLE)
        intent.removeExtra(EXTRA_CONTENT)
    }
}

@Composable
fun LoginScreen(
    openedNotification: NotificationData? = null,
    onCloseNotification: () -> Unit = {}
) {
    val requestLocation = rememberLocationPermissionRequester()
    val requestNotification = rememberNotificationPermissionRequester()
    val requestCamera = rememberCameraPermissionRequester()

    var showLocationCard by rememberSaveable { mutableStateOf(false) }
    var showNotificationCard by rememberSaveable { mutableStateOf(false) }
    var showCameraCard by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconTextButton(Icons.Default.LocationOn, "Location") { showLocationCard = true }
        Spacer(Modifier.height(16.dp))
        IconTextButton(Icons.Default.Notifications, "Notification") { showNotificationCard = true }
        Spacer(Modifier.height(16.dp))
        IconTextButton(Icons.Default.CameraAlt, "Camera") { showCameraCard = true }
    }
    if (showLocationCard) {
        PermissionCardDialog(
            icon = Icons.Default.LocationOn,
            title = "Location",
            message = "Allow maps to access your location while you use the app?",
            primaryText = "Allow",
            onPrimary = {
                showLocationCard = false
                requestLocation()
            },
            onSkip = { showLocationCard = false }
        )
    }


    if (showNotificationCard) {
        PermissionCardDialog(
            icon = Icons.Default.Notifications,
            title = "Notification",
            message = "Please enable notifications to receive updates and reminders",
            primaryText = "Turn on",
            onPrimary = {
                showNotificationCard = false
                requestNotification()
            },
            onSkip = { showNotificationCard = false }
        )
    }
    if (showCameraCard) {
        PermissionCardDialog(
            icon = Icons.Default.CameraAlt,
            title = "Camera",
            message = "We need access to your camera to scan QR codes",
            primaryText = "Turn on",
            onPrimary = {
                showCameraCard = false
                requestCamera()
            },
            onSkip = { showCameraCard = false }
        )
    }
    if (openedNotification != null) {
        AlertDialog(
            onDismissRequest = onCloseNotification,
            confirmButton = { TextButton(onClick = onCloseNotification) { Text("Đóng") } },
            title = { Text(openedNotification.title) },
            text = { Text(openedNotification.content) }
        )
    }
}

@Composable
private fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(0.8f)) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}