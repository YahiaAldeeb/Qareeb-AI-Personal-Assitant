package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.usecase.task.AddTaskUseCase
import com.example.qareeb.domain.usecase.task.DeleteTaskUseCase
import com.example.qareeb.domain.usecase.task.GetTasksByUserUseCase
import com.example.qareeb.domain.usecase.task.UpdateTaskUseCase
import com.example.qareeb.presentation.utilis.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(
    private val getTasksByUser: GetTasksByUserUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val userId: Long,
    val username: String
) : ViewModel() {

    val filters = listOf("All", "Work", "Sports", "Personal", "Travel")

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    // ── Raw tasks from DB ──
    private val allTasks: StateFlow<List<TaskDomain>> = getTasksByUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Today's tasks filtered by selected date and category ──
    val todayTasks: StateFlow<List<TaskDomain>> = combine(
        allTasks, _selectedDate, _selectedFilter
    ) { tasks, date, filter ->
        tasks.filter { task ->
            val matchesDate = task.dueDate?.toLocalDate() == date
            val matchesFilter = filter == "All" || task.priority == filter
            matchesDate && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Tomorrow's tasks filtered by selected date + 1 and category ──
    val tomorrowTasks: StateFlow<List<TaskDomain>> = combine(
        allTasks, _selectedDate, _selectedFilter
    ) { tasks, date, filter ->
        tasks.filter { task ->
            val matchesDate = task.dueDate?.toLocalDate() == date.plusDays(1)
            val matchesFilter = filter == "All" || task.priority == filter
            matchesDate && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Priority tasks count for dashboard ──
    val priorityTasksCount: StateFlow<Int> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.count { it.priority != null && it.dueDate?.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Completed tasks count for dashboard ──
    val completedTasksCount: StateFlow<Int> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.count { it.status == TaskStatus.COMPLETED && it.dueDate?.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Events ──
    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun addTask(task: TaskDomain) {
        viewModelScope.launch { addTask.invoke(task) }
    }

    fun updateTask(task: TaskDomain) {
        viewModelScope.launch { updateTask.invoke(task) }
    }

    fun deleteTask(task: TaskDomain) {
        viewModelScope.launch { deleteTask.invoke(task) }
    }

    fun markAsCompleted(task: TaskDomain) {
        viewModelScope.launch {
            updateTask.invoke(task.copy(status = TaskStatus.COMPLETED))
        }
    }

    fun markAsInProgress(task: TaskDomain) {
        viewModelScope.launch {
            updateTask.invoke(task.copy(status = TaskStatus.IN_PROGRESS))
        }
    }

    fun markAsPostponed(task: TaskDomain) {
        viewModelScope.launch {
            updateTask.invoke(task.copy(status = TaskStatus.POSTPONED))
        }
    }
}

class TaskViewModelFactory(
    private val getTasksByUser: GetTasksByUserUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val userId: Long,
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(
            getTasksByUser, addTask, updateTask, deleteTask, userId, username
        ) as T
    }
}