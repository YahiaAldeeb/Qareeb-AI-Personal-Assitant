package com.example.qareeb.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.qareeb.data.AppDatabase
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.CategoryRepositoryImpl
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
    categoryRepo: CategoryRepositoryImpl,
    userRepository: UserRepository,
    syncRepository: SyncRepository,
    db: AppDatabase,
    onStartQareeb: () -> Unit
) {
    val navController = rememberNavController()

    AppNavGraph(
        navController = navController,
        sessionManager = sessionManager,
        taskRepo = taskRepo,
        financeRepo = financeRepo,
        categoryRepo = categoryRepo,
        syncRepository = syncRepository,
        userRepository = userRepository,
        db = db,
        onStartQareeb = onStartQareeb
    )
}