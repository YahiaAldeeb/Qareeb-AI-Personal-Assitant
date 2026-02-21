package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.UserRepository
import com.example.qareeb.domain.usecase.task.AddTaskUseCase
import com.example.qareeb.domain.usecase.task.DeleteTaskUseCase
import com.example.qareeb.domain.usecase.task.GetTasksByUserUseCase
import com.example.qareeb.domain.usecase.task.UpdateTaskUseCase
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            if (email.isBlank() || password.isBlank()) {
                _loginState.value = LoginState.Error("Please fill in all fields")
                return@launch
            }

            val user = userRepository.getUserByEmailAndPassword(email, password)
            if (user != null) {
                sessionManager.saveUserSession(
                    userId = user.userId,
                    username = user.name,
                    email = user.email
                )
                _loginState.value = LoginState.Success(user)
            } else {
                _loginState.value = LoginState.Error("Invalid email or password")
            }
        }
    }
}

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(
            userRepository, sessionManager
        ) as T
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserDomain) : LoginState()
    data class Error(val message: String) : LoginState()
}