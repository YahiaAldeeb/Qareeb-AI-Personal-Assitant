package com.example.qareeb.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.domain.repository.UserRepository
import com.example.qareeb.presentation.navigation.AppNavGraph
import com.example.qareeb.presentation.utilis.SessionManager

@Composable
fun MainScaffold(
    sessionManager: SessionManager,
    taskRepo: TaskRepositoryImpl,
    financeRepo: TransactionRepositoryImpl,
    userRepository: UserRepository,
    syncRepository: SyncRepository,
    onStartQareeb: () -> Unit
) {
    val navController = rememberNavController()

    // The Scaffold with conditional BottomNavBar now lives inside AppNavGraph.
    // MainScaffold is only responsible for setting up navController and wiring dependencies.
    AppNavGraph(
        navController = navController,
        sessionManager = sessionManager,
        taskRepo = taskRepo,
        financeRepo = financeRepo,
        syncRepository = syncRepository,
        userRepository = userRepository,
        onStartQareeb = onStartQareeb
    )
}