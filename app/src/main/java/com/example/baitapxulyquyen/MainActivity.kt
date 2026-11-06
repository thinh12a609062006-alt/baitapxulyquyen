package com.example.baitapxulyquyen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.baitapxulyquyen.ui.theme.BaitapxulyquyenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaitapxulyquyenTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    // (1) Dòng này gọi hàm trong file LocationPermission.kt
    val requestLocation = rememberLocationPermissionRequester()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // (2) Nhấn Allow thì xin quyền + lấy vị trí
            requestLocation()
        }) {
            Text("Allow")
        }
    }
}