package com.example.qareeb.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.presentation.navigation.AppNavGraph
import com.example.qareeb.presentation.ui.components.BottomNavBar
import com.example.qareeb.presentation.utilis.SessionManager

@Composable
fun MainScaffold(
    sessionManager: SessionManager,
    taskRepo: TaskRepositoryImpl,
    financeRepo: TransactionRepositoryImpl,
    onStartQareeb: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            sessionManager = sessionManager,
            taskRepo = taskRepo,
            financeRepo = financeRepo,
            modifier = Modifier.padding(paddingValues),
            onStartQareeb = onStartQareeb
        )
    }
}