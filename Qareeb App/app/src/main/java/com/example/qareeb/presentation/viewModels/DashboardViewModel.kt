package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.model.TransactionDomain
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.usecase.task.GetTasksByUserUseCase
import com.example.qareeb.domain.usecase.transaction.GetTransactionsByUserUseCase
import com.example.qareeb.presentation.utilis.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class DashboardViewModel(
    private val getTasksByUser: GetTasksByUserUseCase,
    private val getTransactionsByUser: GetTransactionsByUserUseCase,
    private val userId: Long,
    val username: String
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val allTasks: StateFlow<List<TaskDomain>> = getTasksByUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionDomain>> = getTransactionsByUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // tasks matching selected date
    val todayTasks: StateFlow<List<TaskDomain>> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.filter { it.dueDate?.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // count of today's tasks
    val todayTasksCount: StateFlow<Int> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.count { it.dueDate?.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // priority tasks
    val priorityTasksCount: StateFlow<Int> = allTasks
        .combine(_selectedDate) { tasks, date ->
            tasks.count { it.priority != null && it.dueDate?.toLocalDate() == date }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // completed tasks
    val completedTasksCount: StateFlow<Int> = allTasks
        .combine(_selectedDate) { tasks, date ->
            tasks.count { it.status == TaskStatus.COMPLETED && it.dueDate?.toLocalDate() == date }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // tasks that have a priority set, for the selected date
    val priorityTasks: StateFlow<List<TaskDomain>> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.filter {
            it.priority != null && it.dueDate?.toLocalDate() == date
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // recent transactions (last 4)
    val recentTransactions: StateFlow<List<TransactionDomain>> = getTransactionsByUser(userId)
        .combine(_selectedDate) { transactions, _ ->
            transactions.take(4)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }
}

class DashboardViewModelFactory(
    private val getTasksByUser: GetTasksByUserUseCase,
    private val getTransactionsByUser: GetTransactionsByUserUseCase,
    private val userId: Long,
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(
            getTasksByUser, getTransactionsByUser, userId, username
        ) as T
    }
}