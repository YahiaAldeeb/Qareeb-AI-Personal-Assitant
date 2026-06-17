package com.example.qareeb.presentation.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.qareeb.presentation.utilis.SessionManager

class UserViewModel(private val sessionManager: SessionManager) : ViewModel() {
    val userId: String? get() = sessionManager.getUserId()
    val username: String get() = sessionManager.getUsername() ?: "Guest"
    val email: String? get() = sessionManager.getUserEmail()

    fun logout() {
        sessionManager.clearSession()
    }
}
class UserViewModelFactory(private val sessionManager:SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserViewModel(sessionManager) as T
    }
}