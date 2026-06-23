package com.example.qareeb.presentation.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.data.mapper.toEntity
import com.example.qareeb.domain.model.TaskDomain
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.usecase.task.AddTaskUseCase
import com.example.qareeb.domain.usecase.task.DeleteTaskUseCase
import com.example.qareeb.domain.usecase.task.GetTasksByUserUseCase
import com.example.qareeb.domain.usecase.task.UpdateTaskUseCase
import com.example.qareeb.notification.TaskNotificationScheduler
import com.example.qareeb.presentation.utilis.SessionManager
import com.example.qareeb.presentation.utilis.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(
    private val getTasksByUser: GetTasksByUserUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val sessionManager: SessionManager,
    private val context: Context,              // ✅ added for notification scheduler
    val username: String
) : ViewModel() {

    val filters = listOf("All", "Work", "Sports", "Personal", "Travel")

    private val userId: String = sessionManager.getUserId() ?: ""

    init {
        android.util.Log.d("VIEWMODEL", "ViewModel userId: $userId")
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val allTasks: StateFlow<List<TaskDomain>> = getTasksByUser(userId)
        .onEach { list ->
            android.util.Log.d("TASK_VM", "Tasks loaded: ${list.size}")
            list.forEach { task ->
                val dueDateLocal = task.dueDate?.toLocalDate()
                android.util.Log.d("TASK_VM", "  - '${task.title}', dueDate millis: ${task.dueDate}, localDate: $dueDateLocal")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<TaskDomain>> = combine(
        allTasks, _selectedDate, _selectedFilter
    ) { tasks, date, filter ->
        android.util.Log.d("TASK_VM", "Filtering for date: $date, tasks count: ${tasks.size}")
        tasks.filter { task ->
            val taskDueDate = task.dueDate?.toLocalDate()
            val matchesDate = taskDueDate == date
            val matchesFilter = filter == "All" || task.priority == filter
            matchesDate && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tomorrowTasks: StateFlow<List<TaskDomain>> = combine(
        allTasks, _selectedDate, _selectedFilter
    ) { tasks, date, filter ->
        tasks.filter { task ->
            val taskDueDate = task.dueDate?.toLocalDate()
            val matchesDate = taskDueDate == date.plusDays(1)
            val matchesFilter = filter == "All" || task.priority == filter
            matchesDate && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val priorityTasksCount: StateFlow<Int> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.count { it.priority != null && it.dueDate?.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedTasksCount: StateFlow<Int> = combine(
        allTasks, _selectedDate
    ) { tasks, date ->
        tasks.count { it.status == TaskStatus.COMPLETED && it.dueDate?.toLocalDate() == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onDateSelected(date: LocalDate) { _selectedDate.value = date }
    fun onFilterSelected(filter: String) { _selectedFilter.value = filter }

    fun addTask(task: TaskDomain) {
        viewModelScope.launch {
            addTask.invoke(task)
            // ✅ Schedule notification 2 hours before due date
            TaskNotificationScheduler.schedule(context, task.toEntity())
        }
    }

    fun updateTask(task: TaskDomain) {
        viewModelScope.launch {
            updateTask.invoke(task)
            // ✅ Reschedule notification with updated due date
            TaskNotificationScheduler.cancel(context, task.toEntity())
            TaskNotificationScheduler.schedule(context, task.toEntity())
        }
    }

    fun deleteTask(task: TaskDomain) {
        viewModelScope.launch {
            deleteTask.invoke(task)
            // ✅ Cancel notification when task is deleted
            TaskNotificationScheduler.cancel(context, task.toEntity())
        }
    }

    fun markAsCompleted(task: TaskDomain) {
        viewModelScope.launch {
            val updated = task.copy(status = TaskStatus.COMPLETED)
            updateTask.invoke(updated)
            // ✅ Cancel notification — task is done
            TaskNotificationScheduler.cancel(context, task.toEntity())
        }
    }

    fun markAsInProgress(task: TaskDomain) {
        viewModelScope.launch {
            updateTask.invoke(task.copy(status = TaskStatus.IN_PROGRESS))
        }
    }

    fun markAsPostponed(task: TaskDomain) {
        viewModelScope.launch {
            val updated = task.copy(status = TaskStatus.POSTPONED)
            updateTask.invoke(updated)
            // ✅ Cancel notification — task is postponed
            TaskNotificationScheduler.cancel(context, task.toEntity())
        }
    }
}

class TaskViewModelFactory(
    private val getTasksByUser: GetTasksByUserUseCase,
    private val addTask: AddTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val sessionManager: SessionManager,
    private val context: Context,              // ✅ added
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(
            getTasksByUser, addTask, updateTask, deleteTask,
            sessionManager, context, username             // ✅ added
        ) as T
    }
}