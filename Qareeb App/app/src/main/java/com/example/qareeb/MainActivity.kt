package com.example.qareeb

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.qareeb.screens.DashboardScreen
import com.example.qareeb.presentation.ui.components.FancyGradientBackground

class MainActivity : ComponentActivity() {

    // Permission Launchers (Standard Android way, wrapped for Activity)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) startService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This is your Compose UI Entry Point
           FancyGradientBackground {
               DashboardScreen("Manar","8 June 2025",9)
           }
        }
    }

    private fun checkPermissionsAndStart() {
        // 1. Check Overlay Permission (Special)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        // 2. Check Mic & Notification Permissions
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun startService() {
        val intent = Intent(this, QareebListeningService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        // Minimize App
        moveTaskToBack(true)
    }
}

// --- THIS IS YOUR NEW UI (Written in Kotlin, not XML) ---
@Composable
fun QareebHomeScreen(onStartClick: () -> Unit) {
    val context = LocalContext.current

    // Background Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E1E), Color(0xFF000000))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Header
            Text(
                text = "Qareeb AI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Agentic Assistant",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // The Big Power Button
            Button(
                onClick = onStartClick,
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF)
                ),
                elevation = ButtonDefaults.buttonElevation(10.dp)
            ) {
                Text(text = "ON", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Setup Guide:", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. Click button to grant permissions.", color = Color.LightGray, fontSize = 12.sp)
                    Text("2. App will minimize.", color = Color.LightGray, fontSize = 12.sp)
                    Text("3. Say 'Hi Q' anytime.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}