package com.example.qareeb

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.presentation.MainScaffold
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Permission Launchers (Standard Android way, wrapped for Activity)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) startService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val sessionManager = SessionManager.getInstance(this)
        val taskRepo = TaskRepositoryImpl(db.taskDao())
        val financeRepo = TransactionRepositoryImpl(db.transactionDao())

        val userId = sessionManager.getUserId()
        if (!userId.isNullOrEmpty()) {
            lifecycleScope.launch {
                val syncRepo = SyncRepository(
                    taskDao = db.taskDao(),
                    api = RetrofitInstance.api,
                    prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                )
                syncRepo.sync(userId)
            }
        }

        setContent {
            // This is your Compose UI Entry Point
            MainScaffold(
                sessionManager = sessionManager,
                taskRepo = taskRepo,
                financeRepo = financeRepo,
                onStartQareeb = { checkPermissionsAndStart() }
            )
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
//        moveTaskToBack(true)
    }
}