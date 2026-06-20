package com.example.qareeb.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.CategoryRepositoryImpl
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.domain.repository.UserRepository
import com.example.qareeb.presentation.navigation.AppNavGraph
import com.example.qareeb.presentation.navigation.Routes
import com.example.qareeb.presentation.ui.components.BottomNavBar
import com.example.qareeb.presentation.utilis.SessionManager

@Composable
fun MainScaffold(
    sessionManager: SessionManager,
    taskRepo: TaskRepositoryImpl,
    financeRepo: TransactionRepositoryImpl,
    categoryRepo: CategoryRepositoryImpl,
    userRepository: UserRepository,
    syncRepository: SyncRepository,
    db: AppDatabase,
    //onStartQareeb: () -> Unit,
    //onLoginSuccess: () -> Unit = {}
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ✅ Single source of truth for bottom bar visibility
    val hideNavBarRoutes = setOf(
        Routes.LOGIN,
        Routes.REGISTER,
        Routes.SPLASH,
        Routes.VOICE_ENROLLMENT
    )

    Scaffold(
        bottomBar = {
            if (currentRoute !in hideNavBarRoutes) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            sessionManager = sessionManager,
            taskRepo = taskRepo,
            financeRepo = financeRepo,
            categoryRepository = categoryRepo,
            syncRepository = syncRepository,
            userRepository = userRepository,
            modifier = Modifier.padding(innerPadding),
            db = db
        )
    }
}