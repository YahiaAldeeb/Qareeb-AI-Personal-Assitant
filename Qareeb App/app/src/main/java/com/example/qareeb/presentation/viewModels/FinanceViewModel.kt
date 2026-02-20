package com.example.qareeb.presentation.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.domain.usecase.transaction.AddTransactionUseCase
import com.example.qareeb.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.qareeb.domain.usecase.transaction.GetTransactionsByUserUseCase
import com.example.qareeb.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.qareeb.presentation.utilis.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class FinanceViewModel(
    private val getTransactionsByUser: GetTransactionsByUserUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val userId: String,
    val username: String
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val categories = listOf("All", "Pending", "Completed", "Declined", "In Progress")

    // all transactions from DB
    private val allTransactions: StateFlow<List<TransactionDomain>> =
        getTransactionsByUser(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // today's transactions filtered by date and category
    val todayTransactions: StateFlow<List<TransactionDomain>> = combine(
        allTransactions, _selectedDate, _selectedCategory
    ) { transactions, date, category ->
        transactions
            .filter { it.date.toLocalDate() == date }
            .filter { matchesCategory(it, category) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // tomorrow's transactions filtered by date and category
    val tomorrowTransactions: StateFlow<List<TransactionDomain>> = combine(
        allTransactions, _selectedDate, _selectedCategory
    ) { transactions, date, category ->
        transactions
            .filter { it.date.toLocalDate() == date.plusDays(1) }
            .filter { matchesCategory(it, category) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun updateTransactionState(transaction: TransactionDomain, newState: TransactionState) {
        viewModelScope.launch {
            updateTransaction(transaction.copy(state = newState))
        }
    }

    private fun matchesCategory(transaction: TransactionDomain, category: String): Boolean {
        return when (category) {
            "All"         -> true
            "Pending"     -> transaction.state == TransactionState.PENDING
            "Completed"   -> transaction.state == TransactionState.COMPLETED
            "Declined"    -> transaction.state == TransactionState.DECLINED
            "In Progress" -> transaction.state == TransactionState.IN_PROGRESS
            else          -> true
        }
    }
}

class FinanceViewModelFactory(
    private val getTransactionsByUser: GetTransactionsByUserUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val userId: String,
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(
            getTransactionsByUser, updateTransaction,
            addTransaction, deleteTransaction, userId, username
        ) as T
    }
}