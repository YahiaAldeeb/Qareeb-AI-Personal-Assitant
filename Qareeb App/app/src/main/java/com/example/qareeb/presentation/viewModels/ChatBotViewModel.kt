package com.example.qareeb.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qareeb.NetworkModule
import com.example.qareeb.QareebResponse
import com.example.qareeb.TextMessageRequest
import com.example.qareeb.data.dao.TaskDao
import com.example.qareeb.data.dao.TransactionDao
import com.example.qareeb.data.entity.Task
import com.example.qareeb.data.entity.Transaction
import com.example.qareeb.data.remote.SyncRepository
import com.example.qareeb.domain.model.enums.TaskStatus
import com.example.qareeb.domain.model.enums.TransactionState
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

sealed class ChatUiState {
    data object Idle : ChatUiState()
    data object Thinking : ChatUiState()
    data class Success(val message: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatBotViewModel(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val userId: String = sessionManager.getUserId() ?: ""

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        // Add welcome message
        _messages.value = listOf(
            ChatMessage(
                text = "Hi! I'm Qareeb. Tell me what you'd like to do - like 'I spent 50 pounds on food' or 'I have a meeting at 3pm' or 'open YouTube'",
                isUser = false
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value is ChatUiState.Thinking) return

        // Add user message
        _messages.value = _messages.value + ChatMessage(text = text, isUser = true)

        // Show thinking state
        _uiState.value = ChatUiState.Thinking

        viewModelScope.launch {
            try {
                val response = NetworkModule.api.sendTextMessage(
                    TextMessageRequest(text = text, userID = userId)
                )

                handleResponse(response)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to send message: ${e.message}")
                // Add error bot response
                addBotMessage("Sorry, I couldn't process your request. Please try again.")
            }
        }
    }

    private fun handleResponse(response: QareebResponse) {
        viewModelScope.launch {
            try {
                when (response.status) {
                    "success" -> {
                        when (response.intent) {
                            "FINANCE" -> {
                                val transactionData = response.result?.data?.transaction
                                if (transactionData != null) {
                                    val transaction = parseTransactionFromResponse(transactionData, userId)
                                    transactionDao.upsertTransaction(transaction)
                                    val amount = transactionData.amount ?: 0.0
                                    val isIncome = transactionData.income ?: false
                                    val prefix = if (isIncome) "Added" else "Spent"
                                    val amountText = "£${String.format("%.2f", amount)}"
                                    val message = if (isIncome) "$amountText income ✅" else "$amountText expense ✅"
                                    _uiState.value = ChatUiState.Success(message)
                                    addBotMessage(message)
                                } else {
                                    _uiState.value = ChatUiState.Error("Could not create transaction")
                                    addBotMessage("Sorry, I couldn't create the transaction. Please try again.")
                                }
                            }
                            "TASK_TRACKER" -> {
                                val taskData = response.result?.data?.task
                                if (taskData != null) {
                                    val task = parseTaskFromResponse(taskData, userId)
                                    taskDao.upsertTask(task)
                                    val title = taskData.title ?: "task"
                                    _uiState.value = ChatUiState.Success("Created task: $title ✅")
                                    addBotMessage("Created task: $title ✅")
                                } else {
                                    _uiState.value = ChatUiState.Error("Could not create task")
                                    addBotMessage("Sorry, I couldn't create the task. Please try again.")
                                }
                            }
                            else -> {
                                _uiState.value = ChatUiState.Idle
                            }
                        }
                    }
                    "accepted" -> {
                        // UI Automation - job started
                        val command = response.text ?: ""
                        _uiState.value = ChatUiState.Success("Opening $command...")
                        addBotMessage("Opening $command... (This may take a moment)")
                    }
                    "unknown_intent" -> {
                        _uiState.value = ChatUiState.Error("I didn't understand that. Try something like 'open YouTube', 'I spent 50 pounds', or 'create a task'")
                        addBotMessage("I didn't understand that. Try something like:\n• 'Open YouTube'\n• 'I spent 50 pounds'\n• 'Create a meeting at 3pm'")
                    }
                    else -> {
                        _uiState.value = ChatUiState.Error(response.result?.error ?: "Something went wrong")
                        addBotMessage("Sorry, something went wrong. Please try again.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Error: ${e.message}")
                addBotMessage("Sorry, I encountered an error. Please try again.")
            }
        }
    }

    private fun addBotMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(text = text, isUser = false)
    }

    fun resetState() {
        _uiState.value = ChatUiState.Idle
    }

    private fun parseTransactionFromResponse(
        transactionData: com.example.qareeb.TransactionResponse,
        userId: String
    ): Transaction {
        val dateMillis = try {
            transactionData.date?.let {
                try {
                    OffsetDateTime.parse(it).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(it).toEpochMilli()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val state = try {
            transactionData.state?.let { TransactionState.valueOf(it.uppercase()) }
                ?: TransactionState.PENDING
        } catch (e: Exception) {
            TransactionState.PENDING
        }

        return Transaction(
            transactionId = transactionData.transactionID,
            userId = userId,
            categoryId = transactionData.categoryID,
            amount = transactionData.amount ?: 0.0,
            date = dateMillis,
            source = transactionData.source,
            description = transactionData.description,
            title = transactionData.title ?: transactionData.description ?: "Transaction",
            income = transactionData.income ?: false,
            state = state,
            isDeleted = transactionData.is_deleted ?: false,
            is_synced = true,
            updatedAt = transactionData.created_at ?: OffsetDateTime.now().toString()
        )
    }

    private fun parseTaskFromResponse(
        taskData: com.example.qareeb.TaskResponse,
        userId: String
    ): Task {
        val dueDateMillis = taskData.dueDate?.let { dueDateStr ->
            try {
                try {
                    OffsetDateTime.parse(dueDateStr).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    try {
                        Instant.parse(dueDateStr).toEpochMilli()
                    } catch (e2: Exception) {
                        java.time.LocalDate.parse(dueDateStr)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }
                }
            } catch (e: Exception) {
                java.time.LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
        } ?: run {
            java.time.LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        val status = try {
            taskData.status?.let { TaskStatus.valueOf(it.uppercase()) }
                ?: TaskStatus.PENDING
        } catch (e: Exception) {
            TaskStatus.PENDING
        }

        val priority = taskData.priority ?: "MEDIUM"

        return Task(
            taskId = taskData.taskID,
            userId = userId,
            title = taskData.title ?: "Task",
            description = taskData.description,
            status = status,
            progressPercentage = taskData.progressPercentage ?: 0,
            priority = priority,
            dueDate = dueDateMillis,
            isDeleted = taskData.is_deleted ?: false,
            is_synced = true,
            updatedAt = taskData.updated_at ?: OffsetDateTime.now().toString()
        )
    }
}

class ChatBotViewModelFactory(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatBotViewModel(
            transactionDao,
            taskDao,
            sessionManager,
            syncRepository
        ) as T
    }
}