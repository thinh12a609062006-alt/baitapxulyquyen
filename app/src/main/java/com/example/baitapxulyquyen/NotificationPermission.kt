package com.example.baitapxulyquyen

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberNotificationPermissionRequester(onDone: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnDone by rememberUpdatedState(onDone)

    var showComposeDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("Nhắc nhở") }
    var content by remember { mutableStateOf("Đây là nội dung thông báo thử nghiệm của bạn.") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showComposeDialog = true
        } else {
            message = "Bạn đã từ chối quyền thông báo. " +
                    "Hãy vào Cài đặt → Ứng dụng → app → Thông báo để bật lại."
        }
    }

    if (showComposeDialog) {
        AlertDialog(
            onDismissRequest = {
                showComposeDialog = false
                currentOnDone()
            },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            title = { Text("Gửi thông báo mới") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tiêu đề") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Nội dung") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Mẹo: chọn \"Gửi sau 5 giây\" rồi bấm nút Home để thoát app, " +
                            "sau đó nhấn vào thông báo để thử.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showComposeDialog = false
                    val t = title
                    val c = content
                    Toast.makeText(context, "Sẽ gửi thông báo sau 5 giây...", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        delay(5000)
                        showNotification(context, t, c)
                    }
                    currentOnDone()
                }) { Text("Gửi sau 5 giây") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showComposeDialog = false
                    showNotification(context, title, content)
                    currentOnDone()
                }) { Text("Gửi ngay") }
            }
        )
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = {
                message = null
                currentOnDone()
            },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                    currentOnDone()
                }) { Text("Đóng") }
            },
            title = { Text("Thông báo") },
            text = { Text(msg) }
        )
    }
    return {
        if (hasNotificationPermission(context)) {
            showComposeDialog = true
        } else if (Build.VERSION.SDK_INT >= 33) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            message = "Thông báo đang bị tắt. Hãy vào Cài đặt → Ứng dụng → app → Thông báo để bật."
        }
    }
}