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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val TAG = "QAREEB_DEBUG"

    // Activity-level deps (accessible from permission callbacks)
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var syncRepository: SyncRepository

    // Used to continue starting Qareeb after user grants overlay permission in Settings
    private var pendingStartAfterOverlay = false

    // 1) Overlay Permission launcher (Settings screen)
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (pendingStartAfterOverlay) {
                pendingStartAfterOverlay = false
                checkPermissionsAndStart()
            }
        }

    // 2) Runtime Permissions launcher (Mic + Notifications)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startQareebService()
                // Optional: minimize app
                // moveTaskToBack(true)
            } else {
                Log.w(TAG, "Permissions denied: $permissions")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Init DB + session
        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager.getInstance(this)

        // Repos for UI
        val taskRepo = TaskRepositoryImpl(db.taskDao())
        val financeRepo = TransactionRepositoryImpl(db.transactionDao())
        val userRepo = UserRepositoryImpl(db.userDao())

        // Sync repository
        syncRepository = SyncRepository(
            taskDao = db.taskDao(),
            userDao = db.userDao(),
            transactionDao = db.transactionDao(),
            api = RetrofitInstance.syncApi, // must exist in your RetrofitInstance
            prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        )

        // Sync on app start if already logged in (only if missing local data)
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
                    Log.d(
                        "SYNC",
                        "Missing data → tasks: ${localTasks.size}, transactions: ${localTransactions.size}, syncing..."
                    )
                    try {
                        withContext(Dispatchers.IO) { syncRepository.sync(userId) }
                    } catch (e: Exception) {
                        Log.e("SYNC", "Sync failed: ${e.message}", e)
                    }
                } else {
                    Log.d(
                        "SYNC",
                        "All data present → tasks: ${localTasks.size}, transactions: ${localTransactions.size}, skipping sync"
                    )
                }
            } else {
                Log.d("SYNC", "No user logged in, skipping sync")
            }
        }

        // ✅ Compose entry point
        setContent {
            MainScaffold(
                sessionManager = sessionManager,
                taskRepo = taskRepo,
                financeRepo = financeRepo,
                syncRepository = syncRepository,
                userRepository = userRepo,
                onStartQareeb = { checkPermissionsAndStart() } // called from ChatBotScreen switch
            )
        }
    }

    /**
     * Called when user toggles "Enable Qareeb Voice Assistant" ON.
     * Handles overlay permission -> runtime permissions -> starts foreground service.
     */
    private fun checkPermissionsAndStart() {
        // 1) Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            pendingStartAfterOverlay = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        // 2) Runtime permissions: RECORD_AUDIO + (Android 13+) POST_NOTIFICATIONS
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    /**
     * Starts the listening foreground service.
     */
    private fun startQareebService() {
        val intent = Intent(this, QareebListeningService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, "QareebListeningService started")
    }
}