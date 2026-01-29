package com.example.qareeb.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.qareeb.presentation.screens.ChatBotScreen
import com.example.qareeb.presentation.screens.DashboardScreen
//import com.example.qareeb.presentation.screens.MyFinanceScreen
//import com.example.qareeb.presentation.screens.MyTasksScreen
import com.example.qareeb.screens.MyFinanceScreen
import com.example.qareeb.screens.MyTasksScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val TASKS = "task_tracker"
    const val CHATBOT = "chatbot"
    const val FINANCE = "finance"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier
    ) {
        composable(Routes.DASHBOARD) { DashboardScreen() }

        composable(Routes.TASKS) { MyTasksScreen() }

        composable(Routes.CHATBOT) { ChatBotScreen() }

        composable(Routes.FINANCE) { MyFinanceScreen() }
    }
}
