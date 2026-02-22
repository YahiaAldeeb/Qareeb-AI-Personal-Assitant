package com.example.qareeb.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.example.qareeb.presentation.screens.SplashScreen
import com.example.qareeb.presentation.screens.DashboardScreen
import com.example.qareeb.presentation.screens.FinanceScreen
import com.example.qareeb.presentation.screens.LoginScreen
import com.example.qareeb.presentation.screens.SignUpScreen
import com.example.qareeb.presentation.screens.SplashScreen
import com.example.qareeb.presentation.screens.TasksScreen
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
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    taskRepo: TaskRepositoryImpl,
    financeRepo: TransactionRepositoryImpl,
    syncRepository: SyncRepository,
    userRepository: UserRepository,
    modifier: Modifier = Modifier,
    onStartQareeb: () -> Unit
) {

    val startDestination = if (sessionManager.isLoggedIn()) {
        Routes.SPLASH      //already logged in,go to splash then dashboard
    } else {
        Routes.LOGIN       //never logged in, show login first
    }

    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(sessionManager)
    )
    val getTasksByUser = GetTasksByUserUseCase(taskRepo)
    val addTask = AddTaskUseCase(taskRepo)
    val updateTask = UpdateTaskUseCase(taskRepo)
    val deleteTask = DeleteTaskUseCase(taskRepo)

    val getTransactionsByUser = GetTransactionsByUserUseCase(financeRepo)
    val addTransaction = AddTransactionUseCase(financeRepo)
    val updateTransaction = UpdateTransactionUseCase(financeRepo)
    val deleteTransaction = DeleteTransactionUseCase(financeRepo)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        //Splash
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        //Dashboard
        composable(Routes.DASHBOARD) {
            val userViewModel: UserViewModel = viewModel(
                factory = UserViewModelFactory(sessionManager)
            )
            val username = userViewModel.username

            android.util.Log.d("NAV", "Dashboard userId: ${sessionManager.getUserId()}")

            val vm: DashboardViewModel = viewModel(
                factory = DashboardViewModelFactory(
                    getTasksByUser = getTasksByUser,
                    getTransactionsByUser = getTransactionsByUser,
                    sessionManager = sessionManager,  // ← sessionManager instead of userId
                    username = username
                )
            )
            DashboardScreen(
                viewModel = vm,
                onViewAllPlans = { navController.navigate(Routes.TASKS) },
                onViewAllExpenses = { navController.navigate(Routes.FINANCE) }
            )
        }

        // ── Tasks ──
        composable(Routes.TASKS) {
            val userViewModel: UserViewModel = viewModel(
                factory = UserViewModelFactory(sessionManager)
            )
            val username = userViewModel.username

            android.util.Log.d("NAV", "Tasks userId: ${sessionManager.getUserId()}")

            val vm: TaskViewModel = viewModel(
                factory = TaskViewModelFactory(
                    getTasksByUser = getTasksByUser,
                    addTask = addTask,
                    updateTask = updateTask,
                    deleteTask = deleteTask,
                    sessionManager = sessionManager,  // ← sessionManager instead of userId
                    username = username
                )
            )
            TasksScreen(viewModel = vm)
        }

        //finance
        composable(Routes.FINANCE) {
            val userViewModel: UserViewModel = viewModel(
                factory = UserViewModelFactory(sessionManager)
            )
            val username = userViewModel.username

            android.util.Log.d("NAV", "Finance userId: ${sessionManager.getUserId()}")

            val vm: FinanceViewModel = viewModel(
                factory = FinanceViewModelFactory(
                    getTransactionsByUser = getTransactionsByUser,
                    updateTransaction = updateTransaction,
                    addTransaction = addTransaction,
                    deleteTransaction = deleteTransaction,
                    sessionManager = sessionManager,  // ← sessionManager instead of userId
                    username = username
                )
            )
            FinanceScreen(viewModel = vm)
        }
        //login
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(
                    userRepository = userRepository,
                    sessionManager = sessionManager,  // ← already available in AppNavGraph
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

        //register
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
        // ── ChatBot ──
        composable(Routes.CHATBOT) {
            ChatBotScreen(
                username = username,
                onStartQareeb = onStartQareeb
            )
        }
    }
}