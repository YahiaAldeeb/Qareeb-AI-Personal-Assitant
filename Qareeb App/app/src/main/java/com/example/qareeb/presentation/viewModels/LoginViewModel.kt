package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.data.remote.LoginRequest
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.UserRepository
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
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

            try {
                val response = RetrofitInstance.api.login(
                    LoginRequest(email = email, password = password)
                )

                sessionManager.saveUserSession(
                    userId = response.userID,
                    username = response.name,
                    email = response.email
                )

                // Sync right after login
                syncRepository.sync(response.userID)

                _loginState.value = LoginState.Success(
                    UserDomain(
                        userId = response.userID,
                        name = response.name,
                        email = response.email,
                        password = ""
                    )
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Invalid email or password")
            }
        }
    }
}

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(userRepository, sessionManager, syncRepository) as T
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserDomain) : LoginState()
    data class Error(val message: String) : LoginState()
}