package com.example.qareeb

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.data.remote.SyncApi
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.CategoryRepositoryImpl
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.data.repositoryImp.UserRepositoryImpl
import com.example.qareeb.notification.TaskNotificationReceiver
import com.example.qareeb.presentation.MainScaffold
import com.example.qareeb.presentation.screens.SplashScreen
import com.example.qareeb.presentation.utilis.SessionManager
import com.example.qareeb.security.AppLockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val TAG = "QAREEB_DEBUG"

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var syncRepository: SyncRepository
    private lateinit var categoryRepo: CategoryRepositoryImpl  // ✅ added

    private var pendingStartAfterOverlay = false

    // ✅ Sync broadcast receiver
    private val syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val userId = intent.getStringExtra("userID")
                ?: sessionManager.getUserId()
                ?: return
            Log.d("SYNC", "Sync broadcast received for userID=$userId")
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { syncRepository.sync(userId) }
                    Log.d("SYNC", "Sync after notification completed")
                } catch (e: Exception) {
                    Log.e("SYNC", "Sync after notification failed: ${e.message}", e)
                }
            }
        }
    }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (pendingStartAfterOverlay) {
                pendingStartAfterOverlay = false
                checkPermissionsAndStart()
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startQareebService()
            } else {
                Log.w(TAG, "Permissions denied: $permissions")
            }
        }

    // ─────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager.getInstance(this)
        categoryRepo = CategoryRepositoryImpl(db.categoryDao())  // ✅ initialized

        val taskRepo = TaskRepositoryImpl(db.taskDao())
        val financeRepo = TransactionRepositoryImpl(db.transactionDao())
        val userRepo = UserRepositoryImpl(db.userDao())

        syncRepository = SyncRepository(
            taskDao = db.taskDao(),
            userDao = db.userDao(),
            transactionDao = db.transactionDao(),
            promptDao = db.promptDao(),
            memoryDao = db.memoryDao(),
            api = RetrofitInstance.syncApi,
            prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        )

        // ✅ Register sync broadcast receiver
        val filter = IntentFilter("com.example.qareeb.SYNC_NOW")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(syncReceiver, filter)
        }

        // ✅ Register FCM token
        registerFcmToken()

        // Sync on app start
        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            if (!userId.isNullOrEmpty()) {
                val localTasks = withContext(Dispatchers.IO) {
                    db.taskDao().getTasksByUserOneShot(userId)
                }
                val localTransactions = withContext(Dispatchers.IO) {
                    db.transactionDao().getTransactionsByUserOneShot(userId)
                }
                if (localTasks.isEmpty() || localTransactions.isEmpty()) {
                    Log.d("SYNC", "Missing data → syncing...")
                    try {
                        withContext(Dispatchers.IO) { syncRepository.sync(userId) }
                    } catch (e: Exception) {
                        Log.e("SYNC", "Sync failed: ${e.message}", e)
                    }
                } else {
                    Log.d("SYNC", "All data present → skipping sync")
                }
            } else {
                Log.d("SYNC", "No user logged in, skipping sync")
            }
        }

        // ✅ Single setContent — with app lock gate
        setContent {
            var isAppUnlocked by remember { mutableStateOf(false) }

            // ✅ Only re-show splash when coming BACK from background
            // not on first launch
            LaunchedEffect(AppLockManager.isLocked) {
                if (AppLockManager.isLocked && AppLockManager.hasBeenUnlockedOnce) {
                    isAppUnlocked = false
                }
            }

            if (!isAppUnlocked) {
                SplashScreen(
                    activity = this@MainActivity,
                    onSplashFinished = {
                        isAppUnlocked = true
                    }
                )
            } else {
                MainScaffold(
                    sessionManager = sessionManager,
                    taskRepo = taskRepo,
                    financeRepo = financeRepo,
                    categoryRepo = categoryRepo,
                    syncRepository = syncRepository,
                    userRepository = userRepo,
                    db = db
                )
            }
        }
    }

    // ─────────────────────────────────────────
    // onResume — ✅ now at class level
    // ─────────────────────────────────────────
    override fun onResume() {
        super.onResume()

        if (Settings.canDrawOverlays(this)) {
            startQareebService()
        } else {
            checkPermissionsAndStart()
        }

        if (intent.getBooleanExtra("trigger_sync", false)) {
            val userId = intent.getStringExtra("userID")
                ?: sessionManager.getUserId()
                ?: return
            Log.d("SYNC", "App opened from notification, syncing...")
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { syncRepository.sync(userId) }
                    Log.d("SYNC", "Sync after notification completed")
                } catch (e: Exception) {
                    Log.e("SYNC", "Sync failed: ${e.message}", e)
                }
            }
            intent.removeExtra("trigger_sync")
        }
    }

    // ─────────────────────────────────────────
    // onDestroy — ✅ now at class level
    // ─────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(syncReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister syncReceiver: ${e.message}")
        }
    }

    // ─────────────────────────────────────────
    // Private helpers — ✅ now at class level
    // ─────────────────────────────────────────
    private fun registerFcmToken() {
        val userId = sessionManager.getUserId()
        if (userId.isNullOrEmpty()) {
            Log.d("FCM", "No user logged in, skipping token registration")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("FCM_TOKEN", "Token: $token")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        RetrofitInstance.syncApi.registerFcmToken(
                            SyncApi.FCMTokenRequest(
                                userID = userId,
                                fcm_token = token
                            )
                        )
                        Log.d("FCM", "Token sent successfully")
                    } catch (e: Exception) {
                        Log.e("FCM", "Failed to send token: ${e.message}", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to get token: ${e.message}", e)
            }
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            pendingStartAfterOverlay = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun startQareebService() {
        val intent = Intent(this, QareebListeningService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, "QareebListeningService started")
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                TaskNotificationReceiver.CHANNEL_ID,
                "Task Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you 2 hours before a task deadline"
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}