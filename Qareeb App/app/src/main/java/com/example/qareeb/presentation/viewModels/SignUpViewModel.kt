package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.data.remote.RegisterRequest
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.domain.repository.UserRepository
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading

            // Validation
            when {
                fullName.isBlank() -> {
                    _signUpState.value = SignUpState.Error("Full name is required")
                    return@launch
                }
                email.isBlank() -> {
                    _signUpState.value = SignUpState.Error("Email is required")
                    return@launch
                }
                !email.contains("@") || !email.contains(".") -> {
                    _signUpState.value = SignUpState.Error("Invalid email address")
                    return@launch
                }
                phoneNumber.isBlank() -> {
                    _signUpState.value = SignUpState.Error("Phone number is required")
                    return@launch
                }
                phoneNumber.length != 11 -> {
                    _signUpState.value = SignUpState.Error("Phone number must be 11 digits")
                    return@launch
                }
                password.isBlank() -> {
                    _signUpState.value = SignUpState.Error("Password is required")
                    return@launch
                }
                password.length < 6 -> {
                    _signUpState.value = SignUpState.Error("Password must be at least 6 characters")
                    return@launch
                }
                password != confirmPassword -> {
                    _signUpState.value = SignUpState.Error("Passwords do not match")
                    return@launch
                }
            }

            try {
                // ── Call backend API to register user in Supabase ──
                android.util.Log.d("SIGNUP", "Registering user: $email")
                val response = RetrofitInstance.api.register(
                    RegisterRequest(
                        name = fullName,
                        email = email,
                        password = password,
                        phoneNumber = phoneNumber
                    )
                )
                android.util.Log.d("SIGNUP", "Registration success: userId=${response.userID}")

                val newUser = UserDomain(
                    userId = response.userID,
                    name = response.name,
                    email = response.email,
                    password = ""
                )

                // ── Save to local Room DB ──
                userRepository.insertUser(newUser)

                // ── Save Session ──
                sessionManager.saveUserSession(
                    userId = newUser.userId,
                    username = newUser.name,
                    email = newUser.email
                )

                _signUpState.value = SignUpState.Success(newUser)

            } catch (e: Exception) {
                android.util.Log.e("SIGNUP", "Registration failed: ${e.message}")
                e.printStackTrace()
                _signUpState.value = SignUpState.Error(
                    when {
                        e.message?.contains("409") == true -> "Email already exists"
                        e.message?.contains("Unable to resolve host") == true -> "No internet connection"
                        else -> "Registration failed: ${e.message}"
                    }
                )
            }
        }
    }
}

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    data class Success(val user: UserDomain) : SignUpState()
    data class Error(val message: String) : SignUpState()
}

class SignUpViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SignUpViewModel(userRepository, sessionManager) as T
    }
}