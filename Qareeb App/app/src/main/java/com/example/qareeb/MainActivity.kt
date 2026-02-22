package com.example.qareeb

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.data.repositoryImp.UserRepositoryImpl
import com.example.qareeb.presentation.MainScaffold
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // Permission Launchers (Standard Android way, wrapped for Activity)
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val granted = permissions.entries.all { it.value }
//        if (granted) startService()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val sessionManager = SessionManager.getInstance(this)
        val taskRepo = TaskRepositoryImpl(db.taskDao())
        val financeRepo = TransactionRepositoryImpl(db.transactionDao())
        //val userRepo = UserRepositoryImpl(db.userDao())
        val userRepo = com.example.qareeb.data.repositoryImp.UserRepositoryImpl(db.userDao())

//        checkPermissionsAndStart()


        /*val userId = sessionManager.getUserId()
        if (!userId.isNullOrEmpty()) {
            lifecycleScope.launch {
                val syncRepo = SyncRepository(
                    taskDao = db.taskDao(),
                    api = RetrofitInstance.api,
                    prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                )
                Log.d("SYNC", "MainActivity calling sync, userId=$userId")

                syncRepo.sync("ccabd069-bb4f-465f-9bdd-8f711b85cb18")
            }
        }*/

        /* lifecycleScope.launch {
            val testUserId = "ccabd069-bb4f-465f-9bdd-8f711b85cb18"

            // Save session so ViewModel uses the same userId
            sessionManager.saveUserSession(
                userId = testUserId,
                username = "Farida",
                email = "farida"
            )

            val syncRepo = SyncRepository(
                taskDao = db.taskDao(),
                userDao = db.userDao(),
                api = RetrofitInstance.api,
                prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            )

            Log.d("SYNC", "MainActivity calling sync, userId=$testUserId")
            syncRepo.sync(testUserId)
        }*/


        val syncRepository = SyncRepository(
            taskDao = db.taskDao(),
            userDao = db.userDao(),
            api = RetrofitInstance.syncApi,  // Changed from .api to .syncApi
            prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        )

// Sync on app start if already logged in
        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            if (!userId.isNullOrEmpty()) {
                val localTasks = db.taskDao().getTasksByUserOneShot(userId)
                if (localTasks.isEmpty()) {
                    // No local tasks → sync to get them from server
                    Log.d("SYNC", "No local tasks, syncing...")
                    syncRepository.sync(userId)
                } else {
                    // Tasks already in DB → no need to sync
                    Log.d("SYNC", "Already have ${localTasks.size} tasks in DB, skipping sync")
                }
            } else {
                Log.d("SYNC", "No user logged in, skipping sync")
            }
        }


            setContent {
                MainScaffold(
                    sessionManager = sessionManager,
                    taskRepo = taskRepo,
                    financeRepo = financeRepo,
                    syncRepository = syncRepository,
                    userRepository = userRepo
                )
            }
        }

//    private fun checkPermissionsAndStart() {
//        // 1. Check Overlay Permission (Special)
//        if (!Settings.canDrawOverlays(this)) {
//            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
//            startActivity(intent)
//            return
//        }
//
//        // 2. Check Mic & Notification Permissions
//        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
//        if (Build.VERSION.SDK_INT >= 33) {
//            perms.add(Manifest.permission.POST_NOTIFICATIONS)
//        }
//
//        requestPermissionLauncher.launch(perms.toTypedArray())
//    }
//
//    private fun startService() {
//        val intent = Intent(this, QareebListeningService::class.java)
//        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
//        // Minimize App
//        moveTaskToBack(true)
//    }
//}

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
                        Text(
                            text = "ON",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
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
                            Text(
                                "1. Click button to grant permissions.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            Text("2. App will minimize.", color = Color.LightGray, fontSize = 12.sp)
                            Text(
                                "3. Say 'Hi Q' anytime.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }