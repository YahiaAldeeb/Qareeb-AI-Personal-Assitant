package com.example.qareeb.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.data.repositoryImp.TaskRepositoryImpl
import com.example.qareeb.data.repositoryImp.TransactionRepositoryImpl
import com.example.qareeb.domain.repository.UserRepository
import com.example.qareeb.domain.usecase.task.AddTaskUseCase
import com.example.qareeb.domain.usecase.task.DeleteTaskUseCase
import com.example.qareeb.domain.usecase.task.GetTasksByUserUseCase
import com.example.qareeb.domain.usecase.task.UpdateTaskUseCase
import com.example.qareeb.domain.usecase.transaction.AddTransactionUseCase
import com.example.qareeb.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.qareeb.domain.usecase.transaction.GetTransactionsByUserUseCase
import com.example.qareeb.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.qareeb.presentation.screens.ChatBotScreen
import com.example.qareeb.presentation.screens.DashboardScreen
import com.example.qareeb.presentation.screens.FinanceScreen
import com.example.qareeb.presentation.screens.LoginScreen
import com.example.qareeb.presentation.screens.SignUpScreen
import com.example.qareeb.presentation.screens.SplashScreen
import com.example.qareeb.presentation.screens.TasksScreen
import com.example.qareeb.presentation.screens.ProfileScreen
import com.example.qareeb.presentation.ui.components.BottomNavBar
import com.example.qareeb.presentation.utilis.SessionManager
import com.example.qareeb.presentation.viewModels.DashboardViewModel
import com.example.qareeb.presentation.viewModels.DashboardViewModelFactory
import com.example.qareeb.presentation.viewModels.FinanceViewModel
import com.example.qareeb.presentation.viewModels.FinanceViewModelFactory
import com.example.qareeb.presentation.viewModels.LoginViewModel
import com.example.qareeb.presentation.viewModels.LoginViewModelFactory
import com.example.qareeb.presentation.viewModels.SignUpViewModel
import com.example.qareeb.presentation.viewModels.SignUpViewModelFactory
import com.example.qareeb.presentation.viewModels.TaskViewModel
import com.example.qareeb.presentation.viewModels.TaskViewModelFactory
import com.example.qareeb.presentation.viewModels.UserViewModel
import com.example.qareeb.presentation.viewModels.UserViewModelFactory

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val TASKS = "task_tracker"
    const val CHATBOT = "chatbot"
    const val FINANCE = "finance"
    const val REGISTER = "register"
    const val PROFILE = "profile"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    taskRepo: TaskRepositoryImpl,
    financeRepo: TransactionRepositoryImpl,
    syncRepository: SyncRepository,
    userRepository: UserRepository,
    onStartQareeb: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Bottom bar should only be visible on main app screens, not on auth/splash screens
    val showBottomBar = currentRoute in listOf(
        Routes.DASHBOARD,
        Routes.TASKS,
        Routes.FINANCE,
        Routes.CHATBOT,
        Routes.PROFILE
    )

    val startDestination = if (sessionManager.isLoggedIn()) {
        Routes.SPLASH
    } else {
        Routes.LOGIN
    }

    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(sessionManager)
    )
    val username = userViewModel.username

    val getTasksByUser = GetTasksByUserUseCase(taskRepo)
    val addTask = AddTaskUseCase(taskRepo)
    val updateTask = UpdateTaskUseCase(taskRepo)
    val deleteTask = DeleteTaskUseCase(taskRepo)

    val getTransactionsByUser = GetTransactionsByUserUseCase(financeRepo)
    val addTransaction = AddTransactionUseCase(financeRepo)
    val updateTransaction = UpdateTransactionUseCase(financeRepo)
    val deleteTransaction = DeleteTransactionUseCase(financeRepo)

    // Scaffold owns the BottomNavBar visibility based on current route
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // Login (no bottom bar)
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(
                        userRepository = userRepository,
                        sessionManager = sessionManager,
                        syncRepository = syncRepository
                    )
                )
                LoginScreen(
                    viewModel = vm,
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onForgotPasswordClick = { },
                    onRegisterClick = { navController.navigate(Routes.REGISTER) }
                )
            }

            // Register (no bottom bar)
            composable(Routes.REGISTER) {
                val vm: SignUpViewModel = viewModel(
                    factory = SignUpViewModelFactory(
                        userRepository = userRepository,
                        sessionManager = sessionManager
                    )
                )
                SignUpScreen(
                    viewModel = vm,
                    onSignUpSuccess = {
                        navController.navigate(Routes.SPLASH) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard (bottom bar shows)
            composable(Routes.DASHBOARD) {
                val vm: DashboardViewModel =
                    viewModel(
                        factory = DashboardViewModelFactory(
                            getTasksByUser = getTasksByUser,
                            getTransactionsByUser = getTransactionsByUser,
                            sessionManager = sessionManager,
                            username = username
                        )
                    )
                DashboardScreen(
                    viewModel = vm,
                    onViewAllPlans = { navController.navigate(Routes.TASKS) },
                    onViewAllExpenses = { navController.navigate(Routes.FINANCE) }
                )
            }

            // Tasks (bottom bar shows)
            composable(Routes.TASKS) {
                val vm: TaskViewModel =
                    viewModel(
                        factory = TaskViewModelFactory(
                            getTasksByUser = getTasksByUser,
                            addTask = addTask,
                            updateTask = updateTask,
                            deleteTask = deleteTask,
                            sessionManager = sessionManager,
                            username = username
                        )
                    )
                TasksScreen(viewModel = vm)
            }

            // Finance (bottom bar shows)
            composable(Routes.FINANCE) {
                val vm: FinanceViewModel =
                    viewModel(
                        factory = FinanceViewModelFactory(
                            getTransactionsByUser = getTransactionsByUser,
                            updateTransaction = updateTransaction,
                            addTransaction = addTransaction,
                            deleteTransaction = deleteTransaction,
                            sessionManager = sessionManager,
                            username = username
                        )
                    )
                FinanceScreen(viewModel = vm)
            }

            // ChatBot (bottom bar shows)
            composable(Routes.CHATBOT) {
                ChatBotScreen(
                    username = username,
                    onStartQareeb = onStartQareeb
                )
            }

            // Profile (bottom bar shows)
            composable(Routes.PROFILE) {
                ProfileScreen(
                    userViewModel = userViewModel,
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}